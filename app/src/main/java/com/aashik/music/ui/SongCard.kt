package com.aashik.music.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aashik.music.model.Song
import com.aashik.music.theme.AppGradients

private val SongCardShape = RoundedCornerShape(14.dp)
private val ArtVignetteShape = RoundedCornerShape(10.dp)
private val BadgeShape = RoundedCornerShape(6.dp)
private val EqualizerBarShape = RoundedCornerShape(1.dp)

@Composable
fun LiveAnimatedEqualizer(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary
) {
    val h1 = if (isPlaying) 0.40f else 0.35f
    val h2 = if (isPlaying) 0.95f else 0.45f
    val h3 = if (isPlaying) 0.60f else 0.30f
    val h4 = if (isPlaying) 0.85f else 0.40f

    Row(
        modifier = modifier.height(18.dp).width(18.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight(h1)
                .clip(EqualizerBarShape)
                .background(barColor)
        )
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight(h2)
                .clip(EqualizerBarShape)
                .background(barColor)
        )
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight(h3)
                .clip(EqualizerBarShape)
                .background(barColor)
        )
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight(h4)
                .clip(EqualizerBarShape)
                .background(barColor)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SongCard(
    song: Song,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
    cardWidth: Dp? = null
) {
    val widthModifier = if (cardWidth != null) Modifier.width(cardWidth) else Modifier.fillMaxWidth()
    val cardGradient = AppGradients.card(isActive = isPlaying)
    val borderBrush = AppGradients.border(isActive = isPlaying)
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    // Codec extension and duration format — memoized per track
    val ext = remember(song.path) { song.path.substringAfterLast('.', "").uppercase().take(4) }
    val formattedDuration = remember(song.duration) { formatSongDuration(song.duration) }

    Box(
        modifier = modifier
            .then(widthModifier)
            .height(76.dp) // 1280×720 @ 160dpi: 76dp card = clear automotive touch target
            .clip(SongCardShape)
            .background(brush = cardGradient, shape = SongCardShape)
            .border(
                width = if (isPlaying) 1.6.dp else 1.dp,
                brush = borderBrush,
                shape = SongCardShape
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album Artwork with active indicator
            Box(contentAlignment = Alignment.Center) {
                AlbumArtImage(
                    path = song.path,
                    size = 56.dp,  // 56dp — fills the 76dp card nicely at 160dpi
                    borderRadius = 10.dp,
                    isPlaying = isPlaying
                )

                if (isPlaying) {
                    // Semi-translucent dark vignette over art with live equalizer overlay
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.Black.copy(alpha = 0.45f)),
                        contentAlignment = Alignment.Center
                    ) {
                        LiveAnimatedEqualizer(
                            isPlaying = true,
                            barColor = if (isDark) MaterialTheme.colorScheme.primary else Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Metadata Info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = song.title.ifEmpty { "Unknown Title" },
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.SemiBold,
                        fontSize = 15.sp  // 15sp for glanceable at automotive distance on 160dpi
                    ),
                    color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(3.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = song.artist.ifEmpty { "Unknown Artist" },
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp), // bumped for 160dpi
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    // Codec & Duration Micro-Pill
                    val badgeBg = if (isPlaying) {
                        MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.22f else 0.14f)
                    } else {
                        if (isDark) Color(0xFF1E2638) else Color(0xFFE2E8F0)
                    }
                    val badgeTextColor = if (isPlaying) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
                    }

                    Box(
                        modifier = Modifier
                            .clip(BadgeShape)
                            .background(badgeBg)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (ext.isNotEmpty()) "$ext • $formattedDuration" else formattedDuration,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeTextColor,
                            letterSpacing = 0.4.sp
                        )
                    }
                }
            }
        }
    }
}

private fun formatSongDuration(durationMs: Long): String {
    if (durationMs <= 0) return "00:00"
    val totalSeconds = (durationMs / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val m = if (minutes < 10) "0$minutes" else minutes.toString()
    val s = if (seconds < 10) "0$seconds" else seconds.toString()
    return "$m:$s"
}
