package com.dtox.app.audio

import android.content.Context
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.dtox.app.R

/** Real recorded ambience (CC0 / CC BY-SA — credits in LICENSES.md). */
enum class AmbientSound(val id: String, val label: String, val rawResId: Int?) {
    OFF("off", "Off", null),
    RAIN("rain", "Rain", R.raw.ambient_rain),
    OCEAN("ocean", "Ocean", R.raw.ambient_ocean),
    STREAM("stream", "Stream", R.raw.ambient_stream),
    FOREST("forest", "Forest", R.raw.ambient_forest);

    companion object {
        fun from(id: String): AmbientSound = entries.firstOrNull { it.id == id } ?: OFF
    }
}

/**
 * Plays looped ambience with smooth fades, and stops cleanly whenever the
 * screen leaves the foreground (phone calls never get sound layered on top).
 */
class AmbientEngine(private val context: Context) {

    private val handler = Handler(Looper.getMainLooper())

    private var active: Managed? = null

    private class Managed(val player: MediaPlayer) {
        var volume = 0f
        var generation = 0
    }

    fun start(sound: AmbientSound) {
        val resId = sound.rawResId
        val previous = active
        active = null
        if (previous != null) fadeOut(previous)
        if (resId == null) return

        val player = runCatching { MediaPlayer.create(context, resId) }.getOrNull() ?: return
        player.isLooping = true
        player.setVolume(0f, 0f)
        val started = runCatching { player.start() }.isSuccess
        if (!started) {
            runCatching { player.release() }
            return
        }
        val managed = Managed(player)
        active = managed
        fade(managed, target = 1f, durationMs = 1200)
    }

    fun stop() {
        val managed = active ?: return
        active = null
        fadeOut(managed)
    }

    private fun fadeOut(managed: Managed) {
        fade(managed, target = 0f, durationMs = 500) {
            runCatching { managed.player.stop() }
            runCatching { managed.player.release() }
        }
    }

    private fun fade(
        managed: Managed,
        target: Float,
        durationMs: Int,
        onDone: (() -> Unit)? = null,
    ) {
        val from = managed.volume
        val generation = ++managed.generation
        val steps = (durationMs / 40).coerceAtLeast(1)
        val startAt = SystemClock.uptimeMillis()
        for (i in 1..steps) {
            handler.postAtTime(
                {
                    // A newer fade has superseded this one — drop it.
                    if (managed.generation != generation) return@postAtTime
                    val t = (i.toFloat() / steps).coerceIn(0f, 1f)
                    val value = from + (target - from) * t
                    managed.volume = value
                    runCatching { managed.player.setVolume(value, value) }
                    if (i == steps) onDone?.invoke()
                },
                startAt + (i * 40L),
            )
        }
    }
}

/**
 * Plays [soundId] while [active] and the screen is in front. Self-managing:
 * stops on dispose, on leaving the exercise, and whenever the activity goes
 * to the background.
 */
@Composable
fun AmbientSoundEffect(soundId: String, active: Boolean) {
    val context = LocalContext.current.applicationContext
    val engine = remember { AmbientEngine(context) }
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
