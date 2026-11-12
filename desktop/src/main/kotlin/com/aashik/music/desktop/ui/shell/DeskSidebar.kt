package com.aashik.music.desktop.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SettingsInputAntenna
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal enum class DeskDest { SEARCH, HOME, RADIO, RECENT, ARTISTS, ALBUMS, SONGS, EQUALIZER, PLAYLISTS, SETTINGS }

/** macOS Music style sidebar: Search / Home / Radio, Library, Audio, Playlists, profile row. */
@Composable
internal fun DeskSidebar(selected: DeskDest, userName: String, onSelect: (DeskDest) -> Unit, modifier: Modifier = Modifier) {
    val c = Desk.c
    Column(modifier.padding(8.dp).padding(horizontal = 6.dp, vertical = 6.dp)) {
        Spacer(Modifier.height(26.dp)) // room for the window controls (transparent title bar)
        Item(Icons.Rounded.Search, "Search", selected == DeskDest.SEARCH) { onSelect(DeskDest.SEARCH) }
        Item(Icons.Rounded.Home, "Home", selected == DeskDest.HOME) { onSelect(DeskDest.HOME) }
        Item(Icons.Rounded.SettingsInputAntenna, "Radio", selected == DeskDest.RADIO) { onSelect(DeskDest.RADIO) }
        Header("Library")
        Item(Icons.Rounded.Schedule, "Recently Added", selected == DeskDest.RECENT) { onSelect(DeskDest.RECENT) }
        Item(Icons.Rounded.Person, "Artists", selected == DeskDest.ARTISTS) { onSelect(DeskDest.ARTISTS) }
        Item(Icons.Rounded.Album, "Albums", selected == DeskDest.ALBUMS) { onSelect(DeskDest.ALBUMS) }
        Item(Icons.Rounded.MusicNote, "Songs", selected == DeskDest.SONGS) { onSelect(DeskDest.SONGS) }
        Header("Audio")
        Item(Icons.Rounded.GraphicEq, "Equalizer", selected == DeskDest.EQUALIZER) { onSelect(DeskDest.EQUALIZER) }
        Header("Playlists")
        Item(Icons.Rounded.GridView, "All Playlists", selected == DeskDest.PLAYLISTS) { onSelect(DeskDest.PLAYLISTS) }
        Spacer(Modifier.weight(1f))
        Row(
            Modifier.fillMaxWidth().height(36.dp).clip(DeskItem).background(if (selected == DeskDest.SETTINGS) c.text.copy(alpha = 0.10f) else Color.Transparent)
                .clickable { onSelect(DeskDest.SETTINGS) }.padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DeskGlyph(Icons.Rounded.Settings, c.accent, 20.dp)
            Spacer(Modifier.width(12.dp))
            Text("Settings", color = c.text, fontSize = 15.sp, fontWeight = if (selected == DeskDest.SETTINGS) FontWeight.SemiBold else FontWeight.Normal, maxLines = 1)
        }
    }
}

@Composable
private fun Header(t: String) {
    Text(t, color = Desk.c.textSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 12.dp, top = 20.dp, bottom = 6.dp))
}

@Composable
private fun Item(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    val c = Desk.c
    Row(
        Modifier.fillMaxWidth().height(36.dp).clip(DeskItem).background(if (selected) c.text.copy(alpha = 0.10f) else Color.Transparent).clickable(onClick = onClick).padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DeskGlyph(icon, c.accent, 20.dp)
        Spacer(Modifier.width(12.dp))
        Text(label, color = c.text, fontSize = 15.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, maxLines = 1)
    }
}
