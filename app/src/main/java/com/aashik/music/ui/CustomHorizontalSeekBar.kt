package com.aashik.music.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import kotlin.math.roundToInt

private val TooltipShape = RoundedCornerShape(6.dp)
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
 * Isolated phase animator that runs only during active audio stream loading / buffering.
 * Returns 0f immediately and completely disposes the animation when not loading,
 * ensuring 0 CPU / GPU overhead and zero frame ticks on automotive headunits.
 */
@Composable
private fun rememberLoadingPhase(isLoading: Boolean): Float {
    if (!isLoading) return 0f
    val transition = rememberInfiniteTransition(label = "SeekerLoadingTransition")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SeekerLoadingPhase"
    )
    return phase
}

/**
 * High-Performance Telemetry Waveform Scrubber with Zero-Layout-Jump Loading Animation.
 *
 * Optimized for low-power head units:
 * - 48-bar dynamic waveform canvas unrolled with zero runtime allocations
 * - Deterministic visual acoustic fingerprint per track (via waveSeed)
 * - Custom acoustic radar sweep loading animation that perfectly preserves layout height (0px layout shifts)
 * - Interactive thumb with scrub time bubble tooltip
 * - Zero CPU / GPU overhead during normal playback
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

    val loadingPhase = rememberLoadingPhase(isLoading)

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
        // Floating Time Tooltip during Drag Scrubbing — fixed 18.dp row so layout never shifts
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (isDragging && durationMs > 0L) {
                val scrubMs = (displayProgress * durationMs).toLong()

                Box(
                    modifier = Modifier
                        .offset {
                            val tipWidthPx = 48.dp.roundToPx()
                            val offsetX = ((displayProgress * barWidthPx) - (tipWidthPx / 2f))
                                .coerceIn(0f, (barWidthPx - tipWidthPx).coerceAtLeast(0f))
                            IntOffset(offsetX.roundToInt(), 0)
                        }
                        .background(color = activeColor, shape = TooltipShape)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = formatSeekTime(scrubMs),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        // Interactive 48-Bar Dynamic Waveform Scrubber / Loading Sweep — fixed 28.dp height
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .onSizeChanged { barWidthPx = it.width.toFloat() }
                .pointerInput(isLoading) {
                    if (isLoading) return@pointerInput
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

                val step = canvasWidth / NUM_WAVE_BARS
                val barWidth = (step * 0.58f).coerceIn(2.5f, 7.5f)
                val seedOffset = kotlin.math.abs(waveSeed) % NUM_WAVE_BARS

                if (isLoading) {
                    // ── Custom Automotive Radar / Acoustic Sweep Loading Animation ──
                    val scanCenterX = canvasWidth * loadingPhase
                    val pulseRadiusPx = canvasWidth * 0.18f
                    val restingColor = resolvedInactiveColor.copy(alpha = if (isDark) 0.35f else 0.5f)

                    for (i in 0 until NUM_WAVE_BARS) {
                        val barX = i * step + step / 2f
                        val amp = WAVEFORM_BASE_AMPLITUDES[(i + seedOffset) % NUM_WAVE_BARS]

                        val distPx = kotlin.math.abs(barX - scanCenterX)
                        val normalizedDist = (1f - (distPx / pulseRadiusPx)).coerceIn(0f, 1f)
                        // Smoothstep curve for fluid bell-shaped intensity
                        val intensity = normalizedDist * normalizedDist * (3f - 2f * normalizedDist)

                        val baseHalf = ((canvasHeight * 0.28f) * amp).coerceAtLeast(2.5f)
                        val peakHalf = ((canvasHeight * 0.44f) * amp).coerceAtLeast(3.5f)
                        val barHalfHeight = baseHalf + (peakHalf - baseHalf) * intensity

                        val barColor = if (intensity > 0.02f) {
                            androidx.compose.ui.graphics.lerp(restingColor, activeColor, intensity)
                        } else {
                            restingColor
                        }

                        drawLine(
                            color = barColor,
                            start = Offset(barX, centerY - barHalfHeight),
                            end = Offset(barX, centerY + barHalfHeight),
                            strokeWidth = barWidth,
                            cap = StrokeCap.Round
                        )
                    }

                    // Luminous tracer playhead gliding with the scan wave
                    drawCircle(
                        color = activeColor.copy(alpha = 0.22f),
                        radius = thumbRadiusPx * 1.6f,
                        center = Offset(scanCenterX, centerY)
                    )
                    drawCircle(
                        color = activeColor,
                        radius = thumbRadiusPx * 0.85f,
                        center = Offset(scanCenterX, centerY)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = (thumbRadiusPx * 0.38f).coerceAtLeast(1.5f),
                        center = Offset(scanCenterX, centerY)
                    )
                } else {
                    // ── Normal Playback Waveform Scrubber ──
                    val progressX = (canvasWidth * displayProgress).coerceIn(0f, canvasWidth)

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

                    // Telemetry playhead indicator & glowing thumb
                    drawCircle(
                        color = activeColor,
                        radius = thumbRadiusPx,
                        center = Offset(progressX, centerY)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = (thumbRadiusPx * 0.42f).coerceAtLeast(1.5f),
                        center = Offset(progressX, centerY)
                    )
                }
            }
        }

        // Built-in Time Labels — fixed 20.dp height so layout never shifts
        if (showTimeLabels) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isLoading) {
                    Text(
                        text = "Buffering stream…",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = activeColor,
                        maxLines = 1
                    )
                    if (durationMs > 0L) {
                        Text(
                            text = formatSeekTime(durationMs),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    } else {
                        Text(
                            text = "LIVE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00E676),
                            maxLines = 1
                        )
                    }
                } else {
                    val currentSec = if (isDragging || pendingSeekProgress != null) {
                        (displayProgress * durationMs).toLong()
                    } else {
                        currentPositionMs
                    }

                    Text(
                        text = formatSeekTime(currentSec),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )

                    if (durationMs > 0L) {
                        Text(
                            text = formatSeekTime(durationMs),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    } else {
                        Text(
                            text = "LIVE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00E676),
                            maxLines = 1
                        )
                    }
                }
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
