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

private val ThumbnailShape = RoundedCornerShape(8.dp)
private val FavoriteRed = Color(0xFFFF4081)

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
            .height(82.dp)
            .background(Color(0xFF121620))
            .padding(horizontal = 16.dp, vertical = 8.dp),
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
                size = 52.dp,
                shape = ThumbnailShape
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = currentSong?.title ?: "No Song Playing",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFF1F5F9),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = currentSong?.artist ?: "Unknown Artist",
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (isFavorite) FavoriteRed else Color(0xFF64748B),
                    modifier = Modifier.size(18.dp)
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
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Shuffle
                IconButton(onClick = onToggleShuffle, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (isShuffleOn) Color(0xFF00E5FF) else Color(0xFF64748B),
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Previous
                IconButton(onClick = onPrevious, modifier = Modifier.size(34.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.SkipPrevious,
                        contentDescription = "Previous",
                        tint = Color(0xFFCBD5E1),
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Play / Pause Circle
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFF00E5FF), shape = CircleShape)
                        .clip(CircleShape)
                        .clickable { DesktopAudioPlayer.togglePlayPause() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color.Black,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Next
                IconButton(onClick = onNext, modifier = Modifier.size(34.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = "Next",
                        tint = Color(0xFFCBD5E1),
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Repeat
                IconButton(onClick = onToggleRepeat, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.Repeat,
                        contentDescription = "Repeat",
                        tint = if (isRepeatOn) Color(0xFF00E5FF) else Color(0xFF64748B),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Scrubbing Seekbar
            Row(
                modifier = Modifier.fillMaxWidth().height(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = formatMs(if (isDraggingSeek) (dragProgress * durationMs).toLong() else positionMs),
                    fontSize = 11.sp,
                    color = Color(0xFF64748B),
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
                        thumbColor = Color(0xFF00E5FF),
                        activeTrackColor = Color(0xFF00E5FF),
                        inactiveTrackColor = Color(0xFF252D3D)
                    ),
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = formatMs(durationMs),
                    fontSize = 11.sp,
                    color = Color(0xFF64748B),
                    modifier = Modifier.width(32.dp)
                )
            }
        }

        // ─── RIGHT: Utility & Output (Lyrics, Queue, Volume, Fullscreen) ──
        Row(
            modifier = Modifier.width(280.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)
        ) {
            // Lyrics Toggle
            IconButton(onClick = onToggleLyrics, modifier = Modifier.size(34.dp)) {
                Icon(
                    imageVector = Icons.Rounded.Subtitles,
                    contentDescription = "Lyrics",
                    tint = if (isLyricsOpen) Color(0xFF00E5FF) else Color(0xFF94A3B8),
                    modifier = Modifier.size(18.dp)
                )
            }

            // Queue Toggle
            IconButton(onClick = onToggleQueue, modifier = Modifier.size(34.dp)) {
                Icon(
                    imageVector = Icons.Rounded.QueueMusic,
                    contentDescription = "Queue",
                    tint = if (isQueueOpen) Color(0xFF00E5FF) else Color(0xFF94A3B8),
                    modifier = Modifier.size(18.dp)
                )
            }

            // Volume Mute/Icon + Slider
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = if (volume > 0.05f) Icons.Rounded.VolumeUp else Icons.Rounded.VolumeOff,
                    contentDescription = "Volume",
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier
                        .size(18.dp)
                        .clickable { DesktopAudioPlayer.setVolume(if (volume > 0.05f) 0f else 0.85f) }
                )
                Slider(
                    value = volume,
                    onValueChange = { DesktopAudioPlayer.setVolume(it) },
                    modifier = Modifier.width(76.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF00E5FF),
                        activeTrackColor = Color(0xFF00E5FF),
                        inactiveTrackColor = Color(0xFF252D3D)
                    )
                )
            }

            // Fullscreen HUD Shortcut
            IconButton(onClick = onOpenNowPlaying, modifier = Modifier.size(34.dp)) {
                Icon(
                    imageVector = Icons.Rounded.Fullscreen,
                    contentDescription = "Now Playing HUD",
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(20.dp)
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
