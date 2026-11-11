package com.aashik.music.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aashik.music.desktop.audio.DesktopAudioPlayer
import com.aashik.music.desktop.model.DesktopSong

private val PlayerCapsuleShape = RoundedCornerShape(32.dp)
private val MiniArtShape = RoundedCornerShape(8.dp)

/**
 * Floating Liquid Glass Capsule Mini-Player Dock.
 * Matches pixel-perfect with designScreen/tablet/Album.png and Home.png:
 * - Floating translucent acrylic dock with specular highlight reflection.
 * - Left: Transport buttons (Shuffle, Previous, Play/Pause, Next, Repeat).
 * - Center: Song Thumbnail, Title, Artist, Lossless/Live status, and Scrubber.
 * - Right: Context tools (Options ..., Lyrics toggle, Queue toggle, Volume slider).
 */
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

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = PlayerCapsuleShape,
            color = GlassDock,
            border = GlassBorderSpecular,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .shadow(
                    elevation = 24.dp,
                    shape = PlayerCapsuleShape,
                    ambientColor = Color.Black.copy(alpha = 0.6f),
                    spotColor = Color.Black.copy(alpha = 0.7f)
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // ─── LEFT: TRANSPORT CONTROLS (Shuffle, Prev, Play/Pause, Next, Repeat) ──
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Shuffle Button
                    IconButton(
                        onClick = onToggleShuffle,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (isShuffleOn) DarkAccent else DarkTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Previous Button
                    IconButton(
                        onClick = onPrevious,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SkipPrevious,
                            contentDescription = "Previous",
                            tint = DarkTextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Play / Pause Circle (Solid white circle with dark icon)
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .clickable { DesktopAudioPlayer.togglePlayPause() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = Color(0xFF090B10),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Next Button
                    IconButton(
                        onClick = onNext,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SkipNext,
                            contentDescription = "Next",
                            tint = DarkTextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Repeat Button
                    IconButton(
                        onClick = onToggleRepeat,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Repeat,
                            contentDescription = "Repeat",
                            tint = if (isRepeatOn) DarkAccent else DarkTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // ─── CENTER: THUMBNAIL + TITLE + ARTIST (Click opens full stage) ──────
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onOpenNowPlaying() }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DesktopArtwork(
                        url = currentSong?.artworkUrl,
                        size = 40.dp,
                        shape = MiniArtShape
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = currentSong?.title ?: "Not Playing",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (durationMs <= 0L && currentSong != null) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(DarkPlayingGreen.copy(alpha = 0.2f))
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text("LIVE", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = DarkPlayingGreen)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(1.dp))

                        Text(
                            text = currentSong?.artist ?: "Select a track to play",
                            fontSize = 11.sp,
                            color = DarkTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        // Hairline progress bar under metadata
                        if (durationMs > 0L) {
                            Spacer(modifier = Modifier.height(3.dp))
                            LinearProgressIndicator(
                                progress = progress,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(2.dp)
                                    .clip(RoundedCornerShape(1.dp)),
                                color = DarkAccent,
                                trackColor = DarkSurfaceVariant
                            )
                        }
                    }

                    // Favorite Button (Ice Cyan star/heart, NEVER red)
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (isFavorite) DarkFavorite else DarkTextTertiary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // ─── RIGHT: CONTEXT TOOLS (Options, Lyrics, Queue, Volume) ───────────
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Options ... button
                    IconButton(
                        onClick = { onOpenNowPlaying() },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MoreHoriz,
                            contentDescription = "Options",
                            tint = DarkTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Lyrics Toggle Pill Button (💬 speech bubble with quotes)
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (isLyricsOpen) DarkAccent.copy(alpha = 0.20f) else Color.Transparent)
                            .border(
                                width = 1.dp,
                                color = if (isLyricsOpen) DarkAccent.copy(alpha = 0.6f) else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { onToggleLyrics() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FormatQuote,
                            contentDescription = "Lyrics",
                            tint = if (isLyricsOpen) DarkAccent else DarkTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Up Next Queue Toggle Pill Button (📋 bullet list)
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (isQueueOpen) DarkAccent.copy(alpha = 0.20f) else Color.Transparent)
                            .border(
                                width = 1.dp,
                                color = if (isQueueOpen) DarkAccent.copy(alpha = 0.6f) else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { onToggleQueue() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.QueueMusic,
                            contentDescription = "Queue",
                            tint = if (isQueueOpen) DarkAccent else DarkTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Volume slider with icon
                    Row(
                        modifier = Modifier.width(110.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (volume > 0.05f) Icons.Rounded.VolumeUp else Icons.Rounded.VolumeOff,
                            contentDescription = "Volume",
                            tint = DarkTextSecondary,
                            modifier = Modifier
                                .size(16.dp)
                                .clickable { DesktopAudioPlayer.setVolume(if (volume > 0.05f) 0f else 0.85f) }
                        )

                        Slider(
                            value = volume,
                            onValueChange = { DesktopAudioPlayer.setVolume(it) },
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = DarkTextPrimary,
                                inactiveTrackColor = DarkSurfaceVariant
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}
