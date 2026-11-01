package com.aashik.music.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aashik.music.model.Song

private val SongCardShape = RoundedCornerShape(10.dp)
private val SongArtShape = RoundedCornerShape(8.dp)

private val PLAYING_EQ_HEIGHTS = floatArrayOf(0.40f, 0.95f, 0.60f, 0.85f)
private val IDLE_EQ_HEIGHTS = floatArrayOf(0.35f, 0.45f, 0.30f, 0.40f)

@Composable
fun LiveAnimatedEqualizer(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary
) {
    val heights = if (isPlaying) PLAYING_EQ_HEIGHTS else IDLE_EQ_HEIGHTS

    Canvas(modifier = modifier.size(18.dp)) {
        val totalHeight = size.height
        val barWidth = 3.dp.toPx()
        val spacing = 2.dp.toPx()
        val cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())

        for (i in 0 until 4) {
            val barH = totalHeight * heights[i]
            val left = i * (barWidth + spacing)
            val top = totalHeight - barH
            drawRoundRect(
                color = barColor,
                topLeft = Offset(left, top),
                size = Size(barWidth, barH),
                cornerRadius = cornerRadius
            )
        }
    }
}

/**
 * Modern flat song item row matching automotive glanceable UI:
 * - 52dp square album artwork with 8dp rounded corners
 * - Bold title (15sp) and muted artist (13sp)
 * - Right row: active checkmark indicator (when playing), formatted duration (M:SS), and 3-dots more menu
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SongCard(
    song: Song,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
    cardWidth: Dp? = null,
    @Suppress("UNUSED_PARAMETER") isFavorite: Boolean = false,
    @Suppress("UNUSED_PARAMETER") onFavoriteClick: (() -> Unit)? = null
) {
    val widthModifier = if (cardWidth != null) Modifier.width(cardWidth) else Modifier.fillMaxWidth()
    val rowBackground = if (isPlaying) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    } else {
        Color.Transparent
    }

    Box(
        modifier = modifier
            .then(widthModifier)
            .height(64.dp)
            .background(color = rowBackground, shape = SongCardShape)
            .clip(SongCardShape)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album Artwork (52dp, 8dp radius)
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(SongArtShape),
                contentAlignment = Alignment.Center
            ) {
                AlbumArtImage(
                    path = song.path,
                    artworkUrl = song.artworkUrl,
                    size = 52.dp,
                    borderRadius = 8.dp,
                    isPlaying = isPlaying
                )
            }

            // Title and Artist Column
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp, end = 8.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = song.title.ifEmpty { "Unknown Title" },
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.SemiBold,
                        fontSize = 15.sp
                    ),
                    color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = song.artist.ifEmpty { "Unknown Artist" },
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Right Row: Status / Played Checkmark, Duration, and 3-dots Menu
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (isPlaying) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = "Playing",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                val formattedDuration = song.getFormattedDuration()
                if (formattedDuration.isNotEmpty()) {
                    Text(
                        text = formattedDuration,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(SongCardShape)
                        .clickable(onClick = onLongPress),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = "Options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
