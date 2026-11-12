package com.aashik.music.ui.car.glass

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import com.aashik.music.ui.designsystem.Text
import com.aashik.music.ui.mobile.glass.MobileTheme
import com.aashik.music.utils.getCurrentTimeWithAmPm
import com.aashik.music.viewmodel.LibraryTab
import com.aashik.music.viewmodel.MusicViewModel
import kotlinx.coroutines.delay

private val PlayingDotGreen = Color(0xFF30D158)

/**
 * Solid sidebar (tablet reference, AGENTS.md §7: no glass on navigation).
 * Clock + shortcuts on top, primary library destinations, then Drive HUD / Equalizer / Settings.
 */
@Composable
internal fun CarSidebar(
    viewModel: MusicViewModel,
    selectedTab: LibraryTab,
    isSearchOpen: Boolean,
    onOpenSearch: () -> Unit,
    onSelectTab: (LibraryTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val folders by viewModel.folders.collectAsState()
    val favoritePaths by viewModel.favoritePaths.collectAsState()
    val radioStations by viewModel.radioStations.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    // MORE is a portrait-only legacy tab; on the headunit it shows the full song list.
    val activeTab = if (selectedTab == LibraryTab.MORE) LibraryTab.ALL_SONGS else selectedTab

    Column(
        modifier = modifier
            .background(MobileTheme.panel)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        // Header: just the clock (Equalizer and Settings are in the list below)
        Row(
            modifier = Modifier.fillMaxWidth().height(CarDims.Target).padding(start = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CarClock(isPlaying = isPlaying, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))

        SidebarItem(Icons.Rounded.Search, "Search", selected = isSearchOpen, onClick = onOpenSearch)
        SidebarItem(Icons.Rounded.MusicNote, "Songs", selected = !isSearchOpen && activeTab == LibraryTab.ALL_SONGS) { onSelectTab(LibraryTab.ALL_SONGS) }
        SidebarItem(Icons.Rounded.Favorite, "Favorites", count = favoritePaths.size, selected = !isSearchOpen && activeTab == LibraryTab.FAVORITES) { onSelectTab(LibraryTab.FAVORITES) }
        SidebarItem(Icons.Rounded.Folder, "Folders", count = folders.size, selected = !isSearchOpen && activeTab == LibraryTab.FOLDERS) { onSelectTab(LibraryTab.FOLDERS) }
        SidebarItem(Icons.Rounded.Radio, "Radio & Genres", count = radioStations.size, selected = !isSearchOpen && activeTab == LibraryTab.RADIO) { onSelectTab(LibraryTab.RADIO) }

        Spacer(Modifier.weight(1f))
        Box(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp).height(0.5.dp).background(carHairline))

        SidebarItem(Icons.Rounded.DirectionsCar, "Drive HUD", selected = false) { viewModel.openDriveMode() }
        SidebarItem(Icons.Rounded.GraphicEq, "Equalizer", selected = false) { viewModel.openEqualizer() }
        SidebarItem(Icons.Rounded.Settings, "Settings", selected = false) { viewModel.openSettings() }
    }
}

@Composable
private fun SidebarItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    count: Int? = null,
    onClick: () -> Unit
) {
    val fill by animateColorAsState(if (selected) carSelectedFill else Color.Transparent, tween(220), label = "sideFill")
    val tint = if (selected) MobileTheme.accent else MobileTheme.text
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(CarDims.SidebarRowHeight)
            .clip(CarSidebarItemShape) // also bounds the press ripple
            .background(fill)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CarGlyph(icon, null, tint, 24.dp)
        Spacer(Modifier.width(14.dp))
        Text(
            text = label,
            color = tint,
            fontSize = 17.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )
        if (count != null && count > 0) {
            Text(text = "$count", color = if (selected) MobileTheme.accent else MobileTheme.textTertiary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** Leaf 12-hour clock, refreshed every 15 s; the dot turns green while music plays. */
@Composable
internal fun CarClock(isPlaying: Boolean, modifier: Modifier = Modifier, fontSize: Float = 22f) {
    var now by remember { mutableStateOf(getCurrentTimeWithAmPm()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = getCurrentTimeWithAmPm()
            delay(15_000L)
        }
    }
    val digits = now.substringBefore(' ')
    val amPm = now.substringAfter(' ', "")
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = fontSize.sp, letterSpacing = (-0.3).sp)) { append(digits) }
                if (amPm.isNotEmpty()) {
                    append(" ")
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, fontSize = (fontSize * 0.55f).sp, color = MobileTheme.textSecondary)) { append(amPm) }
                }
            },
            color = MobileTheme.text,
            maxLines = 1
        )
        Spacer(Modifier.width(8.dp))
        Box(Modifier.size(7.dp).background(if (isPlaying) PlayingDotGreen else MobileTheme.textTertiary, CarCircle))
    }
}
