package com.aashik.music.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

/**
 * Vertical Android 13+ Style Squiggly Wave Seekbar.
 */
@Composable
fun CustomVerticalSeekBar(
    progress: Float,
    onProgressChanged: (Float) -> Unit,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = false,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
) {
    var barHeightPx by remember { mutableFloatStateOf(1f) }
    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(0f) }

    val displayProgress = if (isDragging) dragProgress else progress.coerceIn(0f, 1f)

    val infiniteTransition = rememberInfiniteTransition(label = "VerticalWaveSeekbar")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "VerticalWavePhase"
    )

    val waveAmplitudeFactor by animateFloatAsState(
        targetValue = if (isPlaying) 1.0f else 0.0f,
        animationSpec = tween(durationMillis = 400),
        label = "VerticalWaveAmplitude"
    )

    val thumbRadius by animateDpAsState(
        targetValue = if (isDragging) 7.dp else 5.dp,
        animationSpec = tween(120),
        label = "VerticalThumbRadius"
    )

    val wavePath = remember { Path() }

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
        val strokeWidthPx = with(density) { 3.5.dp.toPx() }
        val baseAmplitudePx = with(density) { 3.dp.toPx() }
        val wavelengthPx = with(density) { 20.dp.toPx() }

        Canvas(modifier = Modifier.matchParentSize()) {
            barHeightPx = size.height
            val canvasWidth = size.width
            val canvasHeight = size.height
            val centerX = canvasWidth / 2f

            if (canvasHeight <= 0f) return@Canvas

            val progressY = (canvasHeight * (1f - displayProgress)).coerceIn(0f, canvasHeight)
            val currentAmplitude = baseAmplitudePx * waveAmplitudeFactor

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

            // Active bottom squiggly track
            if (progressY < canvasHeight) {
                wavePath.reset()
                wavePath.moveTo(centerX, canvasHeight)

                if (currentAmplitude <= 0.2f || (canvasHeight - progressY) < 10f) {
                    wavePath.lineTo(centerX, progressY)
                } else {
                    val stepPx = 3f
                    var y = canvasHeight
                    while (y >= progressY) {
                        val distFromBottom = canvasHeight - y
                        val distFromThumb = y - progressY

                        val startTaper = (distFromBottom / (wavelengthPx * 0.75f)).coerceIn(0f, 1f)
                        val endTaper = (distFromThumb / (wavelengthPx * 0.75f)).coerceIn(0f, 1f)
                        val taper = (startTaper * endTaper).coerceIn(0f, 1f)

                        val waveX = centerX + sin((distFromBottom / wavelengthPx) * 2 * PI.toFloat() - wavePhase) * currentAmplitude * taper
                        wavePath.lineTo(waveX, y)
                        y -= stepPx
                    }
                    wavePath.lineTo(centerX, progressY)
                }

                drawPath(
                    path = wavePath,
                    color = activeColor,
                    style = Stroke(
                        width = strokeWidthPx,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }

            // Thumb
            drawCircle(
                color = activeColor,
                radius = thumbRadius.toPx(),
                center = Offset(centerX, progressY)
            )
            drawCircle(
                color = Color.White,
                radius = (thumbRadius.toPx() * 0.5f).coerceAtLeast(1.5f),
                center = Offset(centerX, progressY)
            )
        }
    }
}
