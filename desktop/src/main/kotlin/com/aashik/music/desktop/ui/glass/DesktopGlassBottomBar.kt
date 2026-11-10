package com.aashik.music.desktop.ui.glass

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.aashik.music.desktop.model.DesktopSong

/**
 * Floating Mini-Player Capsule Dock (700dp × 66dp) with liquid glass effect.
 * Exactly matches "Now Playing Bar iPad" (node 700x66dp).
 */
@Composable
fun DesktopGlassBottomBar(
    currentSong: DesktopSong?,
    isPlaying: Boolean,
    isShuffle: Boolean,
    isRepeat: Boolean,
    isLyricsOpen: Boolean,
    isQueueOpen: Boolean,
    volume: Float,
    albumArt: ImageBitmap?,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleLyrics: () -> Unit,
    onToggleQueue: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onOpenNowPlaying: () -> Unit,
    modifier: Modifier = Modifier
) {
    val capsuleShape = RoundedCornerShape(33.dp)

    Box(
        modifier = modifier
            .width(700.dp)
            .height(66.dp)
            .shadow(
                elevation = 20.dp,
                shape = capsuleShape,
                ambientColor = Color.Black.copy(alpha = 0.50f),
                spotColor = Color.Black.copy(alpha = 0.60f)
            )
            .liquidGlassEffect(
                shape = capsuleShape,
                tintColor = Color(0xFF14161A).copy(alpha = 0.85f),
                borderColor = Color.White.copy(alpha = 0.15f)
            )
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left Controls Cluster (202dp)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Shuffle
                Text(
                    text = "🔀",
                    color = if (isShuffle) Color(0xFF00E5FF) else Color(0xFF94A3B8),
                    fontSize = 15.sp,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onToggleShuffle
                    )
                )

                // Prev
                Text(
                    text = "⏮",
                    color = Color.White,
                    fontSize = 16.sp,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onPrevious
                    )
                )

                // Play / Pause Circle
                DesktopGlassPlayButton(
                    isPlaying = isPlaying,
                    onClick = onPlayPause,
                    size = 38.dp,
                    useCyan = true
                )

                // Next
                Text(
                    text = "⏭",
                    color = Color.White,
                    fontSize = 16.sp,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onNext
                    )
                )

                // Repeat
                Text(
                    text = "🔁",
                    color = if (isRepeat) Color(0xFF00E5FF) else Color(0xFF94A3B8),
                    fontSize = 15.sp,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onToggleRepeat
                    )
                )
            }

            // Center Info Cluster (280dp)
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onOpenNowPlaying
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Album Art
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.White.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (albumArt != null) {
                        Image(
                            bitmap = albumArt,
                            contentDescription = "Album Art",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text("🎵", fontSize = 18.sp)
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = currentSong?.title ?: "Not Playing",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = currentSong?.artist ?: "Select a song",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Right Cluster: Lyrics, Queue, Volume
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Lyrics
                Text(
                    text = "💬",
                    color = if (isLyricsOpen) Color(0xFF00E5FF) else Color(0xFF94A3B8),
                    fontSize = 16.sp,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onToggleLyrics
                    )
                )

                // Queue
                Text(
                    text = "☰",
                    color = if (isQueueOpen) Color(0xFF00E5FF) else Color(0xFF94A3B8),
                    fontSize = 17.sp,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onToggleQueue
                    )
                )
            }
        }
    }
}
