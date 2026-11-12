package com.aashik.music.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp

private val EqCurveContainerShape = RoundedCornerShape(8.dp)
private val EqCurveBg = Color(0xFF10141D)
private val EqCurveBorder = Color(0xFF1E293B)
private val EqCurveGridColor = Color(0x3394A3B8)
private val EqCurveLineColor = Color(0xFF00E5FF)
private val EqCurveBypassColor = Color(0x6694A3B8)

/**
 * Direct Canvas visual frequency response curve polyline.
 * Hermite cubic spline interpolation across equalizer band gains.
 * Geometry is measured in Modifier.onSizeChanged to protect Mali GPU fill rate.
 */
@Composable
fun EqualizerCurveCanvas(
    bandGains: List<Float>,
    isEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    var canvasWidth by remember { mutableFloatStateOf(1f) }
    var canvasHeight by remember { mutableFloatStateOf(1f) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(EqCurveBg, shape = EqCurveContainerShape)
            .onSizeChanged {
                canvasWidth = it.width.toFloat()
                canvasHeight = it.height.toFloat()
            }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val midY = h / 2f
            val maxDbOffset = (h / 2f) - 6f

            // Reference lines: +12dB, 0dB, -12dB
            drawLine(
                color = EqCurveGridColor,
                start = Offset(0f, midY - maxDbOffset),
                end = Offset(w, midY - maxDbOffset),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = EqCurveGridColor.copy(alpha = 0.45f),
                start = Offset(0f, midY),
                end = Offset(w, midY),
                strokeWidth = 1.2.dp.toPx()
            )
            drawLine(
                color = EqCurveGridColor,
                start = Offset(0f, midY + maxDbOffset),
                end = Offset(w, midY + maxDbOffset),
                strokeWidth = 1.dp.toPx()
            )

            if (bandGains.isEmpty()) return@Canvas

            val activeColor = if (isEnabled) EqCurveLineColor else EqCurveBypassColor
            val count = bandGains.size
            val points = ArrayList<Offset>(count + 2)

            // Left edge clamping point
            val firstGain = if (isEnabled) bandGains.first().coerceIn(-12f, 12f) else 0f
            val firstY = midY - (firstGain / 12f) * maxDbOffset
            points.add(Offset(0f, firstY))

            for (i in 0 until count) {
                val gain = if (isEnabled) bandGains[i].coerceIn(-12f, 12f) else 0f
                val x = (i.toFloat() / (count - 1).coerceAtLeast(1)) * w
                val y = midY - (gain / 12f) * maxDbOffset
                points.add(Offset(x, y))
            }

            // Right edge clamping point
            val lastGain = if (isEnabled) bandGains.last().coerceIn(-12f, 12f) else 0f
            val lastY = midY - (lastGain / 12f) * maxDbOffset
            points.add(Offset(w, lastY))

            // Hermite / Cubic Spline interpolation
            val path = Path()
            path.moveTo(points[0].x, points[0].y)
            for (i in 0 until points.size - 1) {
                val p0 = if (i > 0) points[i - 1] else points[i]
                val p1 = points[i]
                val p2 = points[i + 1]
                val p3 = if (i + 2 < points.size) points[i + 2] else p2

                val cp1x = p1.x + (p2.x - p0.x) / 6f
                val cp1y = p1.y + (p2.y - p0.y) / 6f
                val cp2x = p2.x - (p3.x - p1.x) / 6f
                val cp2y = p2.y - (p3.y - p1.y) / 6f

                path.cubicTo(cp1x, cp1y, cp2x, cp2y, p2.x, p2.y)
            }

            drawPath(
                path = path,
                color = activeColor,
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
            )

            // Draw band nodes
            for (i in 0 until count) {
                val gain = if (isEnabled) bandGains[i].coerceIn(-12f, 12f) else 0f
                val x = (i.toFloat() / (count - 1).coerceAtLeast(1)) * w
                val y = midY - (gain / 12f) * maxDbOffset
                drawCircle(
                    color = activeColor,
                    radius = 3.dp.toPx(),
                    center = Offset(x, y)
                )
            }
        }
    }
}

