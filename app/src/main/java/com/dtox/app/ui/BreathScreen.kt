package com.dtox.app.ui

import android.os.SystemClock
import android.view.HapticFeedbackConstants
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.dtox.app.data.AppInfo
import com.dtox.app.data.BreathPattern
import com.dtox.app.data.Prefs
import com.dtox.app.data.phaseLabel
import com.dtox.app.ui.breath.BreathingRing
import com.dtox.app.ui.breath.phaseIndexAt
import com.dtox.app.ui.breath.rememberBreathScale
import com.dtox.app.ui.breath.targetScaleFor
import com.dtox.app.util.findActivity
import kotlinx.coroutines.delay

/**
 * The gate: a full-screen breathing pause shown before [app] opens.
 *
 * Call-safe by construction — D-Tox has no background components, so an
 * incoming or outgoing call is never touched. If a call interrupts the
 * breath, the timer pauses and picks up right where it left off.
 */
@Composable
fun BreathScreen(
    app: AppInfo,
    prefs: Prefs,
    onOpenApp: () -> Unit,
    onNotNow: () -> Unit,
) {
    val seconds = prefs.breathSeconds
    val pattern = BreathPattern.from(prefs.patternId)
    val haptics = prefs.haptics
    val allowSkip = prefs.allowSkip
    val durationMs = seconds * 1000L

    val startAt by rememberSaveable { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    var now by remember { mutableLongStateOf(startAt) }
    var pausedTotal by rememberSaveable { mutableLongStateOf(0L) }
    var pauseStart by rememberSaveable { mutableLongStateOf(0L) }

    BackHandler { onNotNow() }

    // Pause the clock whenever the screen is not in front (e.g. a phone call).
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    if (pauseStart == 0L) pauseStart = SystemClock.elapsedRealtime()
                }

                Lifecycle.Event.ON_RESUME -> {
                    if (pauseStart != 0L) {
                        pausedTotal += SystemClock.elapsedRealtime() - pauseStart
                        pauseStart = 0L
                    }
                }

                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    // Keep the screen awake while breathing.
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val activity = context.findActivity()
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    // Tick only while resumed.
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                now = SystemClock.elapsedRealtime()
                delay(50L)
            }
        }
    }

    val elapsedMs = run {
        val base = now - startAt - pausedTotal
        if (pauseStart != 0L) base - (SystemClock.elapsedRealtime() - pauseStart) else base
    }.coerceAtLeast(0L)

    val done = elapsedMs >= durationMs
    val remainingSeconds = (((durationMs - elapsedMs) + 999L) / 1000L).coerceAtLeast(0L)

    val phaseIndex = if (done) pattern.phases.lastIndex else phaseIndexAt(pattern, elapsedMs)
    val phase = pattern.phases[phaseIndex]

    val scale = rememberBreathScale(
        target = if (done) 0.72f else targetScaleFor(pattern.phases, phaseIndex),
        animationMs = if (done) 500 else phase.seconds * 1000,
    )

    val view = LocalView.current
    LaunchedEffect(phaseIndex, done) {
        if (haptics && !done) {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Before ${app.label}",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "one breath first",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.weight(1f))

        BreathingRing(
            fraction = if (durationMs > 0L) elapsedMs.toFloat() / durationMs else 0f,
            scale = scale,
            centerTop = if (done) "Done" else phaseLabel(phase),
            centerMain = if (done) null else remainingSeconds.toString(),
            centerSub = if (done) null else "seconds",
        )

        Spacer(Modifier.weight(1f))

        Text(
            text = if (done) {
                "Nice. Now go in on purpose."
            } else {
                "The app can wait. You're in charge."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(onClick = onNotNow, modifier = Modifier.weight(1f)) {
                Text("Not now")
            }
            Button(
                onClick = onOpenApp,
                enabled = done || allowSkip,
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = if (done || allowSkip) {
                        "Open ${app.label}"
                    } else {
                        "Open in ${remainingSeconds}s"
                    },
                    maxLines = 1,
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        Text(
            text = "No internet · No tracking · Calls never interrupted",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
