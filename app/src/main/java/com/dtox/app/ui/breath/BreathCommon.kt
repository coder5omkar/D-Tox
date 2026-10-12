package com.dtox.app.ui.breath

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dtox.app.data.BreathPattern
import com.dtox.app.data.Phase
import com.dtox.app.data.PhaseKind

/** Which phase of [pattern] we are in after [elapsedMs] milliseconds. */
fun phaseIndexAt(pattern: BreathPattern, elapsedMs: Long): Int {
    val phases = pattern.phases
    val cycleMs = pattern.cycleSeconds * 1000L
    val pos = elapsedMs % cycleMs
    var acc = 0L
    for (i in phases.indices) {
        if (pos < acc + phases[i].seconds * 1000L) return i
        acc += phases[i].seconds * 1000L
    }
    return phases.lastIndex
}

/** Target circle scale: full on inhale, small on exhale, holds keep the last one. */
fun targetScaleFor(phases: List<Phase>, phaseIndex: Int): Float {
    for (i in phaseIndex downTo 0) {
        when (phases[i].kind) {
            PhaseKind.IN, PhaseKind.SIP -> return 1f
            PhaseKind.OUT -> return 0.6f
            PhaseKind.HOLD -> Unit
        }
    }
    return 0.6f
}

/** Eased circle scale for the current phase. */
@Composable
fun rememberBreathScale(target: Float, animationMs: Int): Float {
    val scale by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = animationMs, easing = LinearOutSlowInEasing),
        label = "breathScale",
    )
    return scale
}

/**
 * The breathing visual: progress ring, soft gradient circle that grows and
 * shrinks, with centre text. Colours follow the active theme, so the same
 * component works on every palette.
 */
@Composable
fun BreathingRing(
    fraction: Float,
    scale: Float,
    centerTop: String,
    centerMain: String?,
    centerSub: String?,
    modifier: Modifier = Modifier,
) {
    val ringTrack = MaterialTheme.colorScheme.surfaceVariant
    val ringFill = MaterialTheme.colorScheme.primary
    val circleInner = MaterialTheme.colorScheme.primaryContainer
    val circleOuter = MaterialTheme.colorScheme.surface
    val onBackground = MaterialTheme.colorScheme.onBackground
    val onVariant = MaterialTheme.colorScheme.onSurfaceVariant

    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        val ringSize = maxWidth.coerceAtMost(320.dp)
        val innerSize = ringSize * 0.77f

        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(ringSize)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 10.dp.toPx()
                val inset = strokeWidth / 2f
                val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                val topLeft = Offset(inset, inset)

                drawArc(
                    color = ringTrack,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )

                if (fraction > 0f) {
                    drawArc(
                        color = ringFill,
                        startAngle = -90f,
                        sweepAngle = 360f * fraction.coerceIn(0f, 1f),
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(innerSize)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .background(
                        brush = Brush.radialGradient(listOf(circleInner, circleOuter)),
                        shape = CircleShape,
                    ),
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = centerTop,
                    style = MaterialTheme.typography.titleMedium,
                    color = onBackground,
                )
                if (centerMain != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = centerMain,
                        fontSize = 46.sp,
                        fontWeight = FontWeight.Light,
                        color = onBackground,
                    )
                }
                if (centerSub != null) {
                    Text(
                        text = centerSub,
                        style = MaterialTheme.typography.labelMedium,
                        color = onVariant,
                    )
                }
            }
        }
    }
}
