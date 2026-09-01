package com.aashik.music.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aashik.music.model.Song
import com.aashik.music.theme.AppGradients

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
    val shape = RoundedCornerShape(14.dp)
    val cardGradient = AppGradients.card(isActive = isPlaying)
    val borderBrush = AppGradients.border(isActive = isPlaying)

    Box(
        modifier = modifier
            .then(widthModifier)
            .height(72.dp) // Generous automotive touch target
            .clip(shape)
            .background(brush = cardGradient, shape = shape)
            .border(
                border = BorderStroke(if (isPlaying) 1.5.dp else 1.dp, borderBrush),
                shape = shape
            )
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
            Box(contentAlignment = Alignment.Center) {
                AlbumArtImage(
                    path = song.path,
                    size = 54.dp,
                    borderRadius = 10.dp,
                    isPlaying = isPlaying
                )
                if (isPlaying) {
                    // High-contrast active playing badge
                    Icon(
                        imageVector = Icons.Rounded.Equalizer,
                        contentDescription = "Playing",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = song.title.ifEmpty { "Unknown Title" },
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.SemiBold
                    ),
                    color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = if (isPlaying) Modifier.basicMarquee() else Modifier
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
        }
    }
}
