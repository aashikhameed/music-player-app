package com.aashik.music.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
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
import com.aashik.music.ui.components.LiveAnimatedEqualizer

private val SongCardShape = RoundedCornerShape(12.dp)
private val SongArtShape = RoundedCornerShape(8.dp)
private val FocusBorderWidth = 1.8.dp

/**
 * Modern flat song item row matching automotive glanceable UI:
 * - 52dp square album artwork with 8dp rounded corners
 * - Bold title (15sp) and muted artist (13sp)
 * - Right row: active checkmark indicator (when playing), formatted duration (M:SS), and 3-dots more menu
 * - Full Android TV remote D-Pad focus border & highlight support
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
    @Suppress("UNUSED_PARAMETER") showOptions: Boolean = true,
    @Suppress("UNUSED_PARAMETER") isFavorite: Boolean = false,
    @Suppress("UNUSED_PARAMETER") onFavoriteClick: (() -> Unit)? = null
) {
    var isFocused by remember { mutableStateOf(false) }
    val widthModifier = if (cardWidth != null) Modifier.width(cardWidth) else Modifier.fillMaxWidth()
    val rowBackground = when {
        isFocused -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        isPlaying -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f)
        else -> Color.Transparent
    }

    Box(
        modifier = modifier
            .then(widthModifier)
            .height(68.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .then(
                if (isFocused) Modifier.border(FocusBorderWidth, MaterialTheme.colorScheme.primary, SongCardShape)
                else Modifier
            )
            .background(color = rowBackground, shape = SongCardShape)
            .clip(SongCardShape)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress
            )
    ) {
        // Left accent bar when currently playing
        if (isPlaying) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.primary)
            )
        }

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
                        fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.SemiBold
                    ),
                    color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = song.artist.ifEmpty { "Unknown Artist" },
                    style = MaterialTheme.typography.bodyMedium,
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
                    LiveAnimatedEqualizer(
                        isPlaying = isPlaying,
                        barColor = MaterialTheme.colorScheme.primary
                    )
                }

                val formattedDuration = song.getFormattedDuration()
                if (formattedDuration.isNotEmpty()) {
                    Text(
                        text = formattedDuration,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
