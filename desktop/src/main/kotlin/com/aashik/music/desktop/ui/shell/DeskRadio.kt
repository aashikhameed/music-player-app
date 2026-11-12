package com.aashik.music.desktop.ui.shell

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.luminance
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

private val TileShape = RoundedCornerShape(14.dp)
private val FavRed = Color(0xFFFF2D55)

/**
 * Radio & Genres, laid out like the headunit: Favorites on top (saved genres + stations), then all genres & moods as
 * a tile grid. A genre opens its Songs shelf and its Live radio stations.
 */
@Composable
internal fun DeskRadio(
    current: DesktopSong?,
    bottomInset: Dp,
    onPlay: (DesktopSong, List<DesktopSong>) -> Unit,
    onStartRadio: (DesktopSong) -> Unit,
    modifier: Modifier = Modifier
) {
    val c = Desk.c
    var category by remember { mutableStateOf<DesktopRadioCategory?>(null) }
    var favCats by remember { mutableStateOf(DeskPrefs.favCategories()) }
    var favStations by remember { mutableStateOf(DeskPrefs.favStations()) }
    val stations = remember { DesktopRadioData.CURATED_STATIONS }
    val categories = remember { DesktopRadioData.CATEGORIES }
    fun toggleCat(id: String) { favCats = if (id in favCats) favCats - id else favCats + id; DeskPrefs.saveFavCategories(favCats) }
    fun toggleStation(id: String) { favStations = if (id in favStations) favStations - id else favStations + id; DeskPrefs.saveFavStations(favStations) }

    val cat = category
    if (cat != null) {
        GenreDetail(cat, stations.filter { it.categoryId == cat.id }, current, favStations, bottomInset, onPlay, onStartRadio, ::toggleStation, { category = null }, modifier)
        return
    }

    val favCategoryList = categories.filter { it.id in favCats }
    val favStationList = stations.filter { it.id in favStations }
    LazyVerticalGrid(
        GridCells.Adaptive(230.dp), modifier.padding(horizontal = 24.dp),
        contentPadding = PaddingValues(top = 20.dp, bottom = bottomInset),
        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }, key = "head") {
            Row(Modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Radio & Genres", color = c.text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("Live stations, moods and genres", color = c.textSecondary, fontSize = 14.sp)
                }
                DeskPillButton("Surprise Me", { stations.randomOrNull()?.let { onPlay(it.toSong(), emptyList()) } }, icon = Icons.Rounded.Shuffle)
            }
        }
        if (favCategoryList.isNotEmpty() || favStationList.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "h_fav") { GridHeader("Favorites", "${favCategoryList.size + favStationList.size} saved") }
            items(favCategoryList, key = { "fav_${it.id}" }) { g -> CategoryTile(g, categories.indexOf(g), true, { toggleCat(g.id) }, { category = g }) }
            items(favStationList, key = { "fav_st_${it.id}" }, span = { GridItemSpan(maxLineSpan) }) { s ->
                StationRow(s, current?.path == s.streamUrl, true, { onPlay(s.toSong(), emptyList()) }, { toggleStation(s.id) }, { onStartRadio(s.toSong()) })
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }, key = "h_all") { GridHeader("All genres & moods", null) }
        items(categories, key = { it.id }) { g -> CategoryTile(g, categories.indexOf(g), g.id in favCats, { toggleCat(g.id) }, { category = g }) }
    }
}

@Composable
private fun GridHeader(title: String, trailing: String?) {
    val c = Desk.c
    Row(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = c.text, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        if (trailing != null) Text(trailing, color = c.textTertiary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** Genre drill-down: Songs shelf (Jio) + the live stations of that genre. */
@Composable
private fun GenreDetail(
    cat: DesktopRadioCategory, stations: List<DesktopRadioStation>, current: DesktopSong?, favStations: Set<String>, bottomInset: Dp,
    onPlay: (DesktopSong, List<DesktopSong>) -> Unit, onStartRadio: (DesktopSong) -> Unit, onToggleStation: (String) -> Unit,
    onBack: () -> Unit, modifier: Modifier
) {
    val c = Desk.c
    var songs by remember(cat.id) { mutableStateOf<List<DesktopSong>>(emptyList()) }
    var loading by remember(cat.id) { mutableStateOf(true) }
    LaunchedEffect(cat.id) {
        loading = true
        songs = runCatching { DesktopOnlineRepository.searchSongs("${cat.title} Hits", limit = 20) }.getOrDefault(emptyList())
        loading = false
    }
    Column(modifier.padding(start = 24.dp, end = 24.dp, top = 12.dp)) {
        Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
            DeskIconButton(Icons.Rounded.ArrowBack, onBack, size = 40.dp, glyph = 20.dp, glass = true)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(cat.title, color = c.text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("Genre", color = c.textSecondary, fontSize = 13.sp)
            }
        }
        if (loading && stations.isEmpty()) {
            Box(Modifier.fillMaxSize(), Alignment.Center) { Text("Loading ${cat.title}…", color = c.textSecondary, fontSize = 15.sp) }
        } else if (songs.isEmpty() && stations.isEmpty()) {
            Box(Modifier.fillMaxSize(), Alignment.Center) { Text("Couldn't load ${cat.title}. Check your connection.", color = c.textSecondary, fontSize = 15.sp) }
        } else LazyColumn(contentPadding = PaddingValues(top = 8.dp, bottom = bottomInset), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (songs.isNotEmpty()) item { Shelf("Songs", songs, onPlay) }
            if (stations.isNotEmpty()) {
                item { Text("Live radio stations", color = c.text, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)) }
                items(stations, key = { "live_${it.id}" }) { s ->
                    StationRow(s, current?.path == s.streamUrl, s.id in favStations, { onPlay(s.toSong(), emptyList()) }, { onToggleStation(s.id) }, { onStartRadio(s.toSong()) })
                }
            }
        }
    }
}

/** Coloured genre tile: artwork on the right, title bottom-left, heart top-left (like the headunit tile). */
@Composable
private fun CategoryTile(cat: DesktopRadioCategory, index: Int, isFav: Boolean, onFav: () -> Unit, onClick: () -> Unit) {
    var bmp by remember(cat.id) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(cat.id) { if (cat.imageUrl.isNotBlank()) bmp = DesktopImageCache.load(cat.imageUrl) }
    val base = Color(cat.stripeColor)
    val bg = when {
        base.luminance() > 0.60f -> base.copy(red = base.red * 0.45f, green = base.green * 0.45f, blue = base.blue * 0.45f)
        base.luminance() > 0.40f -> base.copy(red = base.red * 0.65f, green = base.green * 0.65f, blue = base.blue * 0.65f)
        else -> base
    }
    Box(Modifier.fillMaxWidth().height(88.dp).clip(TileShape).background(bg).clickable(onClick = onClick)) {
        bmp?.let { Image(it, null, contentScale = ContentScale.Crop, modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(78.dp).clip(RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp))) }
        Text(cat.title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 12.dp, bottom = 10.dp, end = 86.dp))
        DeskGlyph(if (isFav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, if (isFav) FavRed else Color.White, 18.dp,
            modifier = Modifier.align(Alignment.TopStart).clip(TileShape).clickable(onClick = onFav).padding(10.dp))
    }
}

/** Station card: art, name, "genre • bitrate", then start-radio, favourite and play actions (headunit CarStationRow). */
@Composable
private fun StationRow(s: DesktopRadioStation, isPlaying: Boolean, isFav: Boolean, onPlay: () -> Unit, onFav: () -> Unit, onStartRadio: () -> Unit) {
    val c = Desk.c
    var bmp by remember(s.id) { mutableStateOf<ImageBitmap?>(null) }
    val art = remember(s.id) { s.toSong().artworkUrl }
    LaunchedEffect(s.id) { if (!art.isNullOrBlank()) bmp = DesktopImageCache.load(art) }
    Row(
        Modifier.fillMaxWidth().height(64.dp).clip(DeskCard).background(if (isPlaying) c.selectedFill else c.idleFill).clickable(onClick = onPlay).padding(start = 10.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(44.dp).clip(DeskArt8).background(c.idleFill), Alignment.Center) {
            val b = bmp
            if (b != null) Image(b, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) else DeskGlyph(Icons.Rounded.Radio, c.textSecondary, 22.dp)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(s.name, color = if (isPlaying) c.accent else c.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${s.genre} • ${s.bitrate}", color = c.textSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        DeskIconButton(Icons.Rounded.Podcasts, onStartRadio, size = 40.dp, glyph = 20.dp, tint = c.textSecondary)
        DeskIconButton(if (isFav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, onFav, size = 40.dp, glyph = 20.dp, tint = if (isFav) FavRed else c.textTertiary)
        DeskIconButton(Icons.Rounded.PlayArrow, onPlay, size = 40.dp, glyph = 24.dp, tint = c.accent)
    }
}
