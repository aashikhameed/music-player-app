package com.aashik.music.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
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
    thumbColor: Color = MaterialTheme.colorScheme.primary,
    thumbSizeDp: Float = 12f
) {
    var barHeightPx by remember { mutableFloatStateOf(1f) }
    val density = LocalDensity.current

    Box(
        modifier = modifier
            .width(4.dp)
            .fillMaxHeight()
            .pointerInput(Unit) {
                detectVerticalDragGestures { change, _ ->
                    change.consume() // consume so we keep getting events
                    val y = change.position.y
                    val newProgress = y / barHeightPx
                    onProgressChanged(min(1f, max(0f, newProgress)))
                }
            },
        contentAlignment = Alignment.TopCenter
    ) {
        // Background bar
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(4.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(barColor)
                .onGloballyPositioned {
                    barHeightPx = it.size.height.toFloat()
                }
        )

        // Filled height in dp
        val filledHeightDp = with(density) {
            (progress.coerceIn(0f, 1f) * barHeightPx).toDp()
        }

        // Filled part
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(filledHeightDp)
                .clip(RoundedCornerShape(4.dp))
                .background(thumbColor)
                .align(Alignment.TopCenter)
        )

        // Thumb circle
//        Box(
//            modifier = Modifier
//                .width(thumbSizeDp.dp)
//                .height(thumbSizeDp.dp)
//                .clip(CircleShape)
//                .background(thumbColor)
//                .align(Alignment.TopCenter)
//                .offset(y = filledHeightDp - (thumbSizeDp / 2).dp)
//        )
    }
}

