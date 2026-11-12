package com.aashik.music.desktop.ui.shell

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aashik.music.desktop.model.DesktopSong
import com.aashik.music.desktop.ui.DesktopImageCache
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Small artwork tile that loads lazily; falls back to a note glyph. */
@Composable
internal fun DeskArt(song: DesktopSong?, size: androidx.compose.ui.unit.Dp, shape: androidx.compose.ui.graphics.Shape = DeskArt8, modifier: Modifier = Modifier) {
    val c = Desk.c
    var bmp by remember(song?.id) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(song?.id) { bmp = song?.let { DesktopImageCache.load(it.artworkUrl ?: it.path) } }
    Box(modifier.size(size).clip(shape).background(c.idleFill), contentAlignment = Alignment.Center) {
        val b = bmp
        if (b != null) Image(b, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        else DeskGlyph(Icons.Rounded.MusicNote, c.textTertiary, size / 2)
    }
}

/** Library page: title row (+ Play All / Shuffle), inline search, two-column 64 dp rows. */
@Composable
internal fun DeskLibrary(
    title: String,
    songs: List<DesktopSong>,
    currentSong: DesktopSong?,
    isPlaying: Boolean,
    favoriteIds: Set<String>,
    isSearchOpen: Boolean,
    query: String,
    bottomInset: androidx.compose.ui.unit.Dp,
    onQuery: (String) -> Unit,
    onCloseSearch: () -> Unit,
    onToggleFavorite: (DesktopSong) -> Unit,
    onPlay: (DesktopSong, List<DesktopSong>) -> Unit,
    onShuffle: (List<DesktopSong>) -> Unit,
    modifier: Modifier = Modifier
) {
    val c = Desk.c
    val shown = remember(songs, query) {
        if (query.isBlank()) songs else songs.filter { it.title.contains(query, true) || it.artist.contains(query, true) || it.album.contains(query, true) }
    }
    Column(modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp)) {
        Row(Modifier.fillMaxWidth().height(64.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(if (isSearchOpen) "Search" else title, color = c.text, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                Text(if (shown.size == 1) "1 song" else "${shown.size} songs", color = c.textSecondary, fontSize = 14.sp)
            }
            if (!isSearchOpen && shown.isNotEmpty()) {
                DeskPillButton("Play All", { onPlay(shown.first(), shown) }, icon = Icons.Rounded.PlayArrow)
                Spacer(Modifier.width(10.dp))
                DeskPillButton("Shuffle", { onShuffle(shown) }, icon = Icons.Rounded.Shuffle, accent = false)
            }
        }
        if (isSearchOpen) {
            val focus = remember { FocusRequester() }
            LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth().height(52.dp).background(c.idleFill, DeskPill).padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                DeskGlyph(Icons.Rounded.Search, c.textSecondary, 22.dp)
                Spacer(Modifier.width(12.dp))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) Text("Songs, artists, albums", color = c.textTertiary, fontSize = 16.sp)
                    BasicTextField(query, onQuery, singleLine = true, textStyle = TextStyle(color = c.text, fontSize = 16.sp), cursorBrush = SolidColor(c.accent), modifier = Modifier.fillMaxWidth().focusRequester(focus))
                }
                DeskIconButton(Icons.Rounded.Close, onCloseSearch, size = 36.dp, glyph = 18.dp)
            }
        }
        Spacer(Modifier.height(8.dp))
        if (shown.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(if (isSearchOpen) "No matches" else "No songs here yet. Pick a music folder in Settings.", color = c.textSecondary, fontSize = 16.sp)
            }
        } else BoxWithConstraints(Modifier.fillMaxSize()) {
            val columns = if (maxWidth >= 860.dp) 2 else 1
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                contentPadding = PaddingValues(bottom = bottomInset),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                items(shown, key = { it.id }) { song ->
                    SongRow(song, song.id == currentSong?.id, isPlaying, favoriteIds.contains(song.id), onToggleFavorite = { onToggleFavorite(song) }) { onPlay(song, shown) }
                }
            }
        }
    }
}

@Composable
private fun SongRow(song: DesktopSong, isCurrent: Boolean, isPlaying: Boolean, isFavorite: Boolean, onToggleFavorite: () -> Unit, onClick: () -> Unit) {
    val c = Desk.c
    Column {
        Row(Modifier.fillMaxWidth().height(64.dp).clickable(onClick = onClick), verticalAlignment = Alignment.CenterVertically) {
            DeskArt(song, 48.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(song.title.ifBlank { "Untitled" }, color = if (isCurrent) c.accent else c.text, fontSize = 16.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${song.artist.ifBlank { "Unknown Artist" }} • ${song.album}", color = c.textSecondary, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (isCurrent && isPlaying) { Text("▮▮", color = c.accent, fontSize = 10.sp); Spacer(Modifier.width(8.dp)) }
            if (song.duration > 0) Text(deskFormatTime(song.duration), color = c.textTertiary, fontSize = 13.sp)
            DeskIconButton(if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, onToggleFavorite, size = 44.dp, glyph = 20.dp, tint = if (isFavorite) c.accent else c.textSecondary)
        }
        Box(Modifier.fillMaxWidth().padding(start = 62.dp).height(0.5.dp).background(c.hairline))
    }
}
