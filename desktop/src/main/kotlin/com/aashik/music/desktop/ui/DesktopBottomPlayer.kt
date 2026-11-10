package com.aashik.music.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aashik.music.desktop.audio.DesktopAudioPlayer
import com.aashik.music.desktop.model.DesktopSong

private val ThumbnailShape = RoundedCornerShape(6.dp)

@Composable
fun DesktopBottomPlayer(
    currentSong: DesktopSong?,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    isShuffleOn: Boolean,
    onToggleShuffle: () -> Unit,
    isRepeatOn: Boolean,
    onToggleRepeat: () -> Unit,
    isQueueOpen: Boolean,
    onToggleQueue: () -> Unit,
    isLyricsOpen: Boolean,
    onToggleLyrics: () -> Unit,
    onOpenNowPlaying: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPlaying by DesktopAudioPlayer.isPlaying.collectAsState()
    val positionMs by DesktopAudioPlayer.currentPositionMs.collectAsState()
    val durationMs by DesktopAudioPlayer.durationMs.collectAsState()
    val volume by DesktopAudioPlayer.volume.collectAsState()

    var isDraggingSeek by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableStateOf(0f) }

    val progress = if (isDraggingSeek) {
        dragProgress
    } else {
        if (durationMs > 0) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(78.dp)
            .background(DarkSidebar)
            .padding(horizontal = 20.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // ─── LEFT: Current Track Info & Favorite ──────────────────────────
        Row(
            modifier = Modifier
                .width(280.dp)
                .clickable { onOpenNowPlaying() },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DesktopArtwork(
                url = currentSong?.artworkUrl,
                size = 48.dp,
                shape = ThumbnailShape
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = currentSong?.title ?: "Not Playing",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DarkTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = currentSong?.artist ?: "Unknown Artist",
                    fontSize = 11.5.sp,
                    color = DarkTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (isFavorite) DarkFavorite else DarkTextTertiary,
                    modifier = Modifier.size(17.dp)
                )
            }
        }

        // ─── CENTER: Controls & Scrubber ──────────────────────────────────
        Column(
            modifier = Modifier.weight(1f).padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Transport buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Shuffle
                IconButton(onClick = onToggleShuffle, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (isShuffleOn) DarkAccent else DarkTextTertiary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Previous
                IconButton(onClick = onPrevious, modifier = Modifier.size(34.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.SkipPrevious,
                        contentDescription = "Previous",
                        tint = DarkTextPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Play / Pause Circle
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(DarkAccent)
                        .clickable { DesktopAudioPlayer.togglePlayPause() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color(0xFF0C0E14),
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Next
                IconButton(onClick = onNext, modifier = Modifier.size(34.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = "Next",
                        tint = DarkTextPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Repeat
                IconButton(onClick = onToggleRepeat, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.Repeat,
                        contentDescription = "Repeat",
                        tint = if (isRepeatOn) DarkAccent else DarkTextTertiary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            if (durationMs > 0L) {
                // Scrubbing Slider
                Row(
                    modifier = Modifier.fillMaxWidth().height(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = formatMs(if (isDraggingSeek) (dragProgress * durationMs).toLong() else positionMs),
                        fontSize = 11.sp,
                        color = DarkTextTertiary,
                        modifier = Modifier.width(32.dp)
                    )

                    Slider(
                        value = progress,
                        onValueChange = {
                            isDraggingSeek = true
                            dragProgress = it
                        },
                        onValueChangeFinished = {
                            DesktopAudioPlayer.seekTo((dragProgress * durationMs).toLong())
                            isDraggingSeek = false
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = DarkAccent,
                            inactiveTrackColor = DarkSurfaceVariant
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = formatMs(durationMs),
                        fontSize = 11.sp,
                        color = DarkTextTertiary,
                        modifier = Modifier.width(32.dp)
                    )
                }
            } else if (currentSong != null) {
                // Live Stream Indicator
                Row(
                    modifier = Modifier.fillMaxWidth().height(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(DarkPlayingGreen.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(DarkPlayingGreen))
                            Text("LIVE BROADCAST", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = DarkPlayingGreen)
                        }
                    }
                }
            }
        }

        // ─── RIGHT: Utility & Output (Lyrics, Queue, Volume) ──────────────
        Row(
            modifier = Modifier.width(280.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)
        ) {
            // Lyrics Toggle
            IconButton(
                onClick = onToggleLyrics,
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.FormatQuote,
                    contentDescription = "Lyrics",
                    tint = if (isLyricsOpen) DarkAccent else DarkTextTertiary,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Up Next Queue Toggle
            IconButton(
                onClick = onToggleQueue,
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.QueueMusic,
                    contentDescription = "Queue",
                    tint = if (isQueueOpen) DarkAccent else DarkTextTertiary,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Volume Slider
            Row(
                modifier = Modifier.width(110.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = if (volume > 0.05f) Icons.Rounded.VolumeUp else Icons.Rounded.VolumeOff,
                    contentDescription = "Volume",
                    tint = DarkTextTertiary,
                    modifier = Modifier
                        .size(16.dp)
                        .clickable { DesktopAudioPlayer.setVolume(if (volume > 0.05f) 0f else 0.85f) }
                )

                Slider(
                    value = volume,
                    onValueChange = { DesktopAudioPlayer.setVolume(it) },
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = DarkTextSecondary,
                        inactiveTrackColor = DarkSurfaceVariant
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

private fun formatMs(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val m = totalSec / 60
    val s = totalSec % 60
    return String.format("%d:%02d", m, s)
}
