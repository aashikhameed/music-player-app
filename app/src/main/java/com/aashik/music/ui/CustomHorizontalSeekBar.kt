package com.aashik.music.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    barColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    thumbColor: Color = MaterialTheme.colorScheme.primary,
    trackHeightDp: Float = 8f
) {
    var barWidthPx by remember { mutableFloatStateOf(1f) }
    val density = LocalDensity.current

    // Large 36dp touch height to allow effortless dragging while driving
    Box(
        modifier = modifier
            .height(36.dp)
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, _ ->
                    change.consume()
                    val x = change.position.x
                    val newProgress = x / barWidthPx
                    onProgressChanged(min(1f, max(0f, newProgress)))
                }
            },
        contentAlignment = Alignment.CenterStart
    ) {
        // Visual Track
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(trackHeightDp.dp)
                .clip(RoundedCornerShape(trackHeightDp.dp / 2))
                .background(barColor)
                .onGloballyPositioned {
                    barWidthPx = it.size.width.toFloat()
                }
        )

        val filledWidthDp = with(density) {
            (progress.coerceIn(0f, 1f) * barWidthPx).toDp()
        }

        // Filled active track
        Box(
            modifier = Modifier
                .width(filledWidthDp)
                .height(trackHeightDp.dp)
                .clip(RoundedCornerShape(trackHeightDp.dp / 2))
                .background(thumbColor)
                .align(Alignment.CenterStart)
        )

        // Floating tactile circular thumb
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .width(filledWidthDp)
        ) {
            Box(
                modifier = Modifier
                    .width(18.dp)
                    .height(18.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .align(Alignment.CenterEnd)
            )
        }
    }
}
