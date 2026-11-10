package com.aashik.music.ui.mobile.screens

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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    modifier: Modifier = Modifier
) {
    var isFavorite by remember { mutableStateOf(false) }
    var isBioExpanded by remember { mutableStateOf(false) }

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
                // Background Gradient Simulation
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF1E293B),
                                    Color(0xFF0F172A),
                                    Color(0xFF0D0F12)
                                )
                            )
                        )
                )

                // Top Navigation Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                        .align(Alignment.TopCenter),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(38.dp)
                            .mobileLiquidGlass(
                                shape = CircleShape,
                                tintColor = Color.Black.copy(alpha = 0.35f),
                                borderColor = Color.White.copy(alpha = 0.15f)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = { isFavorite = !isFavorite },
                            modifier = Modifier
                                .size(38.dp)
                                .mobileLiquidGlass(
                                    shape = CircleShape,
                                    tintColor = Color.Black.copy(alpha = 0.35f),
                                    borderColor = Color.White.copy(alpha = 0.15f)
                                )
                        ) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                                contentDescription = "Favorite",
                                tint = if (isFavorite) Color(0xFF00E5FF) else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(
                            onClick = {},
                            modifier = Modifier
                                .size(38.dp)
                                .mobileLiquidGlass(
                                    shape = CircleShape,
                                    tintColor = Color.Black.copy(alpha = 0.35f),
                                    borderColor = Color.White.copy(alpha = 0.15f)
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MoreVert,
                                contentDescription = "Options",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Artist Name at bottom of header
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 20.dp, end = 20.dp, bottom = 16.dp)
                ) {
                    Text(
                        text = artist.name,
                        color = Color.White,
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
                    onClick = { artist.topSongs.firstOrNull()?.let(onSongClick) },
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp),
                    shape = RoundedCornerShape(21.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = "Play",
                            tint = Color.Black,
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
                            tintColor = Color.White.copy(alpha = 0.10f),
                            borderColor = Color.White.copy(alpha = 0.15f)
                        )
                        .clickable {
                            val shuffled = artist.topSongs.shuffled()
                            shuffled.firstOrNull()?.let(onSongClick)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Shuffle,
                            contentDescription = "Shuffle",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Shuffle",
                            color = Color.White,
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
                text = "Top Songs",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.3).sp,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 12.dp)
            )
        }

        itemsIndexed(artist.topSongs.take(5)) { index, song ->
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
                    text = "Essential Albums",
                    color = Color.White,
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
                            Box(
                                modifier = Modifier
                                    .size(150.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF1E293B))
                                    .mobileLiquidGlass(RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize(0.6f)
                                        .clip(CircleShape)
                                        .background(Color(0xFF0F172A)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize(0.35f)
                                            .clip(CircleShape)
                                            .background(Color(0xFF00E5FF).copy(alpha = 0.4f))
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = album.title,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = album.year,
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // ─── 'ABOUT' ARTIST BIOGRAPHY CARD ─────────────────────────────────────
        item {
            Text(
                text = "About ${artist.name}",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.3).sp,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 12.dp)
            )
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .mobileLiquidGlass(
                        shape = RoundedCornerShape(18.dp),
                        tintColor = Color.White.copy(alpha = 0.05f),
                        borderColor = Color.White.copy(alpha = 0.10f)
                    )
                    .padding(18.dp)
            ) {
                Column {
                    // Origin Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("ORIGIN", color = Color(0xFF64748B), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(artist.origin, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Column {
                            Text("GENRE", color = Color(0xFF64748B), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(artist.genre, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                        if (artist.born.isNotBlank()) {
                            Column {
                                Text("BORN", color = Color(0xFF64748B), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text(artist.born, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Biography Body
                    Text(
                        text = artist.biography,
                        color = Color(0xFFCBD5E1),
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        maxLines = if (isBioExpanded) Int.MAX_VALUE else 3,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (isBioExpanded) "LESS" else "MORE",
                        color = Color(0xFF00E5FF),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { isBioExpanded = !isBioExpanded }
                    )
                }
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
            color = Color(0xFF64748B),
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(26.dp)
        )

        // 44x44pt Thumbnail
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF1E293B)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = Color(0xFF00E5FF).copy(alpha = 0.7f),
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Title and Album
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title.ifBlank { "Untitled" },
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = song.album.ifBlank { "Single" },
                color = Color(0xFF94A3B8),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // More Options
        IconButton(
            onClick = {},
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = "Options",
                tint = Color(0xFF64748B),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
