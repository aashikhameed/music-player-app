package com.aashik.music.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/**
 * Ultra-fast, zero-overhead Vertical Seekbar for Automotive displays.
 * Clean straight line track without sin() canvas loops or infinite redraws.
 */
@Composable
fun CustomVerticalSeekBar(
    progress: Float,
    onProgressChanged: (Float) -> Unit,
    modifier: Modifier = Modifier,
    @Suppress("UNUSED_PARAMETER") isPlaying: Boolean = false,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
) {
    var barHeightPx by remember { mutableFloatStateOf(1f) }
    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(0f) }

    val displayProgress = if (isDragging) dragProgress else progress.coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .width(22.dp)
            .fillMaxHeight()
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val newProgress = 1f - (offset.y / barHeightPx).coerceIn(0f, 1f)
                    onProgressChanged(newProgress)
                }
            }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        dragProgress = 1f - (offset.y / barHeightPx).coerceIn(0f, 1f)
                    },
                    onDragEnd = {
                        isDragging = false
                        onProgressChanged(dragProgress)
                    },
                    onDragCancel = {
                        isDragging = false
                    }
                ) { change, _ ->
                    change.consume()
                    val newProgress = 1f - (change.position.y / barHeightPx).coerceIn(0f, 1f)
                    dragProgress = newProgress
                    onProgressChanged(newProgress)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        val density = LocalDensity.current
        val strokeWidthPx = with(density) { 4.dp.toPx() }
        val thumbRadiusPx = with(density) { if (isDragging) 7.dp.toPx() else 5.dp.toPx() }

        Canvas(modifier = Modifier.matchParentSize()) {
            barHeightPx = size.height
            val canvasWidth = size.width
            val canvasHeight = size.height
            val centerX = canvasWidth / 2f

            if (canvasHeight <= 0f) return@Canvas

            val progressY = (canvasHeight * (1f - displayProgress)).coerceIn(0f, canvasHeight)

            // Inactive top track
            if (progressY > 0f) {
                drawLine(
                    color = inactiveColor,
                    start = Offset(centerX, 0f),
                    end = Offset(centerX, progressY),
                    strokeWidth = strokeWidthPx,
                    cap = StrokeCap.Round
                )
            }

            // Active bottom straight track (zero CPU math)
            if (progressY < canvasHeight) {
                drawLine(
                    color = activeColor,
                    start = Offset(centerX, canvasHeight),
                    end = Offset(centerX, progressY),
                    strokeWidth = strokeWidthPx,
                    cap = StrokeCap.Round
                )
            }

            // Thumb
            drawCircle(
                color = activeColor,
                radius = thumbRadiusPx,
                center = Offset(centerX, progressY)
            )
            drawCircle(
                color = Color.White,
                radius = (thumbRadiusPx * 0.5f).coerceAtLeast(1.5f),
                center = Offset(centerX, progressY)
            )
        }
    }
}
