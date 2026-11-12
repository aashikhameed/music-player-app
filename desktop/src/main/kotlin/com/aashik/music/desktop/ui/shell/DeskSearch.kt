package com.aashik.music.desktop.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Search
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aashik.music.desktop.model.DesktopSong

internal enum class SearchScope(val label: String) { LIBRARY("Library"), ONLINE("Online") }

private val FieldShape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
private val TileShape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
internal val SearchTopBar = 64.dp

/** Search page: field + scope switch at the top, then Top Results, Artists and Albums for the query. */
@Composable
internal fun DeskSearchPage(
    query: String,
    onQuery: (String) -> Unit,
    scope: SearchScope,
    onScope: (SearchScope) -> Unit,
    focused: Boolean,
    onFocus: (Boolean) -> Unit,
    onSubmit: () -> Unit,
    onEscape: () -> Unit,
    results: List<DesktopSong>,
    loading: Boolean,
    bottomInset: androidx.compose.ui.unit.Dp,
    onPlay: (DesktopSong, List<DesktopSong>) -> Unit,
    onMore: (DesktopSong) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenAlbum: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val c = Desk.c
    val focus = remember { FocusRequester() }
    val artists = remember(results) { results.groupBy { it.artist.substringBefore(",").substringBefore(" & ").trim().ifBlank { "Unknown Artist" } }.entries.take(12) }
    val albums = remember(results) { results.filter { it.album.isNotBlank() && it.album != "Unknown Album" }.distinctBy { it.album }.take(12) }
    LazyColumn(modifier, contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = bottomInset)) {
        item {
            Row(Modifier.fillMaxWidth().height(SearchTopBar - 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.weight(0.15f))
                Row(
                    Modifier.weight(1f).height(36.dp)
                        .background(c.accent.copy(alpha = if (focused) 0.55f else 0f), FieldShape)
                        .padding(if (focused) 2.dp else 0.dp)
                        .deskLiquid(16.dp)
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DeskGlyph(Icons.Rounded.Search, c.textSecondary, 20.dp)
                    Spacer(Modifier.width(10.dp))
                    Box(Modifier.weight(1f)) {
                        if (query.isEmpty()) Text("Artists, Songs, Lyrics, and More", color = c.textSecondary, fontSize = 14.sp, maxLines = 1)
                        BasicTextField(
                            query, onQuery, singleLine = true,
                            textStyle = TextStyle(color = c.text, fontSize = 14.sp),
                            cursorBrush = SolidColor(c.accent),
                            modifier = Modifier.fillMaxWidth().focusRequester(focus).onFocusChanged { onFocus(it.isFocused) }
                                .onPreviewKeyEvent { e ->
                                    if (e.type != KeyEventType.KeyDown) false
                                    else when (e.key) { Key.Escape -> { onEscape(); true }; Key.Enter -> { onSubmit(); true }; else -> false }
                                }
                        )
                    }
                    LaunchedEffect(Unit) { kotlinx.coroutines.delay(50); runCatching { focus.requestFocus() } }
                    if (query.isNotEmpty()) DeskIconButton(Icons.Rounded.Cancel, { onQuery("") }, size = 26.dp, glyph = 18.dp, tint = c.textSecondary)
                }
                Spacer(Modifier.width(14.dp))
                ScopeSwitch(scope, onScope)
            }
        }
        if (query.isBlank()) {
            item { Box(Modifier.fillMaxWidth().padding(top = 120.dp), Alignment.Center) { Text("Search your library or find new music online.", color = c.textSecondary, fontSize = 16.sp) } }
            return@LazyColumn
        }
        if (results.isEmpty()) {
            item { Box(Modifier.fillMaxWidth().padding(top = 120.dp), Alignment.Center) { Text(if (loading) "Searching…" else "No results for “$query”.", color = c.textSecondary, fontSize = 16.sp) } }
            return@LazyColumn
        }
        item { SectionHeader("Top Results", false) }
        items(results.take(6).chunked(3)) { row ->
            Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { s -> TopResultCard(s, Modifier.weight(1f), onClick = { onPlay(s, results) }, onMore = { onMore(s) }) }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        if (artists.isNotEmpty()) {
            item { SectionHeader("Artists", true) }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(artists, key = { it.key }) { (name, songs) ->
                        Column(Modifier.width(120.dp).clickable { onOpenArtist(name) }, horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(Modifier.size(120.dp)) { DeskArtistFill(songs.firstNotNullOfOrNull { it.artistImage }) }
                            Spacer(Modifier.height(10.dp))
                            Text(name, color = c.text, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
        if (albums.isNotEmpty()) {
            item { SectionHeader("Albums", true) }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(albums, key = { it.id }) { s ->
                        Column(Modifier.width(130.dp).clickable { onOpenAlbum(s.album, s.artist) }) {
                            Box(Modifier.size(130.dp)) { DeskArtFill(s, TileShape) }
                            Spacer(Modifier.height(8.dp))
                            Text(s.album, color = c.text, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(s.artist, color = c.textSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

private fun Color.compositeOverBg(bg: Color): Color {
    val a = alpha
    return Color(red * a + bg.red * (1 - a), green * a + bg.green * (1 - a), blue * a + bg.blue * (1 - a), 1f)
}

@Composable
private fun SectionHeader(title: String, chevron: Boolean) {
    val c = Desk.c
    Row(Modifier.padding(top = 14.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = c.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        if (chevron) DeskGlyph(Icons.Rounded.ChevronRight, c.textSecondary, 24.dp)
    }
}

@Composable
private fun ScopeSwitch(scope: SearchScope, onScope: (SearchScope) -> Unit) {
    val c = Desk.c
    Row(Modifier.height(36.dp).deskLiquid(18.dp).padding(3.dp), verticalAlignment = Alignment.CenterVertically) {
        SearchScope.values().forEach { s ->
            val on = s == scope
            Box(
                Modifier.height(30.dp).clip(DeskPill).background(if (on) c.text.copy(alpha = 0.14f) else Color.Transparent).clickable { onScope(s) }.padding(horizontal = 16.dp),
                Alignment.Center
            ) { Text(s.label, color = c.text, fontSize = 13.sp, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal) }
        }
    }
}

@Composable
private fun TopResultCard(s: DesktopSong, modifier: Modifier, onClick: () -> Unit, onMore: () -> Unit) {
    val c = Desk.c
    Row(Modifier.then(modifier).height(68.dp).deskSurface(12.dp).clickable(onClick = onClick).padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
        DeskArt(s, 46.dp, DeskArt8)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(s.title.ifBlank { "Untitled" }, color = c.text, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("Song · ${s.artist}", color = c.textSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        DeskIconButton(Icons.Rounded.MoreHoriz, onMore, size = 32.dp, glyph = 20.dp, tint = c.text)
    }
}
