package com.aashik.music.ui.mobile.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import com.aashik.music.ui.designsystem.Icon
import com.aashik.music.ui.designsystem.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aashik.music.model.Song
import com.aashik.music.model.domain.DomainAlbum
import com.aashik.music.ui.mobile.glass.MobileGlassBadgePill
import com.aashik.music.ui.mobile.glass.MobileGlassCard
import com.aashik.music.ui.mobile.glass.mobileLiquidGlass

/**
 * 1:1 Mobile Home Screen matching designScreen/mobile/Home.png.
 * Features Large Title 'Home' with user avatar, Featured 'Mood for You' Hero banner,
 * 'Top Picks for you' horizontal carousel, and 'Recently Played' grid.
 */
@Composable
fun MobileHomeScreen(
    songs: List<Song>,
    onSongClick: (Song) -> Unit,
    onAlbumClick: (DomainAlbum) -> Unit,
    onSeeAllTopPicks: () -> Unit,
    onSeeAllRecentlyPlayed: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Generate domain albums dynamically from library
    val albums = remember(songs) {
        songs.groupBy { it.album }
            .map { (albumTitle, albumSongs) ->
                val first = albumSongs.first()
                DomainAlbum(
                    title = albumTitle.ifBlank { "Unknown Album" },
                    artist = first.artist.ifBlank { "Unknown Artist" },
                    year = "2025",
                    genre = "Pop",
                    tracks = albumSongs
                )
            }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 120.dp) // Room for floating dock & nav bar
    ) {
        // ─── TOP LARGE TITLE & USER AVATAR ─────────────────────────────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Home",
                    color = Color.White,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                )

                // User Profile Pill / Avatar Button
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .mobileLiquidGlass(
                            shape = CircleShape,
                            tintColor = Color.White.copy(alpha = 0.12f),
                            borderColor = Color.White.copy(alpha = 0.22f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "JS",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // ─── FEATURED HERO BANNER: "Mood for You: Relax" ───────────────────────
        item {
            val heroAlbum = albums.firstOrNull()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .height(220.dp)
                    .shadow(
                        elevation = 16.dp,
                        shape = RoundedCornerShape(18.dp),
                        ambientColor = Color.Black.copy(alpha = 0.40f),
                        spotColor = Color.Black.copy(alpha = 0.50f)
                    )
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF1E293B))
                    .clickable { heroAlbum?.let { onAlbumClick(it) } }
            ) {
                // Gradient scrim ramp
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color(0xFF090D14).copy(alpha = 0.45f),
                                    Color(0xFF090D14).copy(alpha = 0.95f)
                                )
                            )
                        )
                )

                // Content inside banner
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Eyebrow badge
                    Row {
                        MobileGlassBadgePill(
                            text = "FEATURED PLAYLIST",
                            useCyan = true
                        )
                    }

                    Column {
                        Text(
                            text = "Mood for You: Relax",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Featuring ${heroAlbum?.artist ?: "Taylor Swift"}, Billie Eilish, Sabrina Carpenter",
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // ─── SECTION 1: "Top Picks for you" ───────────────────────────────────
        item {
            SectionHeader(
                title = "Top Picks for you",
                onSeeAll = onSeeAllTopPicks
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(albums) { album ->
                    AlbumCardItem(
                        album = album,
                        onClick = { onAlbumClick(album) }
                    )
                }
            }
        }

        // ─── SECTION 2: "Recently Played" ─────────────────────────────────────
        item {
            Spacer(modifier = Modifier.height(20.dp))
            SectionHeader(
                title = "Recently Played",
                onSeeAll = onSeeAllRecentlyPlayed
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(songs.take(10)) { song ->
                    SongCardItem(
                        song = song,
                        onClick = { onSongClick(song) }
                    )
                }
            }
        }

        // ─── SECTION 3: "Hits in Spatial Audio" ────────────────────────────────
        item {
            Spacer(modifier = Modifier.height(20.dp))
            SectionHeader(
                title = "Hits in Spatial Audio",
                onSeeAll = {}
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(albums.drop(1).take(6)) { album ->
                    AlbumCardItem(
                        album = album,
                        onClick = { onAlbumClick(album) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    onSeeAll: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onSeeAll
            )
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = "See All",
            tint = Color(0xFF64748B),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun AlbumCardItem(
    album: DomainAlbum,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(150.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    ) {
        // Thumbnail Card
        Box(
            modifier = Modifier
                .size(150.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(10.dp),
                    ambientColor = Color.Black.copy(alpha = 0.35f),
                    spotColor = Color.Black.copy(alpha = 0.45f)
                )
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF1E293B)),
            contentAlignment = Alignment.Center
        ) {
            Text("💿", fontSize = 42.sp)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = album.title,
            color = Color.White,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "${album.artist} • ${album.year}",
            color = Color(0xFF94A3B8),
            fontSize = 11.5.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SongCardItem(
    song: Song,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(150.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    ) {
        Box(
            modifier = Modifier
                .size(150.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(10.dp),
                    ambientColor = Color.Black.copy(alpha = 0.35f),
                    spotColor = Color.Black.copy(alpha = 0.45f)
                )
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF161E2E)),
            contentAlignment = Alignment.Center
        ) {
            Text("🎵", fontSize = 42.sp)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = song.title,
            color = Color.White,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = song.artist,
            color = Color(0xFF94A3B8),
            fontSize = 11.5.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

