package com.aashik.music.ui.mobile.screens

import com.aashik.music.ui.mobile.glass.MobileTheme

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import com.aashik.music.ui.designsystem.Button
import com.aashik.music.ui.designsystem.ButtonDefaults
import com.aashik.music.ui.designsystem.Icon
import com.aashik.music.ui.designsystem.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aashik.music.model.Song
import com.aashik.music.model.domain.DomainAlbum
import com.aashik.music.model.domain.DomainArtist
import com.aashik.music.ui.mobile.glass.mobileLiquidGlass

/**
 * 1:1 Mobile Artist Screen matching designScreen/mobile/Artist.png.
 * Features 360pt full-bleed portrait hero header,
 * Solid White Play and Frosted Glass Shuffle capsules,
 * 4-Row 'Top Songs' section with rank numbers and artwork thumbnails,
 * 'Essential Albums' horizontal carousel, and 'About' artist biography card.
 */
@Composable
fun MobileArtistScreen(
    artist: DomainArtist,
    onBack: () -> Unit,
    onSongClick: (Song) -> Unit,
    onAlbumClick: (DomainAlbum) -> Unit,
    modifier: Modifier = Modifier,
    onPlay: () -> Unit = { artist.topSongs.firstOrNull()?.let(onSongClick) },
    onShuffle: () -> Unit = { artist.topSongs.randomOrNull()?.let(onSongClick) }
) {

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 140.dp)
    ) {
        // ─── 360pt FULL-BLEED HERO PORTRAIT HEADER ─────────────────────────────
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
            ) {
                // Full-bleed artwork from the artist's first song, fading into the page.
                artist.topSongs.firstOrNull()?.let { first ->
                    com.aashik.music.ui.AlbumArtImage(
                        path = first.path,
                        artworkUrl = first.artworkUrl,
                        size = 340.dp,
                        borderRadius = 0.dp,
                        sharpSize = 512,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Transparent,
                                0.45f to MobileTheme.background.copy(alpha = 0.2f),
                                0.75f to MobileTheme.background.copy(alpha = 0.85f),
                                1f to MobileTheme.background
                            )
                        )
                )


                // Artist Name at bottom of header
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 20.dp, end = 20.dp, bottom = 16.dp)
                ) {
                    Text(
                        text = artist.name,
                        color = MobileTheme.text,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    )
                }
            }
        }

        // ─── ACTION CAPSULES (PLAY & SHUFFLE) ───────────────────────────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onPlay,
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp),
                    shape = RoundedCornerShape(21.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MobileTheme.text,
                        contentColor = MobileTheme.onText
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = "Play",
                            tint = MobileTheme.onText,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Play",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .mobileLiquidGlass(
                            shape = RoundedCornerShape(21.dp),
                            tintColor = MobileTheme.text.copy(alpha = 0.10f)
                        )
                        .clickable(onClick = onShuffle),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Shuffle,
                            contentDescription = "Shuffle",
                            tint = MobileTheme.text,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Shuffle",
                            color = MobileTheme.text,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }

        // ─── TOP SONGS SECTION ─────────────────────────────────────────────────
        item {
            Text(
                text = "Songs",
                color = MobileTheme.text,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.3).sp,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 12.dp)
            )
        }

        itemsIndexed(artist.topSongs) { index, song ->
            ArtistTopSongItemRow(
                rank = index + 1,
                song = song,
                onClick = { onSongClick(song) }
            )
        }

        // ─── ESSENTIAL ALBUMS SECTION ──────────────────────────────────────────
        if (artist.albums.isNotEmpty()) {
            item {
                Text(
                    text = "Albums",
                    color = MobileTheme.text,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.3).sp,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 12.dp)
                )
            }

            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(artist.albums) { album ->
                        Column(
                            modifier = Modifier
                                .width(150.dp)
                                .clickable { onAlbumClick(album) }
                        ) {
                            CollectionCover(album.tracks, size = 150.dp, cornerRadius = 12.dp, sharpSize = 256)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = album.title,
                                color = MobileTheme.text,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = album.year.ifBlank { if (album.tracks.size == 1) "1 song" else "${album.tracks.size} songs" },
                                color = MobileTheme.textSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // ─── 'ABOUT' — only when real artist info exists ────────────────────────
        if (artist.biography.isNotBlank()) {
            item {
                Text(
                    text = "About ${artist.name}",
                    color = MobileTheme.text,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.3).sp,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 12.dp)
                )
            }
            item {
                ExpandableText(artist.biography, Modifier.fillMaxWidth().padding(horizontal = 20.dp))
            }
        }
    }
}

@Composable
private fun ArtistTopSongItemRow(
    rank: Int,
    song: Song,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Popularity Rank Number
        Text(
            text = "$rank",
            color = MobileTheme.textTertiary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(26.dp)
        )

        com.aashik.music.ui.AlbumArtImage(path = song.path, artworkUrl = song.artworkUrl, size = 44.dp, borderRadius = 6.dp)

        Spacer(modifier = Modifier.width(14.dp))

        // Title and Album
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title.ifBlank { "Untitled" },
                color = MobileTheme.text,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = song.album.ifBlank { "Single" },
                color = MobileTheme.textSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        SongMoreButton(song = song, tint = MobileTheme.textSecondary)
    }
}
