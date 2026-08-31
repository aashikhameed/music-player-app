package com.aashik.music.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
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
fun CustomVerticalSeekBar(
    progress: Float,
    onProgressChanged: (Float) -> Unit,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    thumbColor: Color = MaterialTheme.colorScheme.primary
) {
    var barHeightPx by remember { mutableFloatStateOf(1f) }
    val density = LocalDensity.current

    // Seekbar container with 16dp width aligned flush to the right edge
    Box(
        modifier = modifier
            .width(16.dp)
            .fillMaxHeight()
            .pointerInput(Unit) {
                detectVerticalDragGestures { change, _ ->
                    change.consume()
                    val y = change.position.y
                    val newProgress = 1f - (y / barHeightPx)
                    onProgressChanged(min(1f, max(0f, newProgress)))
                }
            },
        contentAlignment = Alignment.CenterEnd
    ) {
        // Track
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(6.dp)
                .background(barColor)
                .onGloballyPositioned {
                    barHeightPx = it.size.height.toFloat()
                }
        )

        // Filled active track
        val filledHeightDp = with(density) {
            (progress.coerceIn(0f, 1f) * barHeightPx).toDp()
        }

        Box(
            modifier = Modifier
                .width(6.dp)
                .height(filledHeightDp)
                .background(thumbColor)
                .align(Alignment.BottomEnd)
        )

        // Floating tactile circular thumb
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .height(filledHeightDp)
        ) {
            Box(
                modifier = Modifier
                    .width(14.dp)
                    .height(14.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .align(Alignment.TopCenter)
            )
        }
    }
}
