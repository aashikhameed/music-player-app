package com.aashik.music.wear.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.aashik.music.wear.theme.WearPrimaryCyan
import com.aashik.music.wear.theme.WearSurfaceCard

private val CapsuleShape = RoundedCornerShape(2.dp)

/**
 * Draws a horizontal linear capsule progress bar tailored for Square Wear OS screens.
 * @param progress Value between 0.0f and 1.0f.
 */
@Composable
fun LinearProgressCapsule(
    progress: Float,
    modifier: Modifier = Modifier
) {
    val clampedProgress = progress.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(CapsuleShape)
            .background(WearSurfaceCard)
    ) {
        if (clampedProgress > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(clampedProgress)
                    .fillMaxHeight()
                    .clip(CapsuleShape)
                    .background(WearPrimaryCyan)
            )
        }
    }
}
