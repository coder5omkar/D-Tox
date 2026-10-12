package com.dtox.app.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

enum class AmbientSound(val id: String, val label: String) {
    OFF("off", "Off"),
    RAIN("rain", "Rain"),
    OCEAN("ocean", "Ocean"),
    STREAM("stream", "Stream"),
    FOREST("forest", "Forest");

    companion object {
        fun from(id: String): AmbientSound = entries.firstOrNull { it.id == id } ?: OFF
    }
}

/**
 * Procedural ambient audio — every sound is synthesised sample by sample,
 * so there are no audio files, no downloads and no permissions. Rain,
 * ocean, stream and forest-with-birds are filtered noise plus small
 * generative details. Fades in and out to stay click-free and stops
 * whenever the screen is not in front (calls, leaving the exercise).
 */
class AmbientEngine {

    private var thread: Thread? = null

    @Volatile
    private var running = false

    fun start(sound: AmbientSound) {
        stop()
        if (sound == AmbientSound.OFF) return
        running = true
        val worker = Thread({ render(sound) }, "dtox-ambient")
        worker.isDaemon = true
        thread = worker
        worker.start()
    }

    fun stop() {
        val worker = thread ?: return
        running = false
        runCatching { worker.join(600) }
        thread = null
    }

    private fun render(sound: AmbientSound) {
        val frames = 2048
        val minBytes = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        val bufferBytes = maxOf(minBytes, frames * 2 * 2)

        val track = runCatching {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build(),
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build(),
                )
                .setBufferSizeInBytes(bufferBytes)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
        }.getOrNull() ?: return

        val generator = when (sound) {
            AmbientSound.RAIN -> RainGenerator()
            AmbientSound.OCEAN -> OceanGenerator()
            AmbientSound.STREAM -> StreamGenerator()
            AmbientSound.FOREST -> ForestGenerator()
            AmbientSound.OFF -> return
        }

        val samples = ShortArray(frames)
        val fadeStep = 1f / (SAMPLE_RATE * 0.35f)
        var gain = 0f

        try {
            track.play()
            while (running || gain > 0.001f) {
                val target = if (running) 1f else 0f
                for (i in 0 until frames) {
                    gain = if (gain < target) {
                        minOf(target, gain + fadeStep)
                    } else {
                        maxOf(target, gain - fadeStep)
                    }
                    val value = generator.next() * gain
                    samples[i] = (value.coerceIn(-1f, 1f) * 32767f).toInt().toShort()
                }
                track.write(samples, 0, frames)
            }
        } finally {
            runCatching { track.stop() }
            runCatching { track.release() }
        }
    }

    companion object {
        const val SAMPLE_RATE = 22050
    }
}

private interface Generator {
    fun next(): Float
}

private class Noise(seed: Int) {
    private val random = Random(seed)
    fun white(): Float = random.nextFloat() * 2f - 1f
    fun chance(p: Float): Boolean = random.nextFloat() < p
    fun range(from: Float, to: Float): Float = from + random.nextFloat() * (to - from)
}

/** Broadband noise with soft droplets on top. */
private class RainGenerator : Generator {
    private val noise = Noise(11)
    private var lowpass = 0f
    private var dropEnv = 0f
    private var dropPhase = 0.0
    private var dropFreq = 2000.0

    override fun next(): Float {
        val white = noise.white()
        lowpass += 0.42f * (white - lowpass)

        var drop = 0f
        if (dropEnv <= 0.001f && noise.chance(0.0007f)) {
            dropEnv = 0.28f
            dropFreq = noise.range(1400f, 2600f).toDouble()
            dropPhase = 0.0
        }
        if (dropEnv > 0.001f) {
            dropPhase += 2.0 * PI * dropFreq / AmbientEngine.SAMPLE_RATE
            drop = (sin(dropPhase) * dropEnv).toFloat()
            dropEnv *= 0.9994f
        }
        return lowpass * 0.40f + drop * 0.5f
    }
}

/** Brown noise with a slow swell — waves coming and going. */
private class OceanGenerator : Generator {
    private val noise = Noise(23)
    private var brown = 0f
    private var lowpass = 0f
    private var t = 0.0

    override fun next(): Float {
        val white = noise.white()
        brown = (brown + white * 0.02f) * 0.997f
        lowpass += 0.10f * (brown - lowpass)
        val swell = 0.55f + 0.45f * sin(t * 2.0 * PI * 0.07).toFloat()
        t += 1.0 / AmbientEngine.SAMPLE_RATE
        return lowpass * swell * 1.6f
    }
}

/** Bright band-limited noise with a gentle burble. */
private class StreamGenerator : Generator {
    private val noise = Noise(37)
    private var fast = 0f
    private var slow = 0f
    private var t = 0.0

    override fun next(): Float {
        val white = noise.white()
        fast += 0.60f * (white - fast)
        slow += 0.02f * (fast - slow)
        val band = fast - slow
        val wobble = 1f + 0.10f * sin(t * 2.0 * PI * 1.3).toFloat()
        t += 1.0 / AmbientEngine.SAMPLE_RATE
        return band * 0.55f * wobble
    }
}

/** Soft breeze with occasional distant bird chirps. */
private class ForestGenerator : Generator {
    private val noise = Noise(53)
    private var breeze = 0f
    private var nextEventIn = (AmbientEngine.SAMPLE_RATE * 2.0).toInt()
    private var notesInSong = 0
    private var chirpLife = 0
    private var chirpTotal = 1
    private var chirpF0 = 2800.0
    private var chirpPhase = 0.0

    override fun next(): Float {
        val white = noise.white()
        breeze += 0.035f * (white - breeze)

        var bird = 0f
        if (chirpLife <= 0) {
            nextEventIn -= 1
            if (nextEventIn <= 0) {
                if (notesInSong <= 0) notesInSong = 2 + noise.range(0f, 3f).toInt()
                chirpLife = (AmbientEngine.SAMPLE_RATE * noise.range(0.06f, 0.11f)).toInt()
                chirpTotal = chirpLife
                chirpF0 = noise.range(2400f, 3400f).toDouble()
                chirpPhase = 0.0
                notesInSong -= 1
                nextEventIn = if (notesInSong > 0) {
                    (AmbientEngine.SAMPLE_RATE * noise.range(0.08f, 0.22f)).toInt()
                } else {
                    (AmbientEngine.SAMPLE_RATE * noise.range(2.5f, 6.5f)).toInt()
                }
            }
        } else {
            val progress = 1f - chirpLife.toFloat() / chirpTotal
            val freq = chirpF0 * (1.0 - 0.30 * progress)
            chirpPhase += 2.0 * PI * freq / AmbientEngine.SAMPLE_RATE
            val env = sin(PI * progress)
            bird = (sin(chirpPhase) * env).toFloat() * 0.16f
            chirpLife -= 1
        }
        return breeze * 1.2f + bird
    }
}

/**
 * Plays [soundId] while [active] and the screen is in front. Completely
 * self-managing: stops on dispose, on leaving the exercise and whenever the
 * activity goes to the background (phone calls never get noise on top).
 */
@Composable
fun AmbientSoundEffect(soundId: String, active: Boolean) {
    val engine = remember { AmbientEngine() }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, soundId, active) {
        val sound = AmbientSound.from(soundId)
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> if (active) engine.start(sound)
                Lifecycle.Event.ON_STOP -> engine.stop()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        if (active && lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            engine.start(sound)
        }
        onDispose {
            lifecycle.removeObserver(observer)
            engine.stop()
        }
    }
}
