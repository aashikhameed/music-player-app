package com.aashik.music.desktop.ui.shell

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aashik.music.desktop.model.DesktopRadioCategory
import com.aashik.music.desktop.model.DesktopRadioData
import com.aashik.music.desktop.model.DesktopRadioStation
import com.aashik.music.desktop.model.DesktopSong
import com.aashik.music.desktop.service.DesktopOnlineRepository
import com.aashik.music.desktop.ui.DesktopImageCache
import java.io.File

/** Folders grid; picking one opens its songs through [onOpen]. */
@Composable
internal fun DeskFolders(songs: List<DesktopSong>, bottomInset: Dp, onOpen: (String) -> Unit, modifier: Modifier = Modifier) {
    val c = Desk.c
    val folders = remember(songs) { songs.groupBy { File(it.path).parent ?: "Local Music" }.entries.sortedBy { it.key.lowercase() } }
    Column(modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp)) {
        Column(Modifier.height(64.dp), verticalArrangement = Arrangement.Center) {
            Text("Folders", color = c.text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(if (folders.size == 1) "1 folder" else "${folders.size} folders", color = c.textSecondary, fontSize = 14.sp)
        }
        Spacer(Modifier.height(8.dp))
        if (folders.isEmpty()) Box(Modifier.fillMaxSize(), Alignment.Center) { Text("No folders yet", color = c.textSecondary, fontSize = 16.sp) }
        else LazyVerticalGrid(GridCells.Adaptive(240.dp), contentPadding = PaddingValues(bottom = bottomInset), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(folders, key = { it.key }) { (path, list) ->
                Row(Modifier.fillMaxWidth().height(64.dp).deskSurface(14.dp).clickable { onOpen(path) }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    DeskGlyph(Icons.Rounded.Folder, c.accent, 28.dp)
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(File(path).name.ifBlank { path }, color = c.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(if (list.size == 1) "1 song" else "${list.size} songs", color = c.textSecondary, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryTile(cat: DesktopRadioCategory, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = Desk.c
    var bmp by remember(cat.id) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(cat.id) { if (cat.imageUrl.isNotBlank()) bmp = DesktopImageCache.load(cat.imageUrl) }
    Column(modifier.clickable(onClick = onClick)) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(DeskCard).background(Color(cat.stripeColor).copy(alpha = 0.35f))) {
            bmp?.let { Image(it, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
        }
        Spacer(Modifier.height(8.dp))
        Text(cat.title, color = c.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Radio & Genres: live stations, then mood and genre tiles; a tile opens its online top songs. */
@Composable
internal fun DeskRadio(
    current: DesktopSong?,
    bottomInset: Dp,
    onPlay: (DesktopSong, List<DesktopSong>) -> Unit,
    modifier: Modifier = Modifier
) {
    val c = Desk.c
    val scope = rememberCoroutineScope()
    var category by remember { mutableStateOf<DesktopRadioCategory?>(null) }
    var results by remember { mutableStateOf<List<DesktopSong>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    val stations = remember { DesktopRadioData.CURATED_STATIONS }

    val cat = category
    if (cat != null) {
        LaunchedEffect(cat.id) {
            loading = true; results = emptyList()
            results = runCatching { DesktopOnlineRepository.searchSongs("${cat.title} Top Songs", limit = 25) }.getOrDefault(emptyList())
            loading = false
        }
        Column(modifier.padding(start = 24.dp, end = 24.dp, top = 12.dp)) {
            Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
                DeskIconButton(Icons.Rounded.ArrowBack, { category = null }, size = 40.dp, glyph = 20.dp, glass = true)
                Spacer(Modifier.width(12.dp))
                Text(cat.title, color = c.text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
            if (results.isEmpty()) Box(Modifier.fillMaxSize(), Alignment.Center) { Text(if (loading) "Loading ${cat.title}…" else "Nothing found. Check your connection.", color = c.textSecondary, fontSize = 15.sp) }
            else DeskSongTable(results, current, false, emptyMap(), emptySet(), Modifier.fillMaxSize(), bottomInset = bottomInset, onPlay = { s -> onPlay(s, results) }, onMore = {}, onToggleFavorite = {})
        }
        return
    }
    val moods = remember { DesktopRadioData.MOOD_CATEGORIES }
    val genres = remember { DesktopRadioData.GENRE_CATEGORIES }
    LazyColumn(modifier.padding(horizontal = 24.dp), contentPadding = PaddingValues(top = 20.dp, bottom = bottomInset)) {
        item {
            Row(Modifier.fillMaxWidth().height(64.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Radio & Genres", color = c.text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("${stations.size} live stations", color = c.textSecondary, fontSize = 14.sp)
                }
                DeskPillButton("Surprise Me", { stations.randomOrNull()?.let { onPlay(it.toSong(), emptyList()) } }, icon = Icons.Rounded.Casino)
            }
            Spacer(Modifier.height(12.dp))
            SectionTitle("Live Stations")
        }
        items(stations.size) { i -> StationRow(stations[i], current?.id == stations[i].id) { onPlay(stations[i].toSong(), emptyList()) } }
        item { Spacer(Modifier.height(16.dp)); SectionTitle("Moods & Moments") }
        items(moods.chunked(6)) { row -> TileRow(row) { category = it } }
        item { Spacer(Modifier.height(16.dp)); SectionTitle("Genres") }
        items(genres.chunked(6)) { row -> TileRow(row) { category = it } }
    }
}

@Composable
private fun SectionTitle(t: String) {
    Text(t, color = Desk.c.text, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 6.dp))
}

@Composable
private fun TileRow(row: List<DesktopRadioCategory>, onClick: (DesktopRadioCategory) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        row.forEach { CategoryTile(it, { onClick(it) }, Modifier.weight(1f)) }
        repeat(6 - row.size) { Spacer(Modifier.weight(1f)) }
    }
}

@Composable
private fun StationRow(s: DesktopRadioStation, isCurrent: Boolean, onClick: () -> Unit) {
    val c = Desk.c
    Row(Modifier.fillMaxWidth().height(52.dp).clickable(onClick = onClick), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(38.dp).height(38.dp).background(c.idleFill, DeskArt8), Alignment.Center) { DeskGlyph(Icons.Rounded.Radio, if (isCurrent) c.accent else c.textSecondary, 24.dp) }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(s.name, color = if (isCurrent) c.accent else c.text, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${s.genre} • ${s.bitrate}", color = c.textSecondary, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

internal class DeskGroup(val key: String, val title: String, val subtitle: String, val cover: DesktopSong?, val songs: List<DesktopSong>)

/** Artists / Albums / Playlists: a grid of cover tiles; picking one opens its songs. */
@Composable
internal fun DeskGroupGrid(title: String, groups: List<DeskGroup>, bottomInset: Dp, onOpen: (DeskGroup) -> Unit, modifier: Modifier = Modifier, round: Boolean = false) {
    val c = Desk.c
    Column(modifier.padding(horizontal = 24.dp)) {
        Text(title, color = c.text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.height(60.dp).padding(top = 18.dp))
        if (groups.isEmpty()) Box(Modifier.fillMaxSize(), Alignment.Center) { Text("Nothing here yet", color = c.textSecondary, fontSize = 15.sp) }
        else LazyVerticalGrid(GridCells.Adaptive(130.dp), contentPadding = PaddingValues(bottom = bottomInset), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            items(groups, key = { it.key }) { g ->
                Column(Modifier.clickable { onOpen(g) }) {
                    Box(Modifier.fillMaxWidth().aspectRatio(1f)) {
                        DeskArtFill(g.cover, if (round) androidx.compose.foundation.shape.CircleShape else DeskCard)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(g.title, color = c.text, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(g.subtitle, color = c.textSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
internal fun DeskArtFill(song: DesktopSong?, shape: androidx.compose.ui.graphics.Shape) {
    val c = Desk.c
    var bmp by remember(song?.id) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(song?.id) { bmp = song?.let { DesktopImageCache.load(it.artworkUrl ?: it.path) } }
    Box(Modifier.fillMaxSize().clip(shape).background(c.idleFill), Alignment.Center) {
        val b = bmp
        if (b != null) Image(b, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        else DeskGlyph(Icons.Rounded.MusicNote, c.textTertiary, 40.dp)
    }
}

/** Home: shelves of recently added and most played songs. */
@Composable
internal fun DeskHome(recent: List<DesktopSong>, top: List<DesktopSong>, bottomInset: Dp, onPlay: (DesktopSong, List<DesktopSong>) -> Unit, modifier: Modifier = Modifier) {
    val c = Desk.c
    LazyColumn(modifier.padding(horizontal = 24.dp), contentPadding = PaddingValues(bottom = bottomInset)) {
        item { Text("Home", color = c.text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.height(60.dp).padding(top = 18.dp)) }
        if (recent.isEmpty()) item { Text("Add music in Settings to get started.", color = c.textSecondary, fontSize = 15.sp) }
        if (recent.isNotEmpty()) item { Shelf("Recently Added", recent, onPlay) }
        if (top.isNotEmpty()) item { Spacer(Modifier.height(16.dp)); Shelf("Most Played", top, onPlay) }
    }
}

@Composable
private fun Shelf(title: String, songs: List<DesktopSong>, onPlay: (DesktopSong, List<DesktopSong>) -> Unit) {
    val c = Desk.c
    Text(title, color = c.text, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
    androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        items(songs.size) { i ->
            val s = songs[i]
            Column(Modifier.width(120.dp).clickable { onPlay(s, songs.drop(i + 1)) }) {
                Box(Modifier.size(120.dp)) { DeskArtFill(s, DeskCard) }
                Spacer(Modifier.height(8.dp))
                Text(s.title.ifBlank { "Untitled" }, color = c.text, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(s.artist, color = c.textSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
