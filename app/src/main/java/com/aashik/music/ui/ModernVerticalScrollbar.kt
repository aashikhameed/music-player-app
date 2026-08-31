package com.aashik.music.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

@Composable
fun ModernVerticalScrollbar(
    gridState: LazyGridState,
    totalItemCount: Int,
    modifier: Modifier = Modifier,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
    thumbColor: Color = MaterialTheme.colorScheme.primary
) {
    val coroutineScope = rememberCoroutineScope()
    var trackHeightPx by remember { mutableFloatStateOf(1f) }
    var isDragging by remember { mutableStateOf(false) }

    val density = LocalDensity.current

    val scrollFraction by remember {
        derivedStateOf {
            if (totalItemCount <= 0) return@derivedStateOf 0f
            val firstVisibleIndex = gridState.firstVisibleItemIndex.toFloat()
            val firstVisibleOffset = gridState.firstVisibleItemScrollOffset.toFloat()
            val approximateTotalItems = totalItemCount.toFloat()

            // Normalized fraction between 0 and 1
            ((firstVisibleIndex + (firstVisibleOffset / 200f).coerceIn(0f, 1f)) / approximateTotalItems).coerceIn(0f, 1f)
        }
    }

    val animatedFraction by animateFloatAsState(
        targetValue = scrollFraction,
        label = "ScrollbarThumbAnimation"
    )

    // Sleek 6dp modern automotive scrollbar track
    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(28.dp) // Generous touch target for driver dragging
            .onGloballyPositioned {
                trackHeightPx = it.size.height.toFloat()
            }
            .pointerInput(totalItemCount) {
                detectVerticalDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = { isDragging = false },
                    onDragCancel = { isDragging = false }
                ) { change, _ ->
                    change.consume()
                    val y = change.position.y
                    val fraction = (y / trackHeightPx).coerceIn(0f, 1f)
                    val targetItem = (fraction * totalItemCount).toInt().coerceIn(0, max(0, totalItemCount - 1))
                    coroutineScope.launch {
                        gridState.scrollToItem(targetItem)
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Slim Track background
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .padding(vertical = 12.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(trackColor)
        )

        // Floating Pill Thumb
        val thumbHeightDp = 48.dp
        val thumbHeightPx = with(density) { thumbHeightDp.toPx() }
        val maxTravelPx = max(0f, trackHeightPx - thumbHeightPx - with(density) { 24.dp.toPx() })
        val thumbOffsetDp = with(density) {
            (12.dp.toPx() + (if (isDragging) scrollFraction else animatedFraction) * maxTravelPx).toDp()
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = thumbOffsetDp)
                .width(if (isDragging) 8.dp else 5.dp)
                .height(thumbHeightDp)
                .clip(RoundedCornerShape(4.dp))
                .background(if (isDragging) MaterialTheme.colorScheme.secondary else thumbColor)
        )
    }
}
