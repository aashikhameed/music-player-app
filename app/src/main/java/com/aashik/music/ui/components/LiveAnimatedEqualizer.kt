package com.aashik.music.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import com.aashik.music.ui.designsystem.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val PLAYING_EQ_HEIGHTS = floatArrayOf(0.40f, 0.95f, 0.60f, 0.85f)
private val IDLE_EQ_HEIGHTS = floatArrayOf(0.35f, 0.45f, 0.30f, 0.40f)

@Composable
fun LiveAnimatedEqualizer(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary
) {
    val heights = if (isPlaying) PLAYING_EQ_HEIGHTS else IDLE_EQ_HEIGHTS

    Canvas(modifier = modifier.size(18.dp)) {
        val totalHeight = size.height
        val barWidth = 3.dp.toPx()
        val spacing = 2.dp.toPx()
        val cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())

        for (i in 0 until 4) {
            val barH = totalHeight * heights[i]
            val left = i * (barWidth + spacing)
            val top = totalHeight - barH
            drawRoundRect(
                color = barColor,
                topLeft = Offset(left, top),
                size = Size(barWidth, barH),
                cornerRadius = cornerRadius
            )
        }
    }
}

