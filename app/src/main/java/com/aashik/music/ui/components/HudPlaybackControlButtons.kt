package com.aashik.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aashik.music.viewmodel.MusicViewModel

/**
 * Standard Material 3 Playback Controls (Balanced 5-Button Cluster).
 * Shuffle has been moved next to Lyrics in MobilePlayerActionBar.
 */
@Composable
fun HudPlaybackControlButtons(
    viewModel: MusicViewModel,
    isPlaying: Boolean,
    @Suppress("UNUSED_PARAMETER") isShuffleOn: Boolean = false,
    @Suppress("UNUSED_PARAMETER") isStream: Boolean = false,
    controlIconColor: Color = MaterialTheme.colorScheme.onSurface,
    controlInactiveColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Previous Track
        IconButton(
            onClick = { viewModel.playPreviousSong() },
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.SkipPrevious,
                contentDescription = "Previous Song",
                tint = controlIconColor,
                modifier = Modifier.size(34.dp)
            )
        }

        // Quick Skip -10s
        IconButton(
            onClick = { viewModel.skipBackward10s() },
            modifier = Modifier.size(44.dp)
        ) {
            Text(
                text = "-10s",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = controlInactiveColor
            )
        }

        // Hero Play/Pause Button (Material 3 Primary Filled Circle)
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = CircleShape
                )
                .clip(CircleShape)
                .clickable { viewModel.togglePlayPause() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(38.dp)
            )
        }

        // Quick Skip +10s
        IconButton(
            onClick = { viewModel.skipForward10s() },
            modifier = Modifier.size(44.dp)
        ) {
            Text(
                text = "+10s",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = controlInactiveColor
            )
        }

        // Next Track
        IconButton(
            onClick = { viewModel.playNextSong() },
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.SkipNext,
                contentDescription = "Next Song",
                tint = controlIconColor,
                modifier = Modifier.size(34.dp)
            )
        }
    }
}
