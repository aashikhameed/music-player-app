package com.aashik.music.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aashik.music.theme.AppGradients
import kotlin.math.roundToInt

private val TooltipShape = RoundedCornerShape(6.dp)
private val LoadingTrackShape = RoundedCornerShape(3.dp)
private val InactiveDarkTrack = Color(0xFF263245)
private val InactiveLightTrack = Color(0xFFCAD5E2)

/**
 * High-Performance Automotive SeekBar.
 *
 * Optimized for low-power head units with zero animation overhead:
 * - Direct, hardware-accelerated straight-line active & inactive tracks
 * - Interactive thumb with scrub time bubble tooltip
 * - Zero 60fps sinusoid loops or infinite transitions
 */
@Composable
fun CustomHorizontalSeekBar(
    progress: Float,
    onProgressChanged: (Float) -> Unit,
    modifier: Modifier = Modifier,
    @Suppress("UNUSED_PARAMETER") isPlaying: Boolean = false,
    currentPositionMs: Long = 0L,
    durationMs: Long = 0L,
    @Suppress("UNUSED_PARAMETER") waveSeed: Int = 42,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = Color.Unspecified,
    showTimeLabels: Boolean = true,
    isLoading: Boolean = false
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val resolvedInactiveColor = if (inactiveColor != Color.Unspecified) {
        inactiveColor
    } else {
        if (isDark) InactiveDarkTrack else InactiveLightTrack
    }

    if (isLoading) {
        Column(
            modifier = modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp),
                contentAlignment = Alignment.Center
            ) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(LoadingTrackShape),
                    color = activeColor,
                    trackColor = resolvedInactiveColor
                )
            }
            if (showTimeLabels) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Loading audio stream…",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = activeColor
                    )
                    if (durationMs > 0L) {
                        Text(
                            text = formatSeekTime(durationMs),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "LIVE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
        return
    }

    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(0f) }
    var pendingSeekProgress by remember { mutableStateOf<Float?>(null) }
    var barWidthPx by remember { mutableFloatStateOf(1f) }

    // Clear pendingSeekProgress once incoming engine progress catches up to seek target
    androidx.compose.runtime.LaunchedEffect(progress) {
        pendingSeekProgress?.let { target ->
            if (kotlin.math.abs(progress - target) <= 0.025f) {
                pendingSeekProgress = null
            }
        }
    }

    // Auto-expire pendingSeekProgress after 1200ms so it never gets permanently stuck
    androidx.compose.runtime.LaunchedEffect(pendingSeekProgress) {
        if (pendingSeekProgress != null) {
            kotlinx.coroutines.delay(1200L)
            pendingSeekProgress = null
        }
    }

    val displayProgress = when {
        isDragging -> dragProgress
        pendingSeekProgress != null -> pendingSeekProgress!!
        else -> progress.coerceIn(0f, 1f)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Floating Time Tooltip during Drag Scrubbing
        if (isDragging && durationMs > 0) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(18.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                val scrubMs = (displayProgress * durationMs).toLong()
                val tipGradient = AppGradients.primaryButton()

                Box(
                    modifier = Modifier
                        .offset {
                            val xPos = (displayProgress * barWidthPx - 40)
                                .roundToInt()
                                .coerceIn(0, (barWidthPx - 80).roundToInt().coerceAtLeast(0))
                            IntOffset(xPos, 0)
                        }
                        .clip(TooltipShape)
                        .background(brush = tipGradient, shape = TooltipShape)
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

        // High-Performance Track Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .onSizeChanged { barWidthPx = it.width.toFloat() }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val downX = down.position.x
                        if (barWidthPx > 0f) {
                            dragProgress = (downX / barWidthPx).coerceIn(0f, 1f)
                        }
                        isDragging = true

                        val pointerId = down.id
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == pointerId } ?: break

                            if (!change.pressed) {
                                change.consume()
                                break
                            }

                            change.consume()
                            if (barWidthPx > 0f) {
                                dragProgress = (change.position.x / barWidthPx).coerceIn(0f, 1f)
                            }
                        }

                        isDragging = false
                        pendingSeekProgress = dragProgress
                        onProgressChanged(dragProgress)
                    }
                },
            contentAlignment = Alignment.CenterStart
        ) {
            val strokeWidthDp = 4.dp
            val density = LocalDensity.current
            val strokeWidthPx = with(density) { strokeWidthDp.toPx() }
            val thumbRadiusPx = with(density) { if (isDragging) 7.dp.toPx() else 5.5.dp.toPx() }

            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val centerY = canvasHeight / 2f

                if (canvasWidth <= 0f) return@Canvas

                val progressX = (canvasWidth * displayProgress).coerceIn(0f, canvasWidth)

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

                // 2. Draw Active Track (Solid direct line from 0 to progressX)
                if (progressX > 0f) {
                    drawLine(
                        color = activeColor,
                        start = Offset(0f, centerY),
                        end = Offset(progressX, centerY),
                        strokeWidth = strokeWidthPx,
                        cap = StrokeCap.Round
                    )
                }

                // 3. Draw Thumb (Dot at the head of progress)
                drawCircle(
                    color = activeColor,
                    radius = thumbRadiusPx,
                    center = Offset(progressX, centerY)
                )
                // Inner white dot for contrast
                drawCircle(
                    color = Color.White,
                    radius = (thumbRadiusPx * 0.45f).coerceAtLeast(1.5f),
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
                val currentSec = if (isDragging || pendingSeekProgress != null) {
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
    val m = if (minutes < 10) "0$minutes" else minutes.toString()
    val s = if (seconds < 10) "0$seconds" else seconds.toString()
    return "$m:$s"
}
