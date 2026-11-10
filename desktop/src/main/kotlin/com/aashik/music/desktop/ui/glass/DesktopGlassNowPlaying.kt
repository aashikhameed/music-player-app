package com.aashik.music.desktop.ui.glass

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aashik.music.desktop.model.DesktopSong

/**
 * Fullscreen Now Playing Takeover Overlay (1194dp × 834dp).
 * Complete takeover with grab handle, hero artwork, timeline scrubber, and lyrics/queue split stage.
 */
@Composable
fun DesktopGlassNowPlaying(
    currentSong: DesktopSong?,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    albumArt: ImageBitmap?,
    queue: List<DesktopSong>,
    lyricsLines: List<String>,
    onClose: () -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onSongSelect: (DesktopSong) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedRightTab by remember { mutableStateOf(0) } // 0: Lyrics, 1: Queue
    val progress = if (durationMs > 0) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0D0F12).copy(alpha = 0.95f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Dismiss Grab Handle (Rectangle 73: 80x4dp, r=2dp)
            Box(
                modifier = Modifier
                    .padding(top = 14.dp, bottom = 10.dp)
                    .width(80.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.30f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClose
                    )
            )

            // Main Two-Column Stage
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 48.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(48.dp)
            ) {
                // Left Column: Hero Artwork Player (412dp)
                Column(
                    modifier = Modifier
                        .width(412.dp)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Artwork Card (412x412dp)
                    Box(
                        modifier = Modifier
                            .size(380.dp)
                            .shadow(
                                elevation = 32.dp,
                                shape = RoundedCornerShape(16.dp),
                                ambientColor = Color(0xFF00E5FF).copy(alpha = 0.25f),
                                spotColor = Color(0xFF00E5FF).copy(alpha = 0.35f)
                            )
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.05f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (albumArt != null) {
                            Image(
                                bitmap = albumArt,
                                contentDescription = "Hero Artwork",
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text("🎵", fontSize = 64.sp)
                        }
                    }

                    // Metadata Row
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = currentSong?.title ?: "No Song Selected",
                            color = Color.White,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${currentSong?.artist ?: "Unknown Artist"} — ${currentSong?.album ?: "Unknown Album"}",
                            color = Color(0xFF94A3B8),
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Audio Quality Pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "LOSSLESS • ALAC 24-bit/48kHz",
                                color = Color(0xFF00E5FF),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Progress Scrubber Bar
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color.White.copy(alpha = 0.15f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(progress)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color(0xFF00E5FF))
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(formatTime(positionMs), color = Color(0xFF64748B), fontSize = 12.sp)
                            Text(formatTime(durationMs), color = Color(0xFF64748B), fontSize = 12.sp)
                        }
                    }

                    // Transport Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🔀", fontSize = 20.sp, color = Color(0xFF94A3B8))
                        Text(
                            text = "⏮",
                            fontSize = 24.sp,
                            color = Color.White,
                            modifier = Modifier.clickable(onClick = onPrevious)
                        )
                        DesktopGlassPlayButton(
                            isPlaying = isPlaying,
                            onClick = onPlayPause,
                            size = 56.dp,
                            useCyan = true
                        )
                        Text(
                            text = "⏭",
                            fontSize = 24.sp,
                            color = Color.White,
                            modifier = Modifier.clickable(onClick = onNext)
                        )
                        Text("🔁", fontSize = 20.sp, color = Color(0xFF94A3B8))
                    }
                }

                // Right Column: Lyrics / Queue Stage
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    // Stage Switcher Pill
                    DesktopGlassSegmentedPill(
                        items = listOf("❝ Lyrics", "☰ Playing Next (${queue.size})"),
                        selectedIndex = selectedRightTab,
                        onItemSelected = { selectedRightTab = it },
                        height = 36.dp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    if (selectedRightTab == 0) {
                        // Floating Lyrics Mode
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(18.dp)
                        ) {
                            if (lyricsLines.isEmpty()) {
                                item {
                                    Text(
                                        text = "Lyrics not available for this song",
                                        color = Color(0xFF64748B),
                                        fontSize = 18.sp
                                    )
                                }
                            } else {
                                items(lyricsLines.size) { idx ->
                                    val line = lyricsLines[idx]
                                    val isActive = idx == 2
                                    Text(
                                        text = line,
                                        color = if (isActive) Color.White else Color.White.copy(alpha = 0.35f),
                                        fontSize = if (isActive) 26.sp else 20.sp,
                                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    } else {
                        // Queue Mode
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(queue.size) { index ->
                                val song = queue[index]
                                val isCurrent = song.id == currentSong?.id
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isCurrent) Color(0xFF00E5FF).copy(alpha = 0.12f)
                                            else Color.White.copy(alpha = 0.04f)
                                        )
                                        .clickable { onSongSelect(song) }
                                        .padding(horizontal = 14.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Text(
                                                text = "${index + 1}",
                                                color = Color(0xFF64748B),
                                                fontSize = 13.sp,
                                                modifier = Modifier.width(24.dp)
                                            )
                                            Column {
                                                Text(
                                                    text = song.title,
                                                    color = if (isCurrent) Color(0xFF00E5FF) else Color.White,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                Text(
                                                    text = song.artist,
                                                    color = Color(0xFF94A3B8),
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }

                                        Text("☰", color = Color(0xFF64748B), fontSize = 16.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}
