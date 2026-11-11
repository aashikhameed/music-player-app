package com.aashik.music.ui.mobile.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aashik.music.model.Song
import com.aashik.music.ui.AlbumArtImage
import com.aashik.music.ui.designsystem.HorizontalDivider
import com.aashik.music.ui.designsystem.Icon
import com.aashik.music.ui.designsystem.Text
import com.aashik.music.ui.mobile.glass.MobileTheme

/**
 * Song list row from designScreen/mobile/Playlist.png: rounded artwork, title over artist,
 * "•••" on the right and a hairline divider that starts under the text.
 */
@Composable
fun MobileSongRow(
    song: Song,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isCurrent: Boolean = false,
    subtitle: String = song.artist.ifBlank { "Unknown Artist" }
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .clickable(onClick = onClick)
                .padding(start = 20.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AlbumArtImage(
                path = song.path,
                artworkUrl = song.artworkUrl,
                size = 44.dp,
                borderRadius = 6.dp
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title.ifBlank { "Untitled" },
                    color = if (isCurrent) MobileTheme.accent else MobileTheme.text,
                    fontSize = 15.sp,
                    fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(1.dp))
                Text(
                    text = subtitle,
                    color = MobileTheme.textSecondary,
                    fontSize = 12.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            SongMoreButton(song = song)
        }
        HorizontalDivider(
            modifier = Modifier.padding(start = 76.dp),
            thickness = 0.5.dp,
            color = MobileTheme.text.copy(alpha = 0.10f)
        )
    }
}
