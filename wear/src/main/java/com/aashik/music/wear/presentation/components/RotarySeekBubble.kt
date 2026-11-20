package com.aashik.music.wear.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.Text
import com.aashik.music.wear.theme.WearPrimaryCyan
import com.aashik.music.wear.theme.WearSurfaceCard

private val BubbleShape = RoundedCornerShape(16.dp)

private fun formatMs(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}

@Composable
fun RotarySeekBubble(
    visible: Boolean,
    positionMs: Long,
    durationMs: Long,
    deltaSec: Int = 0,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        val posStr = formatMs(positionMs)
        val durStr = formatMs(durationMs)
        val deltaSign = if (deltaSec > 0) "+${deltaSec}s" else if (deltaSec < 0) "${deltaSec}s" else ""

        Box(
            modifier = Modifier
                .clip(BubbleShape)
                .background(WearSurfaceCard.copy(alpha = 0.94f))
                .padding(horizontal = 10.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (deltaSec >= 0) Icons.Rounded.FastForward else Icons.Rounded.FastRewind,
                    contentDescription = "Seek",
                    tint = WearPrimaryCyan,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (deltaSign.isNotEmpty()) "$posStr ($deltaSign)" else "$posStr / $durStr",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
