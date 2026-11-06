package com.aashik.music.ui.mobile

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aashik.music.model.Song
import com.aashik.music.ui.AlbumArtImage

private val NextTrackCardShape = RoundedCornerShape(12.dp)
private val NextBadgeShape = RoundedCornerShape(6.dp)

/**
 * Glanceable Next Song preview row displayed just above the bottom player controls container in mobile portrait.
 *
 * Interaction:
 *  - Tap: Plays the next song immediately.
 *  - Long Press: Opens the user playback queue drawer.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MobileNextTrackPreview(
    nextSong: Song?,
    onPlayNext: () -> Unit,
    onOpenQueue: () -> Unit,
    isNightDimmerActive: Boolean,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    if (nextSong == null) return

    val containerBg = when {
        isNightDimmerActive -> Color(0xFF0F151F)
        isDark -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    }

    val containerBorder = when {
        isNightDimmerActive -> Color(0xFF1E293B)
        isDark -> Color(0xFF2D3B4E)
        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
    }

    val titleColor = when {
        isNightDimmerActive -> Color(0xFFE2E8F0)
        isDark -> Color.White
        else -> MaterialTheme.colorScheme.onSurface
    }

    val subtitleColor = when {
        isNightDimmerActive -> Color(0xFF94A3B8)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(color = containerBg, shape = NextTrackCardShape)
            .border(width = 0.8.dp, color = containerBorder, shape = NextTrackCardShape)
            .clip(NextTrackCardShape)
            .combinedClickable(
                onClick = onPlayNext,
                onLongClick = onOpenQueue
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // "NEXT" Badge
            Row(
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                        shape = NextBadgeShape
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "NEXT",
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 0.5.sp
                )
            }

            // Thumbnail
            AlbumArtImage(
                path = nextSong.path,
                artworkUrl = nextSong.artworkUrl,
                size = 32.dp,
                borderRadius = 6.dp,
                isPlaying = false,
                modifier = Modifier.size(32.dp)
            )

            // Title + Artist
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = nextSong.title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = titleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = nextSong.artist,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Normal,
                    color = subtitleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Trailing Skip Cue Icon
        Icon(
            imageVector = Icons.Rounded.SkipNext,
            contentDescription = "Play Next (Long press for queue)",
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
            modifier = Modifier.size(22.dp)
        )
    }
}

