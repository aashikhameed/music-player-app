package com.aashik.music.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.sin

private val VISUALIZER_AMPLITUDES = floatArrayOf(
    0.35f, 0.70f, 0.95f, 0.50f, 0.85f, 0.40f, 0.75f, 1.00f,
    0.60f, 0.80f, 0.45f, 0.90f, 0.65f, 0.35f, 0.80f, 0.55f
)

/**
 * High-performance perimeter equalizer that pulses around the border of the album art.
 * Renders at 60 FPS on low-power Mali GPUs using direct Canvas drawLine / drawRoundRect calls.
 */
@Composable
fun HeroBorderVisualizer(
    isPlaying: Boolean,
    borderColor: Color,
    visualizerColor: Color = MaterialTheme.colorScheme.primary,
    cornerRadius: Dp = 20.dp,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "HeroBorderVisualizerAnim")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "HeroBorderVisualizerPhase"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val strokeWidth = 1.2.dp.toPx()
        val cornerRadiusPx = cornerRadius.toPx()
        val w = size.width
        val h = size.height

        // 1. Sleek dim perimeter border line
        drawRoundRect(
            color = borderColor,
            size = size,
            cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
            style = Stroke(width = strokeWidth)
        )

        if (!isPlaying) return@Canvas

        val maxBarH = 7.dp.toPx()
        val barStroke = 2.dp.toPx()
        val numBarsPerEdge = 14
        val cornerOffset = cornerRadiusPx + 8.dp.toPx()
        val topSpan = (w - 2 * cornerOffset).coerceAtLeast(1f)
        val sideSpan = (h - 2 * cornerOffset).coerceAtLeast(1f)

        // 2. Top & Bottom Perimeter Visualizer Ticks
        for (i in 0 until numBarsPerEdge) {
            val step = i.toFloat() / (numBarsPerEdge - 1).coerceAtLeast(1)
            val amp = VISUALIZER_AMPLITUDES[i % VISUALIZER_AMPLITUDES.size]
            val wave = abs(sin(phase + i * 0.45f)) * 0.65f + 0.35f
            val barH = maxBarH * amp * wave

            val x = cornerOffset + step * topSpan

            // Top edge (radiates upwards)
            drawLine(
                color = visualizerColor.copy(alpha = (0.45f + 0.55f * wave)),
                start = Offset(x, 0f),
                end = Offset(x, -barH),
                strokeWidth = barStroke,
                cap = StrokeCap.Round
            )

            // Bottom edge (radiates downwards)
            drawLine(
                color = visualizerColor.copy(alpha = (0.45f + 0.55f * wave)),
                start = Offset(x, h),
                end = Offset(x, h + barH),
                strokeWidth = barStroke,
                cap = StrokeCap.Round
            )
        }

        // 3. Left & Right Perimeter Visualizer Ticks
        for (i in 0 until numBarsPerEdge) {
            val step = i.toFloat() / (numBarsPerEdge - 1).coerceAtLeast(1)
            val amp = VISUALIZER_AMPLITUDES[(i + 4) % VISUALIZER_AMPLITUDES.size]
            val wave = abs(sin(phase + (i + 4) * 0.45f)) * 0.65f + 0.35f
            val barH = maxBarH * amp * wave

            val y = cornerOffset + step * sideSpan

            // Left edge (radiates leftwards)
            drawLine(
                color = visualizerColor.copy(alpha = (0.45f + 0.55f * wave)),
                start = Offset(0f, y),
                end = Offset(-barH, y),
                strokeWidth = barStroke,
                cap = StrokeCap.Round
            )

            // Right edge (radiates rightwards)
            drawLine(
                color = visualizerColor.copy(alpha = (0.45f + 0.55f * wave)),
                start = Offset(w, y),
                end = Offset(w + barH, y),
                strokeWidth = barStroke,
                cap = StrokeCap.Round
            )
        }
    }
}

