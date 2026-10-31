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

private val WAVEFORM_BASE_AMPLITUDES = floatArrayOf(
    0.35f, 0.50f, 0.70f, 0.40f, 0.65f, 0.85f, 0.45f, 0.30f,
    0.55f, 0.75f, 0.90f, 0.60f, 0.40f, 0.70f, 0.80f, 0.50f,
    0.30f, 0.60f, 0.85f, 1.00f, 0.75f, 0.50f, 0.35f, 0.65f,
    0.80f, 0.95f, 0.70f, 0.45f, 0.60f, 0.80f, 0.55f, 0.35f,
    0.50f, 0.75f, 0.90f, 0.65f, 0.40f, 0.60f, 0.85f, 0.70f,
    0.50f, 0.35f, 0.55f, 0.70f, 0.45f, 0.30f, 0.40f, 0.25f
)
private const val NUM_WAVE_BARS = 48

/**
 * High-Performance Telemetry Waveform Scrubber.
 *
 * Optimized for low-power head units with zero animation overhead:
 * - 48-bar dynamic waveform canvas unrolled with zero runtime allocations
 * - Deterministic visual acoustic fingerprint per track (via waveSeed)
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
    waveSeed: Int = 42,
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
        // Floating Time Tooltip during Drag Scrubbing — fixed-height row so layout never shifts
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (isDragging && durationMs > 0) {
                val scrubMs = (displayProgress * durationMs).toLong()

                Box(
                    modifier = Modifier
                        .offset {
                            val xPos = (displayProgress * barWidthPx - 40)
                                .roundToInt()
                                .coerceIn(0, (barWidthPx - 80).roundToInt().coerceAtLeast(0))
                            IntOffset(xPos, 0)
                        }
                        .background(color = activeColor, shape = TooltipShape)
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

        // High-Performance Telemetry Waveform Track Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(26.dp)
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
            val density = LocalDensity.current
            val thumbRadiusPx = with(density) { if (isDragging) 7.5.dp.toPx() else 6.dp.toPx() }

            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val centerY = canvasHeight / 2f

                if (canvasWidth <= 0f) return@Canvas

                val progressX = (canvasWidth * displayProgress).coerceIn(0f, canvasWidth)
                val step = canvasWidth / NUM_WAVE_BARS
                val barWidth = (step * 0.58f).coerceIn(2.5f, 7.5f)
                val seedOffset = kotlin.math.abs(waveSeed) % NUM_WAVE_BARS

                // 1. Draw 48 dynamic waveform bars with rounded caps
                for (i in 0 until NUM_WAVE_BARS) {
                    val barX = i * step + step / 2f
                    val amp = WAVEFORM_BASE_AMPLITUDES[(i + seedOffset) % NUM_WAVE_BARS]
                    val barHalfHeight = ((canvasHeight * 0.42f) * amp).coerceAtLeast(3.5f)

                    val isPlayed = barX <= progressX
                    val barColor = if (isPlayed) activeColor else resolvedInactiveColor

                    drawLine(
                        color = barColor,
                        start = Offset(barX, centerY - barHalfHeight),
                        end = Offset(barX, centerY + barHalfHeight),
                        strokeWidth = barWidth,
                        cap = StrokeCap.Round
                    )
                }

                // 2. Telemetry playhead indicator & glowing thumb
                drawCircle(
                    color = activeColor,
                    radius = thumbRadiusPx,
                    center = Offset(progressX, centerY)
                )
                // Inner white contrast core
                drawCircle(
                    color = Color.White,
                    radius = (thumbRadiusPx * 0.42f).coerceAtLeast(1.5f),
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
