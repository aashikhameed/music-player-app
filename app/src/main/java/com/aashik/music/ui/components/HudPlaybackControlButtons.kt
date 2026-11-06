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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aashik.music.viewmodel.MusicViewModel

@Composable
fun HudPlaybackControlButtons(
    viewModel: MusicViewModel,
    isPlaying: Boolean,
    isShuffleOn: Boolean,
    isStream: Boolean,
    controlIconColor: Color,
    controlInactiveColor: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Shuffle Mode Toggle (only for local playback - hidden when streaming)
        if (!isStream) {
            IconButton(
                onClick = { viewModel.toggleShuffle() },
                modifier = Modifier.size(44.dp)
            ) {
                Text(
                    text = if (isShuffleOn) "SHUF" else "SEQ",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isShuffleOn) MaterialTheme.colorScheme.primary else controlInactiveColor
                )
            }
        }

        // Previous Track
        IconButton(
            onClick = { viewModel.playPreviousSong() },
            modifier = Modifier.size(52.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.SkipPrevious,
                contentDescription = "Previous Song",
                tint = controlIconColor,
                modifier = Modifier.size(36.dp)
            )
        }

        // Quick Skip -10s
        IconButton(
            onClick = { viewModel.skipBackward10s() },
            modifier = Modifier.size(44.dp)
        ) {
            Text(
                text = "-10s",
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                color = controlIconColor
            )
        }

        // Hero Play/Pause Button
        Box(
            modifier = Modifier
                .size(60.dp)
                .background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = CircleShape
                )
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
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                color = controlIconColor
            )
        }

        // Next Track
        IconButton(
            onClick = { viewModel.playNextSong() },
            modifier = Modifier.size(52.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.SkipNext,
                contentDescription = "Next Song",
                tint = controlIconColor,
                modifier = Modifier.size(36.dp)
            )
        }
    }
}

