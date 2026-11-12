package com.aashik.music.desktop.ui.shell

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.format.DateTimeFormatter

internal enum class DeskDest { SONGS, FAVORITES, FOLDERS, RADIO, EQUALIZER, SETTINGS }

/** Solid sidebar: clock, library destinations, then Equalizer / Settings (same order as the headunit). */
@Composable
internal fun DeskSidebar(
    selected: DeskDest,
    isSearchOpen: Boolean,
    isPlaying: Boolean,
    favoriteCount: Int,
    onSearch: () -> Unit,
    onSelect: (DeskDest) -> Unit,
    modifier: Modifier = Modifier
) {
    val c = Desk.c
    Column(modifier.background(c.panel).padding(horizontal = 12.dp, vertical = 12.dp)) {
        Row(Modifier.fillMaxWidth().height(56.dp).padding(start = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            DeskClock(isPlaying)
        }
        Spacer(Modifier.height(12.dp))
        Item(Icons.Rounded.Search, "Search", isSearchOpen, null, onSearch)
        Item(Icons.Rounded.MusicNote, "Songs", !isSearchOpen && selected == DeskDest.SONGS, null) { onSelect(DeskDest.SONGS) }
        Item(Icons.Rounded.Favorite, "Favorites", !isSearchOpen && selected == DeskDest.FAVORITES, favoriteCount) { onSelect(DeskDest.FAVORITES) }
        Item(Icons.Rounded.Folder, "Folders", !isSearchOpen && selected == DeskDest.FOLDERS, null) { onSelect(DeskDest.FOLDERS) }
        Item(Icons.Rounded.Radio, "Radio & Genres", !isSearchOpen && selected == DeskDest.RADIO, null) { onSelect(DeskDest.RADIO) }
        Spacer(Modifier.weight(1f))
        Box(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp).height(0.5.dp).background(c.hairline))
        Item(Icons.Rounded.GraphicEq, "Equalizer", selected == DeskDest.EQUALIZER, null) { onSelect(DeskDest.EQUALIZER) }
        Item(Icons.Rounded.Settings, "Settings", selected == DeskDest.SETTINGS, null) { onSelect(DeskDest.SETTINGS) }
    }
}

@Composable
private fun Item(icon: ImageVector, label: String, selected: Boolean, count: Int?, onClick: () -> Unit) {
    val c = Desk.c
    val fill by animateColorAsState(if (selected) c.selectedFill else Color.Transparent, tween(200))
    val tint = if (selected) c.accent else c.text
    Row(
        Modifier.fillMaxWidth().height(52.dp).clip(DeskItem).background(fill).clickable(onClick = onClick).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DeskGlyph(icon, tint, 22.dp)
        Spacer(Modifier.width(14.dp))
        Text(label, color = tint, fontSize = 16.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium, maxLines = 1, modifier = Modifier.weight(1f))
        if (count != null && count > 0) Text("$count", color = if (selected) c.accent else c.textTertiary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun DeskClock(isPlaying: Boolean) {
    val c = Desk.c
    var now by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) { while (true) { now = LocalTime.now(); delay(15_000) } }
    val fmt = remember { DateTimeFormatter.ofPattern("h:mm") }
    val ampm = remember { DateTimeFormatter.ofPattern("a") }
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 22.sp)) { append(now.format(fmt)) }
            append(" ")
            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = c.textSecondary)) { append(now.format(ampm).uppercase()) }
        },
        color = c.text, maxLines = 1
    )
    Spacer(Modifier.width(8.dp))
    Box(Modifier.size(7.dp).background(if (isPlaying) Color(0xFF30D158) else c.textTertiary, DeskCircle))
}
