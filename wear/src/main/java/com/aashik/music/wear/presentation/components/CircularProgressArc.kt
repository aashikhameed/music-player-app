package com.aashik.music.wear.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.aashik.music.wear.theme.WearPrimaryCyan
import com.aashik.music.wear.theme.WearSurfaceCard

/**
 * Draws a bezel-hugging progress arc around the circular Wear OS display.
 * @param progress Value between 0.0f and 1.0f.
 */
@Composable
fun CircularProgressArc(
    progress: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val strokeWidthPx = 4.dp.toPx()
        val diameter = size.minDimension - strokeWidthPx
        val topLeft = Offset(
            (size.width - diameter) / 2f,
            (size.height - diameter) / 2f
        )
        val arcSize = Size(diameter, diameter)

        // Background track (full 360 circle)
        drawArc(
            color = WearSurfaceCard,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidthPx)
        )

        // Active progress arc starting from top (-90 degrees)
        val clampedProgress = progress.coerceIn(0f, 1f)
        if (clampedProgress > 0f) {
            drawArc(
                color = WearPrimaryCyan,
                startAngle = -90f,
                sweepAngle = 360f * clampedProgress,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
            )
        }
    }
}
