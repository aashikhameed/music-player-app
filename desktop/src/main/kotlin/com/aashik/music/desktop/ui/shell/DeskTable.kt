package com.aashik.music.desktop.ui.shell

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIos
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VolumeMute
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aashik.music.desktop.model.DesktopSong
import com.aashik.music.desktop.ui.DesktopImageCache

/** Small artwork tile that loads lazily; falls back to a note glyph. */
@Composable
internal fun DeskArt(song: DesktopSong?, size: Dp, shape: androidx.compose.ui.graphics.Shape = DeskArt8, modifier: Modifier = Modifier) {
    val c = Desk.c
    var bmp by remember(song?.id) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(song?.id) { bmp = song?.let { DesktopImageCache.load(it.artworkUrl ?: it.path) } }
    Box(modifier.size(size).clip(shape).background(c.idleFill), contentAlignment = Alignment.Center) {
        val b = bmp
        if (b != null) Image(b, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        else DeskGlyph(Icons.Rounded.MusicNote, c.textTertiary, size / 2)
    }
}

internal enum class SortKey(val label: String) { TITLE("Title"), TIME("Time"), ARTIST("Artist"), ALBUM("Album"), PLAYS("Plays") }

/** Toolbar: view title, sort/filter button and the "Find in …" pill. */
@Composable
internal fun DeskToolbar(
    title: String,
    query: String,
    placeholder: String,
    sort: SortKey?,
    onSort: ((SortKey) -> Unit)?,
    onQuery: (String) -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null
) {
    val c = Desk.c
    var menu by remember { mutableStateOf(false) }
    Row(modifier.fillMaxWidth().height(60.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) { DeskIconButton(androidx.compose.material.icons.Icons.Rounded.ArrowBackIos, onBack, size = 36.dp, glyph = 18.dp, tint = c.accent, glass = true); Spacer(Modifier.width(4.dp)) }
        Text(title, color = c.text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (onSort != null) {
            Box {
                DeskIconButton(Icons.Rounded.Tune, { menu = !menu }, size = 40.dp, glyph = 20.dp, glass = true)
                androidx.compose.material3.DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    SortKey.values().forEach { k ->
                        androidx.compose.material3.DropdownMenuItem(text = { Text(k.label + if (k == sort) "  ✓" else "") }, onClick = { onSort(k); menu = false })
                    }
                }
            }
            Spacer(Modifier.width(10.dp))
        }
        Row(Modifier.width(300.dp).height(40.dp).deskLiquid(20.dp).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            DeskGlyph(Icons.Rounded.Search, c.textSecondary, 18.dp)
            Spacer(Modifier.width(8.dp))
            Box(Modifier.weight(1f)) {
                if (query.isEmpty()) Text(placeholder, color = c.textSecondary, fontSize = 15.sp, maxLines = 1)
                BasicTextField(query, onQuery, singleLine = true, textStyle = TextStyle(color = c.text, fontSize = 15.sp), cursorBrush = SolidColor(c.accent), modifier = Modifier.fillMaxWidth())
            }
            if (query.isNotEmpty()) DeskIconButton(Icons.Rounded.Close, { onQuery("") }, size = 24.dp, glyph = 14.dp, tint = c.textSecondary)
        }
    }
}

private val LeadW = 34.dp
private val TimeW = 64.dp
private val StarW = 44.dp
private val PlaysW = 70.dp
private val MoreW = 44.dp

/** Apple Music style table: Title ••• | Time | Artist | Album | Genre | ★ | Plays, striped rows. */
@Composable
internal fun DeskSongTable(
    songs: List<DesktopSong>,
    current: DesktopSong?,
    isPlaying: Boolean,
    plays: Map<String, Int>,
    favorites: Set<String>,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    bottomInset: Dp = 100.dp,
    sort: SortKey? = null,
    sortAsc: Boolean = true,
    onHeader: ((SortKey) -> Unit)? = null,
    onPlay: (DesktopSong) -> Unit,
    onMore: (DesktopSong) -> Unit,
    onToggleFavorite: (DesktopSong) -> Unit
) {
    val c = Desk.c
    Column(modifier) {
        Row(Modifier.fillMaxWidth().height(32.dp).padding(end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.width(LeadW))
            HeaderCell("Title", SortKey.TITLE, sort, sortAsc, onHeader, Modifier.weight(2.6f))
            Spacer(Modifier.width(MoreW))
            HeaderCell("Time", SortKey.TIME, sort, sortAsc, onHeader, Modifier.width(TimeW), TextAlign.End)
            Spacer(Modifier.width(16.dp))
            HeaderCell("Artist", SortKey.ARTIST, sort, sortAsc, onHeader, Modifier.weight(2f))
            HeaderCell("Album", SortKey.ALBUM, sort, sortAsc, onHeader, Modifier.weight(2f))
            HeaderCell("Genre", null, sort, sortAsc, null, Modifier.weight(1.2f))
            DeskGlyph(Icons.Rounded.StarBorder, c.textSecondary, 16.dp, Modifier.width(StarW))
            HeaderCell("Plays", SortKey.PLAYS, sort, sortAsc, onHeader, Modifier.width(PlaysW), TextAlign.End)
        }
        Box(Modifier.fillMaxWidth().height(0.5.dp).background(c.hairline))
        LazyColumn(state = state, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = bottomInset)) {
            itemsIndexed(songs, key = { i, s -> "$i-${s.id}" }) { i, s ->
                val isCur = s.id == current?.id
                val fg = if (isCur) c.onAccent else c.text
                Row(
                    Modifier.fillMaxWidth().height(34.dp)
                        .background(if (isCur) c.accent else if (i % 2 == 1) c.text.copy(alpha = 0.04f) else Color.Transparent)
                        .clickable { onPlay(s) }.padding(end = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.width(LeadW), Alignment.Center) {
                        if (isCur) DeskGlyph(if (isPlaying) Icons.Rounded.VolumeUp else Icons.Rounded.VolumeMute, fg, 16.dp)
                    }
                    Text(s.title.ifBlank { "Untitled" }, color = fg, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(2.6f))
                    Box(Modifier.width(MoreW), Alignment.Center) {
                        DeskIconButton(Icons.Rounded.MoreHoriz, { onMore(s) }, size = 30.dp, glyph = 18.dp, tint = if (isCur) fg else c.accent)
                    }
                    Text(if (s.duration > 0) deskFormatTime(s.duration) else "", color = fg, fontSize = 14.sp, textAlign = TextAlign.End, modifier = Modifier.width(TimeW))
                    Spacer(Modifier.width(16.dp))
                    Text(s.artist, color = fg, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(2f))
                    Text(s.album.takeUnless { it == "Unknown Album" }.orEmpty(), color = fg, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(2f))
                    Text("", fontSize = 14.sp, modifier = Modifier.weight(1.2f))
                    Box(Modifier.width(StarW), Alignment.Center) {
                        val fav = favorites.contains(s.id)
                        DeskIconButton(if (fav) Icons.Rounded.Star else Icons.Rounded.StarBorder, { onToggleFavorite(s) }, size = 30.dp, glyph = 16.dp, tint = if (isCur) fg else if (fav) c.accent else c.textTertiary)
                    }
                    Text((plays[s.id] ?: 0).takeIf { it > 0 }?.toString().orEmpty(), color = fg, fontSize = 14.sp, textAlign = TextAlign.End, modifier = Modifier.width(PlaysW))
                }
            }
        }
    }
}

@Composable
private fun HeaderCell(label: String, key: SortKey?, sort: SortKey?, asc: Boolean, onHeader: ((SortKey) -> Unit)?, modifier: Modifier, align: TextAlign = TextAlign.Start) {
    val c = Desk.c
    Row(modifier.then(if (key != null && onHeader != null) Modifier.clickable { onHeader(key) } else Modifier), horizontalArrangement = if (align == TextAlign.End) androidx.compose.foundation.layout.Arrangement.End else androidx.compose.foundation.layout.Arrangement.Start, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = c.text, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1)
        if (key != null && key == sort) DeskGlyph(if (asc) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward, c.accent, 13.dp)
    }
}
