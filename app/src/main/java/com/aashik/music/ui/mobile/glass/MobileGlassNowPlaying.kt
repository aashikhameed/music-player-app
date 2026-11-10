package com.aashik.music.ui.mobile.glass

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.aashik.music.model.Song

/**
 * Mobile Fullscreen Now Playing Takeover Overlay (393pt × 852pt).
 * Includes top grab handle, audio route pill, hero artwork, lossless badge,
 * progress scrubber, 64pt cyan play button, and bottom utility strip.
 */
@Composable
fun MobileGlassNowPlaying(
    currentSong: Song?,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    albumArt: ImageBitmap?,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onClose: () -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleLyrics: () -> Unit,
    onToggleQueue: () -> Unit,
    onOpenAudioRoute: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = if (durationMs > 0) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0D0F12).copy(alpha = 0.96f))
            .systemBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // ─── TOP SECTION: Grab Handle & Audio Route Pill ─────────────────────
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 10.dp)
            ) {
                // Top Dismiss Grab Handle (Rectangle 73: 44x5dp, r=2.5dp)
                Box(
                    modifier = Modifier
                        .width(44.dp)
                        .height(5.dp)
                        .clip(RoundedCornerShape(2.5.dp))
                        .background(Color.White.copy(alpha = 0.35f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onClose
                        )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Centered Audio Route Pill (iPhone > AirPods Max)
                Box(
                    modifier = Modifier
                        .height(32.dp)
                        .mobileLiquidGlass(
                            shape = RoundedCornerShape(16.dp),
                            tintColor = Color.White.copy(alpha = 0.08f),
                            borderColor = Color.White.copy(alpha = 0.12f)
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onOpenAudioRoute
                        )
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Headphones,
                            contentDescription = "Audio Route",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "iPhone > AirPods Max",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // ─── CENTER SECTION: Hero Artwork Card (335x335dp) ───────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .padding(horizontal = 12.dp)
                    .shadow(
                        elevation = 28.dp,
                        shape = RoundedCornerShape(20.dp),
                        ambientColor = Color(0xFF00E5FF).copy(alpha = 0.25f),
                        spotColor = Color(0xFF00E5FF).copy(alpha = 0.35f)
                    )
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.06f)),
                contentAlignment = Alignment.Center
            ) {
                if (albumArt != null) {
                    Image(
                        bitmap = albumArt,
                        contentDescription = "Hero Artwork",
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text("🎵", fontSize = 56.sp)
                }
            }

            // ─── METADATA ROW WITH LOSSLESS BADGE ────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentSong?.title ?: "No Song Playing",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = currentSong?.artist ?: "Unknown Artist",
                        color = Color(0xFF94A3B8),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    MobileGlassBadgePill(
                        text = "LOSSLESS • ALAC 24-bit/48kHz",
                        useCyan = true
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Favorite Heart Button
                    MobileGlassIconButton(
                        icon = {
                            Icon(
                                imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (isFavorite) Color(0xFF00E5FF) else Color(0xFF94A3B8),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        onClick = onToggleFavorite,
                        size = 36.dp,
                        isActive = isFavorite
                    )

                    // Options Menu Button
                    MobileGlassIconButton(
                        icon = {
                            Icon(
                                imageVector = Icons.Rounded.MoreHoriz,
                                contentDescription = "Options",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        onClick = {},
                        size = 36.dp
                    )
                }
            }

            // ─── SCRUBBER TIMELINE ───────────────────────────────────────────────
            Column(modifier = Modifier.fillMaxWidth()) {
                Slider(
                    value = progress,
                    onValueChange = { frac -> onSeek((frac * durationMs).toLong()) },
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color(0xFF00E5FF),
                        inactiveTrackColor = Color.White.copy(alpha = 0.18f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(formatTimeMs(positionMs), color = Color(0xFF64748B), fontSize = 12.sp)
                    val remainingMs = (durationMs - positionMs).coerceAtLeast(0)
                    Text("-${formatTimeMs(remainingMs)}", color = Color(0xFF64748B), fontSize = 12.sp)
                }
            }

            // ─── TRANSPORT CONTROLS ROW ──────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Shuffle
                Icon(
                    imageVector = Icons.Rounded.Shuffle,
                    contentDescription = "Shuffle",
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(22.dp)
                )

                // Previous
                Icon(
                    imageVector = Icons.Rounded.SkipPrevious,
                    contentDescription = "Previous",
                    tint = Color.White,
                    modifier = Modifier
                        .size(32.dp)
                        .clickable(onClick = onPrevious)
                )

                // Elevated 64pt Cyan Play Button
                MobileGlassPlayButton(
                    isPlaying = isPlaying,
                    onClick = onPlayPause,
                    size = 64.dp,
                    useCyan = true
                )

                // Next
                Icon(
                    imageVector = Icons.Rounded.SkipNext,
                    contentDescription = "Next",
                    tint = Color.White,
                    modifier = Modifier
                        .size(32.dp)
                        .clickable(onClick = onNext)
                )

                // Repeat
                Icon(
                    imageVector = Icons.Rounded.Repeat,
                    contentDescription = "Repeat",
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(22.dp)
                )
            }

            // ─── LOWER UTILITY STRIP: Lyrics, Route, Queue ───────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Lyrics Trigger
                MobileGlassIconButton(
                    icon = {
                        Icon(
                            imageVector = Icons.Rounded.ChatBubbleOutline,
                            contentDescription = "Lyrics",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    onClick = onToggleLyrics,
                    size = 38.dp
                )

                // Audio Route Center
                Box(
                    modifier = Modifier
                        .height(34.dp)
                        .mobileLiquidGlass(
                            shape = RoundedCornerShape(17.dp),
                            tintColor = Color.White.copy(alpha = 0.08f),
                            borderColor = Color.White.copy(alpha = 0.12f)
                        )
                        .clickable(onClick = onOpenAudioRoute)
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Airplay,
                            contentDescription = "AirPlay",
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "AirPods Max",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Up Next Queue Trigger
                MobileGlassIconButton(
                    icon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                            contentDescription = "Queue",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    onClick = onToggleQueue,
                    size = 38.dp
                )
            }
        }
    }
}

private fun formatTimeMs(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}
