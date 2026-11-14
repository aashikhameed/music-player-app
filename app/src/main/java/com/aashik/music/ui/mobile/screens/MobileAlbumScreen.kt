package com.aashik.music.ui.mobile.screens

import com.aashik.music.ui.mobile.glass.MobileTheme
import com.aashik.music.ui.mobile.glass.MobileToolbarHeight

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import com.aashik.music.ui.designsystem.Button
import com.aashik.music.ui.designsystem.ButtonDefaults
import com.aashik.music.ui.designsystem.HorizontalDivider
import com.aashik.music.ui.designsystem.Icon
import com.aashik.music.ui.designsystem.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aashik.music.model.Song
import com.aashik.music.model.domain.DomainAlbum
import com.aashik.music.ui.mobile.glass.MobileGlassBadgePill
import com.aashik.music.ui.mobile.glass.MobilePlayShuffleRow

private val HeaderButtonShape = RoundedCornerShape(22.dp)

/**
 * 1:1 Mobile Album Screen matching designScreen/mobile/Album.png.
 * Features 240x240pt centered artwork, format badge pills,
 * Solid White Play and Frosted Glass Shuffle pill buttons,
 * expandable editorial review, and clean numbered tracklist without thumbnails.
 */
@Composable
fun MobileAlbumScreen(
    album: DomainAlbum,
    onBack: () -> Unit,
    onSongClick: (Song) -> Unit,
    onArtistClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    onPlay: () -> Unit = { album.tracks.firstOrNull()?.let(onSongClick) },
    onShuffle: () -> Unit = { album.tracks.randomOrNull()?.let(onSongClick) }
) {
    val quality = remember(album.tracks) { qualityLabel(album.tracks) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 140.dp)
    ) {
        // Safe area + room for the floating glass toolbar (drawn by MobileGlassMusicApp)
        item { Spacer(Modifier.statusBarsPadding().height(MobileToolbarHeight)) }

        // ─── 240x240pt HERO ARTWORK CARD ───────────────────────────────────────
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CollectionCover(album.tracks, size = 240.dp, cornerRadius = 14.dp)

                Spacer(modifier = Modifier.height(18.dp))

                // Album Title
                Text(
                    text = album.title,
                    color = MobileTheme.text,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    lineHeight = 28.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Artist link (plain text for compilations — there is no single artist page)
                val singleArtist = album.artist != "Various Artists" && album.artist != "Unknown Artist"
                Text(
                    text = album.artist,
                    color = if (singleArtist) MobileTheme.accent else MobileTheme.textSecondary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = if (singleArtist) Modifier.clickable { onArtistClick(album.artist) } else Modifier
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Real metadata only: genre/year when known, quality from the actual files.
                val meta = listOf(album.genre, album.year).filter { it.isNotBlank() }.joinToString(" • ")
                if (meta.isNotEmpty() || quality != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (meta.isNotEmpty()) {
                            Text(text = meta, color = MobileTheme.textSecondary, fontSize = 12.sp)
                        }
                        if (album.isDolbyAtmos) MobileGlassBadgePill(text = "DOLBY ATMOS")
                        quality?.let { MobileGlassBadgePill(text = it, useCyan = it == "LOSSLESS") }
                    }
                }
            }
        }

        // ─── ACTION BUTTONS: PLAY & SHUFFLE CAPSULES (160x44pt) ────────────────
        item {
            MobilePlayShuffleRow(onPlay = onPlay, onShuffle = onShuffle, buttonHeight = 44.dp)
        }

        // ─── EDITORIAL REVIEW (only when the album actually has one) ───────────
        if (album.editorialReview.isNotBlank()) {
            item {
                ExpandableText(album.editorialReview, Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp))
            }
        }

        // ─── CLEAN NUMBERED TRACKLIST ──────────────────────────────────────────
        itemsIndexed(album.tracks) { index, song ->
            AlbumNumberedTrackRow(
                index = index + 1,
                song = song,
                onClick = { onSongClick(song) }
            )
            HorizontalDivider(
                modifier = Modifier.padding(start = 52.dp, end = 20.dp),
                thickness = 0.5.dp,
                color = MobileTheme.text.copy(alpha = 0.05f)
            )
        }

        // ─── ALBUM FOOTER SUMMARY ──────────────────────────────────────────────
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
                Text(
                    text = runtimeLabel(album.tracks),
                    color = MobileTheme.textSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (album.year.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Released ${album.year}", color = MobileTheme.textTertiary, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun AlbumNumberedTrackRow(
    index: Int,
    song: Song,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Track Index Number
        Text(
            text = "$index",
            color = MobileTheme.textTertiary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(32.dp)
        )

        // Track Title & Featured Artists
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = song.title.ifBlank { "Untitled" },
                color = MobileTheme.text,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (song.artist.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = song.artist,
                    color = MobileTheme.textSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Duration (mm:ss)
        Text(
            text = formatDuration(song.duration),
            color = MobileTheme.textTertiary,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        SongMoreButton(song = song, tint = MobileTheme.textSecondary)
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}
