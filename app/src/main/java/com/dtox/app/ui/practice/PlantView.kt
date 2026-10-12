package com.dtox.app.ui.practice

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import kotlin.math.cos
import kotlin.math.sin

/**
 * The practice plant, drawn entirely in code — no image assets, so it costs
 * the APK nothing. Grows through 8 stages as sessions accumulate and dulls
 * gently after a few missed days (it never dies and never shrinks).
 *
 * Drawn from a single [size.minDimension] unit so it reads well in both the
 * compact home card and any larger placement.
 */
@Composable
fun PlantView(stage: Int, wilting: Boolean, modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val soil = MaterialTheme.colorScheme.surfaceVariant

    val green = if (wilting) lerp(primary, Color(0xFF9A8B6A), 0.5f) else primary
    val leaf = if (wilting) lerp(primary, Color(0xFF8E9B6F), 0.3f) else lerp(primary, Color.White, 0.12f)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val baseY = h * 0.86f
        val unit = size.minDimension

        // soil mound
        val moundW = w * 0.42f
        val moundH = h * 0.20f
        drawArc(
            color = soil,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = true,
            topLeft = Offset(cx - moundW / 2f, baseY - moundH / 2f),
            size = Size(moundW, moundH),
        )

        val stages = stage.coerceIn(0, 7)

        if (stages == 0) {
            // a seed resting on the mound, tilted
            rotate(degrees = -24f, pivot = Offset(cx, baseY - unit * 0.05f)) {
                drawOval(
                    color = green,
                    topLeft = Offset(cx - unit * 0.10f, baseY - unit * 0.11f),
                    size = Size(unit * 0.20f, unit * 0.13f),
                )
            }
            return@Canvas
        }

        val stemHeight = h * (0.30f + 0.08f * (stages - 1).coerceAtMost(6))
        val stemTop = baseY - stemHeight
        val stemWidth = unit * (0.055f + 0.012f * stages)

        drawLine(
            color = green,
            start = Offset(cx, baseY),
            end = Offset(cx, stemTop),
            strokeWidth = stemWidth,
            cap = StrokeCap.Round,
        )

        val leafCount = when (stages) {
            1, 2 -> 2
            3 -> 3
            4 -> 4
            5 -> 4
            6 -> 5
            else -> 4
        }
        val leafLen = unit * (0.34f + 0.04f * stages)
        val leafWid = leafLen * 0.36f

        for (i in 0 until leafCount) {
            val frac = if (leafCount == 1) 0.5f else i / (leafCount - 1f)
            val y = baseY - stemHeight * (0.32f + 0.58f * frac)
            val right = i % 2 == 0
            drawLeaf(
                anchorX = cx,
                anchorY = y,
                length = leafLen,
                width = leafWid,
                angleDeg = if (right) -30f else -150f,
                color = leaf,
            )
        }

        when (stages) {
            4 -> drawCircle(color = secondary, radius = unit * 0.07f, center = Offset(cx, stemTop))

            5, 6 -> {
                val petalR = unit * (if (stages == 5) 0.055f else 0.07f)
                val ringR = unit * (if (stages == 5) 0.11f else 0.14f)
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
                drawCircle(
                    color = Color(0xFFE8C468),
                    radius = unit * 0.05f,
                    center = Offset(cx, stemTop),
                )
            }

            7 -> {
                val canopyY = stemTop + unit * 0.10f
                drawCircle(color = leaf, radius = unit * 0.16f, center = Offset(cx, canopyY))
                drawCircle(color = leaf, radius = unit * 0.12f, center = Offset(cx - unit * 0.22f, canopyY + unit * 0.06f))
                drawCircle(color = leaf, radius = unit * 0.12f, center = Offset(cx + unit * 0.22f, canopyY + unit * 0.06f))
            }
        }
    }
}

/** A pointed leaf shape (two cubic curves) rooted at the anchor and rotated. */
private fun DrawScope.drawLeaf(
    anchorX: Float,
    anchorY: Float,
    length: Float,
    width: Float,
    angleDeg: Float,
    color: Color,
) {
    rotate(degrees = angleDeg, pivot = Offset(anchorX, anchorY)) {
        val path = Path().apply {
            moveTo(anchorX, anchorY)
            cubicTo(
                anchorX + length * 0.25f, anchorY - width,
                anchorX + length * 0.75f, anchorY - width,
                anchorX + length, anchorY,
            )
            cubicTo(
                anchorX + length * 0.75f, anchorY + width,
                anchorX + length * 0.25f, anchorY + width,
                anchorX, anchorY,
            )
            close()
        }
        drawPath(path, color)
    }
}
