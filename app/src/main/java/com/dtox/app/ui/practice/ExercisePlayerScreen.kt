package com.dtox.app.ui.practice

import android.os.SystemClock
import android.view.HapticFeedbackConstants
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.dtox.app.data.BreathPattern
import com.dtox.app.data.Completion
import com.dtox.app.data.Exercise
import com.dtox.app.data.ExerciseKind
import com.dtox.app.data.PracticeStore
import com.dtox.app.data.phaseLabel
import com.dtox.app.ui.breath.BreathingRing
import com.dtox.app.ui.breath.phaseIndexAt
import com.dtox.app.ui.breath.rememberBreathScale
import com.dtox.app.ui.breath.targetScaleFor
import com.dtox.app.util.findActivity
import kotlinx.coroutines.delay

private enum class Step { MOOD, ACTIVE, COMPLETE }

private val MOOD_FACES = listOf("😣", "😟", "😐", "🙂", "😌")

/**
 * Plays one exercise end to end: optional mood check, the exercise itself,
 * then the completion screen with gems and the plant update.
 */
@Composable
fun ExercisePlayerScreen(
    exercise: Exercise,
    store: PracticeStore,
    zenMode: Boolean,
    haptics: Boolean,
    onSessionRecorded: () -> Unit,
    onGemsChanged: () -> Unit,
    onExit: () -> Unit,
) {
    var step by rememberSaveable { mutableStateOf(if (zenMode) Step.ACTIVE else Step.MOOD) }
    var moodBefore by remember { mutableStateOf<Int?>(null) }
    var completion by remember { mutableStateOf<Completion?>(null) }
    var attempt by rememberSaveable { mutableIntStateOf(0) }

    BackHandler { onExit() }

    when (step) {
        Step.MOOD -> MoodCheck(
            title = "How do you feel right now?",
            onPick = { mood ->
                moodBefore = mood
                step = Step.ACTIVE
            },
            onSkip = {
                moodBefore = null
                step = Step.ACTIVE
            },
        )

        Step.ACTIVE -> key(attempt) {
            when (val kind = exercise.kind) {
                is ExerciseKind.Breathing -> BreathingExercise(
                    exercise = exercise,
                    kind = kind,
                    haptics = haptics,
                    onDone = {
                        completion = store.recordCompletion(exercise)
                        onSessionRecorded()
                        step = Step.COMPLETE
                    },
                    onExit = onExit,
                )

                is ExerciseKind.Steps -> StepsExercise(
                    exercise = exercise,
                    kind = kind,
                    onDone = {
                        completion = store.recordCompletion(exercise)
                        onSessionRecorded()
                        step = Step.COMPLETE
                    },
                    onExit = onExit,
                )
            }
        }

        Step.COMPLETE -> CompletionView(
            exercise = exercise,
            completion = completion,
            zenMode = zenMode,
            moodBefore = moodBefore,
            onMoodBonus = {
                store.awardMoodBonus()
                onGemsChanged()
            },
            onDone = onExit,
            onAgain = {
                completion = null
                moodBefore = null
                attempt++
                step = if (zenMode) Step.ACTIVE else Step.MOOD
            },
        )
    }
}

@Composable
private fun MoodCheck(
    title: String,
    onPick: (Int) -> Unit,
    onSkip: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MOOD_FACES.forEachIndexed { index, face ->
                Text(
                    text = face,
                    fontSize = 34.sp,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { onPick(index + 1) }
                        .padding(6.dp),
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        TextButton(onClick = onSkip) { Text("Skip") }
    }
}

@Composable
private fun BreathingExercise(
    exercise: Exercise,
    kind: ExerciseKind.Breathing,
    haptics: Boolean,
    onDone: () -> Unit,
    onExit: () -> Unit,
) {
    val pattern = BreathPattern.from(kind.patternId)
    val durationMs = (pattern.cycleSeconds * kind.rounds) * 1000L

    // State re-initializes when the exercise changes (the outer key(attempt)
    // already resets it for restarts), so a previous session's saved clock
    // can never leak into a new one.
    val startAt by rememberSaveable(exercise.id) {
        mutableLongStateOf(SystemClock.elapsedRealtime())
    }
    var now by remember { mutableLongStateOf(startAt) }
    var pausedTotal by rememberSaveable(exercise.id) { mutableLongStateOf(0L) }
    var pauseStart by rememberSaveable(exercise.id) { mutableLongStateOf(0L) }

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

    val context = LocalContext.current
    DisposableEffect(Unit) {
        val activity = context.findActivity()
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

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

    LaunchedEffect(done) {
        if (done) {
            delay(900L)
            onDone()
        }
    }

    val round = ((elapsedMs / (pattern.cycleSeconds * 1000L)).toInt() + 1)
        .coerceIn(1, kind.rounds)

    Column(
        modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(16.dp))
        Text(
            text = "${exercise.emoji}  ${exercise.title}",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = if (done) "done" else "round $round of ${kind.rounds}",
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
            text = "Take your time. Nothing else is happening.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(20.dp))

        OutlinedButton(onClick = onExit, modifier = Modifier.fillMaxWidth()) {
            Text("Exit")
        }

        Spacer(Modifier.height(10.dp))
        Text(
            text = "No internet · No tracking",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StepsExercise(
    exercise: Exercise,
    kind: ExerciseKind.Steps,
    onDone: () -> Unit,
    onExit: () -> Unit,
) {
    val steps = kind.steps
    var index by rememberSaveable { mutableIntStateOf(0) }
    var remain by remember(index) { mutableStateOf(steps[index].seconds) }

    val step = steps[index]

    fun advance() {
        if (index < steps.lastIndex) index += 1 else onDone()
    }

    LaunchedEffect(index) {
        val total = steps[index].seconds ?: return@LaunchedEffect
        var left = total
        remain = left
        while (left > 0) {
            delay(1000L)
            left -= 1
            remain = left
        }
        advance()
    }

    val fraction = when (val seconds = step.seconds) {
        null -> (index + 1).toFloat() / steps.size
        else -> {
            val left = remain ?: seconds
            ((seconds - left).toFloat() / seconds).coerceIn(0f, 1f)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(16.dp))
        Text(
            text = "${exercise.emoji}  ${exercise.title}",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "step ${index + 1} of ${steps.size}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.weight(1f))

        Text(
            text = step.title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = step.detail,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (step.seconds != null) {
            Spacer(Modifier.height(24.dp))
            Text(
                text = (remain ?: 0).toString(),
                fontSize = 40.sp,
                fontWeight = FontWeight.Light,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        Spacer(Modifier.weight(1f))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.primary),
            )
        }

        Spacer(Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(onClick = onExit, modifier = Modifier.weight(1f)) {
                Text("Exit")
            }
            Button(onClick = { advance() }, modifier = Modifier.weight(1f)) {
                Text(if (index < steps.lastIndex) "Next" else "Finish")
            }
        }

        Spacer(Modifier.height(10.dp))
        Text(
            text = "No internet · No tracking",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CompletionView(
    exercise: Exercise,
    completion: Completion?,
    zenMode: Boolean,
    moodBefore: Int?,
    onMoodBonus: () -> Unit,
    onDone: () -> Unit,
    onAgain: () -> Unit,
) {
    var moodAfter by remember { mutableStateOf<Int?>(null) }
    var bonusGiven by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(28.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(32.dp))
        Text(text = "🌿", fontSize = 44.sp)
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Nice work",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = exercise.title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (!zenMode && completion != null) {
            Spacer(Modifier.height(24.dp))
            Text(
                text = "💎 +${completion.gemsAwarded + if (bonusGiven) 1 else 0}",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(8.dp))
            completion.breakdown.forEach { (label, amount) ->
                Text(
                    text = "$label  +$amount",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (completion.streak > 1) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Day ${completion.streak} streak 🔥",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
            if (completion.plantStageAfter > completion.plantStageBefore) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "🌱 Your plant grew — now: " +
                        PracticeStore.plantStageName(completion.plantStageAfter),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Spacer(Modifier.height(26.dp))
            if (moodAfter == null) {
                Text(
                    text = "How do you feel now?",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MOOD_FACES.forEachIndexed { index, face ->
                        Text(
                            text = face,
                            fontSize = 30.sp,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable {
                                    val choice = index + 1
                                    moodAfter = choice
                                    if (!bonusGiven && moodBefore != null && choice > moodBefore) {
                                        bonusGiven = true
                                        onMoodBonus()
                                    }
                                }
                                .padding(6.dp),
                        )
                    }
                }
            } else {
                Text(
                    text = if (bonusGiven) "Mood lifted — +1 💎" else "Noted 🌿",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (bonusGiven) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }

        Spacer(Modifier.height(36.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(onClick = onAgain, modifier = Modifier.weight(1f)) {
                Text("Again")
            }
            Button(onClick = onDone, modifier = Modifier.weight(1f)) {
                Text("Done")
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}
