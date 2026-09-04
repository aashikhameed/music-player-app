package com.aashik.music.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.draw.clip
import com.aashik.music.theme.AppGradients
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Android 13+ / Material You Squiggly Waveform SeekBar.
 *
 * Signature Android Media Player feature:
 * - Active played track is a smooth, continuous squiggly/wavy line that undulates while playing
 * - Unplayed inactive track is a clean, straight horizontal track line
 * - Smoothly flattens to a straight line when paused
 * - Interactive thumb with scrub time bubble tooltip
 * - Ultra-lightweight direct Path rendering optimized for low-end devices
 */
@Composable
fun CustomHorizontalSeekBar(
    progress: Float,
    onProgressChanged: (Float) -> Unit,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = false,
    currentPositionMs: Long = 0L,
    durationMs: Long = 0L,
    @Suppress("UNUSED_PARAMETER") waveSeed: Int = 42,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = Color.Unspecified,
    showTimeLabels: Boolean = true
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val resolvedInactiveColor = if (inactiveColor != Color.Unspecified) {
        inactiveColor
    } else {
        if (isDark) Color(0xFF263245) else Color(0xFFCAD5E2)
    }

    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(0f) }
    var barWidthPx by remember { mutableFloatStateOf(1f) }

    val displayProgress = if (isDragging) dragProgress else progress.coerceIn(0f, 1f)

    // Android 13 Wave Traveling Animation while playing
    val infiniteTransition = rememberInfiniteTransition(label = "AndroidWaveSeekbar")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "WavePhase"
    )

    val thumbRadius by animateDpAsState(
        targetValue = if (isDragging) 7.dp else 5.dp,
        animationSpec = tween(120),
        label = "ThumbRadius"
    )

    // Reusable Path to avoid garbage collection allocations during 60fps render loop
    val wavePath = remember { Path() }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Floating Time Tooltip during Drag Scrubbing (Only takes space when dragging)
        if (isDragging && durationMs > 0) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(18.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                val scrubMs = (displayProgress * durationMs).toLong()

                val tipShape = RoundedCornerShape(6.dp)
                val tipGradient = AppGradients.primaryButton()

                Box(
                    modifier = Modifier
                        .offset {
                            val xPos = (displayProgress * barWidthPx - 40)
                                .roundToInt()
                                .coerceIn(0, (barWidthPx - 80).roundToInt().coerceAtLeast(0))
                            IntOffset(xPos, 0)
                        }
                        .clip(tipShape)
                        .background(brush = tipGradient, shape = tipShape)
                ) {
                    Text(
                        text = formatSeekTime(scrubMs),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        // Android 13 Squiggly Wave Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val newProgress = (offset.x / barWidthPx).coerceIn(0f, 1f)
                        onProgressChanged(newProgress)
                    }
                }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            dragProgress = (offset.x / barWidthPx).coerceIn(0f, 1f)
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
                        val newProgress = (change.position.x / barWidthPx).coerceIn(0f, 1f)
                        dragProgress = newProgress
                        onProgressChanged(newProgress)
                    }
                },
            contentAlignment = Alignment.CenterStart
        ) {
            val strokeWidthDp = 3.5.dp
            val density = LocalDensity.current
            val strokeWidthPx = with(density) { strokeWidthDp.toPx() }
            val baseAmplitudePx = with(density) { 3.5.dp.toPx() }
            val wavelengthPx = with(density) { 22.dp.toPx() }

            Canvas(modifier = Modifier.fillMaxSize()) {
                barWidthPx = size.width
                val canvasWidth = size.width
                val canvasHeight = size.height
                val centerY = canvasHeight / 2f

                if (canvasWidth <= 0f) return@Canvas

                val progressX = (canvasWidth * displayProgress).coerceIn(0f, canvasWidth)
                val currentAmplitude = baseAmplitudePx * (if (isPlaying) 1.0f else 0.8f)

                // 1. Draw Inactive Track (Clean straight line from progressX to end)
                if (progressX < canvasWidth) {
                    drawLine(
                        color = resolvedInactiveColor,
                        start = Offset(progressX, centerY),
                        end = Offset(canvasWidth, centerY),
                        strokeWidth = strokeWidthPx,
                        cap = StrokeCap.Round
                    )
                }

                // 2. Draw Active Track (Android 13 Squiggly Wavy Line from 0 to progressX)
                if (progressX > 0f) {
                    wavePath.reset()
                    wavePath.moveTo(0f, centerY)

                    if (progressX < 8f) {
                        wavePath.lineTo(progressX, centerY)
                    } else {
                        // Continuous sine wave
                        val stepPx = 2f // 2px step for ultra high-fidelity curves
                        val effectivePhase = if (isPlaying) wavePhase else 0f
                        var x = 0f
                        while (x <= progressX) {
                            val startTaper = (x / (wavelengthPx * 0.4f)).coerceIn(0f, 1f)
                            val endTaper = ((progressX - x) / (wavelengthPx * 0.4f)).coerceIn(0f, 1f)
                            val taper = startTaper * endTaper

                            val waveY = centerY + sin((x / wavelengthPx) * 2 * PI.toFloat() - effectivePhase) * currentAmplitude * taper
                            wavePath.lineTo(x, waveY)
                            x += stepPx
                        }
                        wavePath.lineTo(progressX, centerY)
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

                // 3. Draw Thumb (Material 3 dot at the head of the wave)
                drawCircle(
                    color = activeColor,
                    radius = thumbRadius.toPx(),
                    center = Offset(progressX, centerY)
                )
                // Inner white dot
                drawCircle(
                    color = Color.White,
                    radius = (thumbRadius.toPx() * 0.5f).coerceAtLeast(1.5f),
                    center = Offset(progressX, centerY)
                )
            }
        }

        // Built-in Time Labels
        if (showTimeLabels && durationMs > 0L) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val currentSec = if (isDragging) {
                    (displayProgress * durationMs).toLong()
                } else {
                    currentPositionMs
                }

                Text(
                    text = formatSeekTime(currentSec),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = formatSeekTime(durationMs),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatSeekTime(timeMs: Long): String {
    val totalSeconds = (timeMs / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
