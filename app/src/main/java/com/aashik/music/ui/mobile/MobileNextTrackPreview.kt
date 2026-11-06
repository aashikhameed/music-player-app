package com.aashik.music.ui.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
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

private val NextTrackCardShape = RoundedCornerShape(14.dp)
private val NextBadgeShape = RoundedCornerShape(6.dp)
private val QueueButtonCircle = CircleShape

/**
 * Glanceable Next Song preview row displayed just above the bottom player controls container in mobile portrait.
 *
 * Interaction:
 *  - Tap on the card: Plays the next song immediately.
 *  - Tap on the queue button: Opens the user playback queue drawer / modal.
 */
@Composable
fun MobileNextTrackPreview(
    nextSong: Song?,
    onPlayNext: () -> Unit,
    onOpenQueue: () -> Unit,
    @Suppress("UNUSED_PARAMETER") isNightDimmerActive: Boolean = false,
    @Suppress("UNUSED_PARAMETER") isDark: Boolean = true,
    modifier: Modifier = Modifier
) {
    if (nextSong == null) return

    val containerBg = MaterialTheme.colorScheme.surfaceContainerHigh
    val containerBorder = MaterialTheme.colorScheme.outlineVariant
    val titleColor = MaterialTheme.colorScheme.onSurface

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(color = containerBg, shape = NextTrackCardShape)
            .border(width = 0.8.dp, color = containerBorder, shape = NextTrackCardShape)
            .clip(NextTrackCardShape)
            .clickable { onPlayNext() }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // "NEXT" Pill Badge
            Box(
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                        shape = NextBadgeShape
                    )
                    .padding(horizontal = 7.dp, vertical = 3.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "NEXT",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 0.7.sp
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

            // Song Title (Single line, artist removed)
            Text(
                text = nextSong.title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = titleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Queue Toggle Button (Tapping opens the queue modal)
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                    shape = QueueButtonCircle
                )
                .clip(QueueButtonCircle)
                .clickable { onOpenQueue() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                contentDescription = "Open Queue Modal",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
