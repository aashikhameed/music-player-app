package com.aashik.music.ui.mobile.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.*
import com.aashik.music.ui.designsystem.HorizontalDivider
import com.aashik.music.ui.designsystem.Icon
import com.aashik.music.ui.designsystem.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aashik.music.model.Song
import com.aashik.music.model.domain.DomainAlbum
import com.aashik.music.ui.mobile.glass.mobileLiquidGlass

enum class LibraryCategory(val title: String, val icon: ImageVector) {
    PLAYLISTS("Playlists", Icons.Rounded.QueueMusic),
    ARTISTS("Artists", Icons.Rounded.Person),
    TRACKS("Tracks", Icons.Rounded.MusicNote),
    ALBUMS("Albums", Icons.Rounded.Album),
    VIDEOS("Music Videos", Icons.Rounded.Videocam),
    GENRES("Genres", Icons.Rounded.GraphicEq)
}

/**
 * 1:1 Mobile Library Screen matching designScreen/mobile/Library.png.
 * Features Large Title 'Library' with Cyan 'Edit' action,
 * Vertical category rows with Cyan icons and chevrons,
 * and 2-Column 'Recently Added' album card grid.
 */
@Composable
fun MobileLibraryScreen(
    songs: List<Song>,
    onCategoryClick: (LibraryCategory) -> Unit,
    onAlbumClick: (DomainAlbum) -> Unit,
    modifier: Modifier = Modifier
) {
    // Group songs into albums
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
        contentPadding = PaddingValues(bottom = 140.dp)
    ) {
        // ─── TOP LARGE TITLE & EDIT ACTION ──────────────────────────────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Library",
                    color = Color.White,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                )

                Text(
                    text = "Edit",
                    color = Color(0xFF00E5FF),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { /* Toggle editing */ }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                )
            }
        }

        // ─── VERTICAL CATEGORY NAVIGATION ROWS ──────────────────────────────────
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                LibraryCategory.values().forEachIndexed { index, category ->
                    LibraryCategoryRow(
                        category = category,
                        onClick = { onCategoryClick(category) }
                    )
                    if (index < LibraryCategory.values().size - 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 44.dp),
                            thickness = 0.5.dp,
                            color = Color.White.copy(alpha = 0.08f)
                        )
                    }
                }
            }
        }

        // ─── RECENTLY ADDED SECTION HEADER ──────────────────────────────────────
        item {
            Text(
                text = "Recently Added",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.3).sp,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 14.dp)
            )
        }

        // ─── 2-COLUMN RECENTLY ADDED GRID (Chunked in rows of 2 for LazyColumn) ───
        val chunkedAlbums = albums.chunked(2)
        items(chunkedAlbums.size) { rowIndex ->
            val rowAlbums = chunkedAlbums[rowIndex]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                rowAlbums.forEach { album ->
                    Box(modifier = Modifier.weight(1f)) {
                        RecentlyAddedAlbumCard(
                            album = album,
                            onClick = { onAlbumClick(album) }
                        )
                    }
                }
                // Spacer for odd number of albums
                if (rowAlbums.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun LibraryCategoryRow(
    category: LibraryCategory,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = category.icon,
            contentDescription = category.title,
            tint = Color(0xFF00E5FF),
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = category.title,
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )

        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = Color(0xFF64748B),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun RecentlyAddedAlbumCard(
    album: DomainAlbum,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        // 168x168pt Artwork Card
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF1E293B))
                .mobileLiquidGlass(RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            // Vinyl Disc Representation
            Box(
                modifier = Modifier
                    .fillMaxSize(0.65f)
                    .clip(CircleShape)
                    .background(Color(0xFF0F172A)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize(0.35f)
                        .clip(CircleShape)
                        .background(Color(0xFF00E5FF).copy(alpha = 0.35f))
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
            text = album.artist,
            color = Color(0xFF94A3B8),
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

