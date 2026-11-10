package com.aashik.music.desktop.ui.components

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aashik.music.desktop.audio.DesktopAudioPlayer

@Composable
fun DesktopPlaybackControls(
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    primaryColor: Color = Color(0xFF00E5FF),
    iconColor: Color = Color(0xFFF1F5F9),
    inactiveColor: Color = Color(0xFF94A3B8)
) {
    val isPlaying by DesktopAudioPlayer.isPlaying.collectAsState()
    val positionMs by DesktopAudioPlayer.currentPositionMs.collectAsState()
    val durationMs by DesktopAudioPlayer.durationMs.collectAsState()

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Previous Track
        IconButton(
            onClick = onPrevious,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.SkipPrevious,
                contentDescription = "Previous Song",
                tint = iconColor,
                modifier = Modifier.size(34.dp)
            )
        }

        // Quick Skip -10s
        IconButton(
            onClick = {
                val newPos = (positionMs - 10_000L).coerceAtLeast(0L)
                DesktopAudioPlayer.seekTo(newPos)
            },
            modifier = Modifier.size(44.dp)
        ) {
            Text(
                text = "-10s",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = inactiveColor
            )
        }

        // Hero Play/Pause Button (64dp filled Circle in Primary Color)
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(color = primaryColor, shape = CircleShape)
                .clip(CircleShape)
                .clickable { DesktopAudioPlayer.togglePlayPause() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = Color.Black,
                modifier = Modifier.size(38.dp)
            )
        }

        // Quick Skip +10s
        IconButton(
            onClick = {
                val newPos = (positionMs + 10_000L).coerceAtMost(durationMs)
                DesktopAudioPlayer.seekTo(newPos)
            },
            modifier = Modifier.size(44.dp)
        ) {
            Text(
                text = "+10s",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = inactiveColor
            )
        }

        // Next Track
        IconButton(
            onClick = onNext,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.SkipNext,
                contentDescription = "Next Song",
                tint = iconColor,
                modifier = Modifier.size(34.dp)
            )
        }
    }
}

