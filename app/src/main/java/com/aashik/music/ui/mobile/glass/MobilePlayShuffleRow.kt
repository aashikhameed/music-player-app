package com.aashik.music.ui.mobile.glass

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aashik.music.ui.designsystem.Icon
import com.aashik.music.ui.designsystem.Text

/**
 * Play + Shuffle capsules shared by the album, artist and playlist headers.
 * Both are liquid-glass buttons (Play tinted with the accent colour).
 */
@Composable
fun MobilePlayShuffleRow(
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    modifier: Modifier = Modifier,
    buttonHeight: Dp = 44.dp
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        LiquidPrimaryButton(
            onClick = onPlay,
            modifier = Modifier.weight(1f).height(buttonHeight)
        ) {
            Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = MobileTheme.text, modifier = Modifier.size(20.dp))
            Text("Play", color = MobileTheme.text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
        LiquidButton(
            onClick = onShuffle,
            modifier = Modifier.weight(1f).height(buttonHeight)
        ) {
            Icon(Icons.Rounded.Shuffle, contentDescription = null, tint = MobileTheme.text, modifier = Modifier.size(18.dp))
            Text("Shuffle", color = MobileTheme.text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}
