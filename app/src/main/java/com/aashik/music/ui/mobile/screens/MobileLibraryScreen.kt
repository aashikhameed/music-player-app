package com.aashik.music.ui.mobile.screens

import com.aashik.music.ui.mobile.glass.MobileTheme

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.*
import com.aashik.music.ui.designsystem.HorizontalDivider
import com.aashik.music.ui.designsystem.Icon
import com.aashik.music.ui.designsystem.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aashik.music.model.Song
import com.aashik.music.model.domain.DomainAlbum

enum class LibraryCategory(val title: String, val icon: ImageVector) {
    FAVORITES("Favorites", Icons.Rounded.Favorite),
    ARTISTS("Artists", Icons.Rounded.Person),
    TRACKS("Tracks", Icons.Rounded.MusicNote),
    ALBUMS("Albums", Icons.Rounded.Album),
    FOLDERS("Folders", Icons.Rounded.Folder),
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
    // Albums ordered by the newest file they contain (file timestamps read off the main thread).
    val albums by produceState(initialValue = emptyList<DomainAlbum>(), songs) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            songs.filter { !it.isStream() }
                .groupBy { it.album.ifBlank { "Unknown Album" } }
                .map { (albumTitle, albumSongs) ->
                    val newest = albumSongs.maxOf { runCatching { java.io.File(it.path).lastModified() }.getOrDefault(0L) }
                    newest to DomainAlbum(
                        title = albumTitle,
                        artist = albumArtistOf(albumSongs),
                        tracks = albumSongs
                    )
                }
                .sortedByDescending { it.first }
                .take(10)
                .map { it.second }
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
                    color = MobileTheme.text,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
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
                            color = MobileTheme.text.copy(alpha = 0.08f)
                        )
                    }
                }
            }
        }

        // ─── RECENTLY ADDED SECTION HEADER ──────────────────────────────────────
        item {
            Text(
                text = "Recently Added",
                color = MobileTheme.text,
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
            tint = MobileTheme.accent,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = category.title,
            color = MobileTheme.text,
            fontSize = 17.sp,
            fontWeight = FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )

        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = MobileTheme.textTertiary,
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
        // Square cover, sized to the card width
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            CollectionCover(album.tracks, size = maxWidth, cornerRadius = 10.dp, sharpSize = 256)
        }

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
            text = album.artist,
            color = MobileTheme.textSecondary,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

