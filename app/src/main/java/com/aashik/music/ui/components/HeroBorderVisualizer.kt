package com.aashik.music.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Clean, subtle perimeter border for the album artwork card (matching Apple Music & Spotify).
 * Free from synthetic bouncing animations, providing a crisp, distraction-free visual presentation.
 */
@Composable
fun HeroBorderVisualizer(
    @Suppress("UNUSED_PARAMETER") isPlaying: Boolean = false,
    borderColor: Color,
    @Suppress("UNUSED_PARAMETER") visualizerColor: Color = Color.Unspecified,
    cornerRadius: Dp = 20.dp,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val cornerRadiusPx = cornerRadius.toPx()
        
        drawRoundRect(
            color = borderColor,
            size = size,
            cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)
        )
    }
}
