package com.dtox.app.ui.practice

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * The practice plant, drawn entirely in code — no image assets, so it costs
 * the APK nothing. Grows through 8 stages as sessions accumulate and dulls
 * gently after a few missed days (it never dies and never shrinks).
 */
@Composable
fun PlantView(stage: Int, wilting: Boolean, modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val soil = MaterialTheme.colorScheme.surfaceVariant

    val green = if (wilting) lerp(primary, Color(0xFF9A8B6A), 0.55f) else primary
    val leaf = if (wilting) lerp(primary, Color(0xFF8E9B6F), 0.35f) else lerp(primary, Color.White, 0.18f)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val baseY = h * 0.88f

        // soil mound
        drawOval(
            color = soil,
            topLeft = Offset(w * 0.2f, baseY - h * 0.02f),
            size = Size(w * 0.6f, h * 0.1f),
        )

        val stages = stage.coerceIn(0, 7)
        if (stages == 0) {
            drawOval(
                color = green,
                topLeft = Offset(cx - w * 0.025f, baseY - h * 0.035f),
                size = Size(w * 0.05f, h * 0.05f),
            )
            return@Canvas
        }

        val stemHeight = h * (0.14f + 0.075f * (stages - 1).coerceAtMost(6))
        val stemTop = baseY - stemHeight
        val stemWidth = (2.5f + stages * 0.4f).dp.toPx()

        drawLine(
            color = green,
            start = Offset(cx, baseY),
            end = Offset(cx, stemTop),
            strokeWidth = stemWidth,
            cap = StrokeCap.Round,
        )

        val leafCount = when (stages) {
            1, 2 -> 2
            3, 4 -> 4
            5 -> 5
            6 -> 6
            else -> 4
        }
        val leafLen = w * (0.10f + 0.012f * stages)
        val leafHei = h * 0.035f

        for (i in 0 until leafCount) {
            val frac = if (leafCount == 1) 0.5f else i / (leafCount - 1f)
            val y = baseY - stemHeight * (0.3f + 0.6f * frac)
            val side = if (i % 2 == 0) -1f else 1f
            rotate(degrees = side * 38f, pivot = Offset(cx, y)) {
                drawOval(
                    color = leaf,
                    topLeft = Offset(cx, y - leafHei / 2f),
                    size = Size(leafLen, leafHei),
                )
            }
        }

        when (stages) {
            4 -> drawCircle(color = secondary, radius = w * 0.022f, center = Offset(cx, stemTop))
            5, 6 -> {
                val petalR = w * (if (stages == 5) 0.02f else 0.028f)
                val ringR = w * (if (stages == 5) 0.035f else 0.045f)
                for (a in 0 until 6) {
                    val ang = Math.toRadians((a * 60).toDouble())
                    drawCircle(
                        color = secondary,
                        radius = petalR,
                        center = Offset(
                            cx + (ringR * cos(ang)).toFloat(),
                            stemTop + (ringR * sin(ang)).toFloat(),
                        ),
                    )
                }
                drawCircle(color = Color(0xFFE8C468), radius = w * 0.018f, center = Offset(cx, stemTop))
            }

            7 -> {
                drawCircle(color = leaf, radius = w * 0.16f, center = Offset(cx, stemTop - h * 0.02f))
                drawCircle(color = leaf, radius = w * 0.12f, center = Offset(cx - w * 0.14f, stemTop + h * 0.02f))
                drawCircle(color = leaf, radius = w * 0.12f, center = Offset(cx + w * 0.14f, stemTop + h * 0.02f))
            }
        }
    }
}
