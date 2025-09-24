package com.aashik.music.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
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
fun CustomHorizontalSeekBar(
    progress: Float,
    onProgressChanged: (Float) -> Unit,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    thumbColor: Color = MaterialTheme.colorScheme.primary,
    barHeightDp: Float = 5f // increased height for visibility
) {
    var barWidthPx by remember { mutableFloatStateOf(1f) }
    val density = LocalDensity.current

    Box(
        modifier = modifier
            .height(barHeightDp.dp)
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
        // Background bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeightDp.dp)
                .clip(RoundedCornerShape(barHeightDp.dp / 2))
                .background(barColor)
                .onGloballyPositioned {
                    barWidthPx = it.size.width.toFloat()
                }
        )

        // Filled part
        Box(
            modifier = Modifier
                .width(with(density) { (progress.coerceIn(0f, 1f) * barWidthPx).toDp() })
                .height(barHeightDp.dp)
                .clip(RoundedCornerShape(barHeightDp.dp / 2))
                .background(thumbColor)
                .align(Alignment.CenterStart)
        )
    }
}

