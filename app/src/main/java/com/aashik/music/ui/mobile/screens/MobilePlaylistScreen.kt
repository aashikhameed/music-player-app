package com.aashik.music.ui.mobile.screens

import com.aashik.music.ui.mobile.glass.MobileTheme
import com.aashik.music.ui.mobile.glass.MobileToolbarHeight

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import com.aashik.music.ui.designsystem.Button
import com.aashik.music.ui.designsystem.ButtonDefaults
import com.aashik.music.ui.designsystem.Icon
import com.aashik.music.ui.designsystem.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aashik.music.model.Song
import com.aashik.music.model.domain.DomainArtist
import com.aashik.music.model.domain.DomainPlaylist
import com.aashik.music.ui.mobile.glass.mobileLiquidGlass

private val HeaderButtonShape = RoundedCornerShape(22.dp)

/**
 * 1:1 Mobile Playlist Detail Screen matching designScreen/mobile/Playlist.png.
 * Features 240x240pt mosaic artwork, curator tag in Cyan,
 * Play & Shuffle capsules, tracklist rows with 42x42pt thumbnails,
 * and 80x80pt 'Featured Artists' circular avatar carousel.
 */
@Composable
fun MobilePlaylistScreen(
    playlist: DomainPlaylist,
    onBack: () -> Unit,
    onSongClick: (Song) -> Unit,
    onArtistClick: (DomainArtist) -> Unit,
    modifier: Modifier = Modifier,
    onPlay: () -> Unit = { playlist.songs.firstOrNull()?.let(onSongClick) },
    onShuffle: () -> Unit = { playlist.songs.randomOrNull()?.let(onSongClick) }
) {

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 140.dp)
    ) {
        // Safe area + room for the floating glass toolbar (drawn by MobileGlassMusicApp)
        item { Spacer(Modifier.statusBarsPadding().height(MobileToolbarHeight)) }

        // ─── 240x240pt MOSAIC HERO ARTWORK CARD ────────────────────────────────
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CollectionMosaic(playlist.songs, size = 240.dp, cornerRadius = 14.dp)

                Spacer(modifier = Modifier.height(18.dp))

                // Playlist Title
                Text(
                    text = playlist.title,
                    color = MobileTheme.text,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    lineHeight = 28.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Curator Tag in Cyan
                if (playlist.curator.isNotBlank()) {
                    Text(
                        text = playlist.curator,
                        color = MobileTheme.accent,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Metadata summary
                Text(
                    text = listOf(playlist.lastUpdated, runtimeLabel(playlist.songs)).filter { it.isNotBlank() }.joinToString(" • "),
                    color = MobileTheme.textSecondary,
                    fontSize = 12.sp
                )
            }
        }

        // ─── ACTION BUTTONS: PLAY & SHUFFLE CAPSULES ───────────────────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Solid White Play Capsule
                Button(
                    onClick = onPlay,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = HeaderButtonShape,
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

                // Frosted Glass Shuffle Capsule
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .mobileLiquidGlass(
                            shape = HeaderButtonShape,
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

        // ─── EDITORIAL DESCRIPTION ────────────────────────────────────────────
        if (playlist.description.isNotBlank()) {
            item {
                ExpandableText(playlist.description, Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp))
            }
        }

        if (playlist.songs.isEmpty()) {
            item {
                Text(
                    text = "No songs here yet.",
                    color = MobileTheme.textTertiary,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 32.dp)
                )
            }
        }

        // ─── TRACKLIST ROWS ────────────────────────────────────────────────────
        itemsIndexed(playlist.songs) { _, song ->
            MobileSongRow(
                song = song,
                onClick = { onSongClick(song) }
            )
        }

        // ─── FEATURED ARTISTS CAROUSEL ─────────────────────────────────────────
        if (playlist.featuredArtists.isNotEmpty()) {
            item {
                Text(
                    text = "Featured Artists",
                    color = MobileTheme.text,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.3).sp,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 12.dp)
                )
            }

            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(playlist.featuredArtists) { artist ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .width(80.dp)
                                .clickable { onArtistClick(artist) }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .background(MobileTheme.surface),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Person,
                                    contentDescription = artist.name,
                                    tint = MobileTheme.accent,
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = artist.name,
                                color = MobileTheme.text,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}


