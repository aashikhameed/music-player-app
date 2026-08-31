package com.aashik.music.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min

@Composable
fun CustomHorizontalSeekBar(
    progress: Float,
    onProgressChanged: (Float) -> Unit,
    modifier: Modifier = Modifier,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    progressColor: Color = MaterialTheme.colorScheme.primary
) {
    var barWidthPx by remember { mutableFloatStateOf(1f) }
    var isDragging by remember { mutableStateOf(false) }
    val density = LocalDensity.current

    val trackHeight by animateDpAsState(
        targetValue = if (isDragging) 6.dp else 4.dp,
        label = "SeekbarTrackHeight"
    )

    val thumbSize by animateDpAsState(
        targetValue = if (isDragging) 16.dp else 12.dp,
        label = "SeekbarThumbSize"
    )

    // Touch Container with generous 24dp hit area
    Box(
        modifier = modifier
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
                    onDragStart = { isDragging = true },
                    onDragEnd = { isDragging = false },
                    onDragCancel = { isDragging = false }
                ) { change, _ ->
                    change.consume()
                    val x = change.position.x
                    val newProgress = (x / barWidthPx).coerceIn(0f, 1f)
                    onProgressChanged(newProgress)
                }
            },
        contentAlignment = Alignment.CenterStart
    ) {
        // Base Inactive Track
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(trackHeight)
                .clip(RoundedCornerShape(3.dp))
                .background(trackColor)
                .onGloballyPositioned {
                    barWidthPx = it.size.width.toFloat()
                }
        )

        val clampedProgress = progress.coerceIn(0f, 1f)
        val filledWidthDp = with(density) {
            (clampedProgress * barWidthPx).toDp()
        }

        // Active Glowing Filled Progress Track
        Box(
            modifier = Modifier
                .width(filledWidthDp)
                .height(trackHeight)
                .clip(RoundedCornerShape(3.dp))
                .background(progressColor)
                .align(Alignment.CenterStart)
        )

        // Modern Glowing Android Auto Scrub Thumb
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = filledWidthDp - (thumbSize / 2))
                .size(thumbSize)
                .shadow(if (isDragging) 4.dp else 2.dp, CircleShape)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}
