package com.aashik.music.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aashik.music.desktop.audio.DesktopAudioPlayer
import com.aashik.music.desktop.model.DesktopRadioStation
import com.aashik.music.desktop.model.DesktopSong
import com.aashik.music.desktop.service.DesktopLocalScanner
import com.aashik.music.desktop.service.DesktopLyricLine
import com.aashik.music.desktop.service.DesktopLyricsRepository
import com.aashik.music.desktop.service.DesktopOnlineRepository
import com.aashik.music.desktop.ui.components.DesktopActionBar
import com.aashik.music.desktop.ui.components.DesktopQueueDrawer
import com.aashik.music.desktop.ui.components.DesktopSearchDialog
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 1:1 Apple Music macOS Native Shell (Landscape Tablet / Desktop Architecture).
 * Implemented using Liquid Glass morphing fluid canvas, segmented top pill bar,
 * collapsible sidebar navigation, and elevated floating pill player dock.
 */
@Composable
fun DesktopMainView() {
    val scope = rememberCoroutineScope()

    var songs by remember { mutableStateOf<List<DesktopSong>>(emptyList()) }
    var currentSong by remember { mutableStateOf<DesktopSong?>(null) }
    var queue by remember { mutableStateOf<List<DesktopSong>>(emptyList()) }
    var lyrics by remember { mutableStateOf<List<DesktopLyricLine>>(emptyList()) }

    var favoriteIds by remember { mutableStateOf(mutableSetOf<String>()) }
    var favoriteStationIds by remember { mutableStateOf(mutableSetOf<String>()) }

    // Screen State & History
    var currentScreen by remember { mutableStateOf(DesktopScreen.NOW_PLAYING) }
    var screenHistory by remember { mutableStateOf(listOf(DesktopScreen.NOW_PLAYING)) }

    // Overlays
    var isQueueDrawerOpen by remember { mutableStateOf(false) }
    var isSearchDialogOpen by remember { mutableStateOf(false) }
    var isLyricsOpen by remember { mutableStateOf(false) }

    // Top Bar Search & Segmented Tabs
    var searchQuery by remember { mutableStateOf("") }
    var selectedTopTab by remember { mutableStateOf(0) } // 0: Home, 1: New, 2: Radio, 3: Songs

    // Radio State
    var isRadioMode by remember { mutableStateOf(false) }
    var isRadioLoading by remember { mutableStateOf(false) }
    var radioSeed by remember { mutableStateOf<DesktopSong?>(null) }

    // Player Play Function
    val playSong: (DesktopSong, List<DesktopSong>?) -> Unit = { song, customQueue ->
        currentSong = song
        if (customQueue != null) {
            queue = customQueue.filter { it.id != song.id }
        }
        DesktopAudioPlayer.play(song)
        scope.launch {
            lyrics = DesktopLyricsRepository.fetchLyrics(song)
        }
    }

    val playNext: () -> Unit = {
        if (queue.isNotEmpty()) {
            val nextSong = queue.first()
            val remaining = queue.drop(1)
            playSong(nextSong, remaining)
        }
    }

    val playPrevious: () -> Unit = {
        if (currentSong != null) {
            DesktopAudioPlayer.seekTo(0)
        }
    }

    var isShuffleOn by remember { mutableStateOf(false) }
    var isRepeatOn by remember { mutableStateOf(false) }

    val startRadioForSong: (DesktopSong) -> Unit = { seed ->
        isRadioMode = true
        isRadioLoading = true
        radioSeed = seed
        scope.launch {
            val radioTracks = DesktopOnlineRepository.fetchRadioQueue(seedSong = seed, limit = 20)
            isRadioLoading = false
            if (radioTracks.isNotEmpty()) {
                val firstTrack = radioTracks.first()
                val remaining = radioTracks.drop(1)
                playSong(firstTrack, remaining)
            }
        }
    }

    // Auto next track on finish
    LaunchedEffect(Unit) {
        DesktopAudioPlayer.setOnTrackFinished {
            playNext()
        }
    }

    // Initial Load: Scan local music and load library
    LaunchedEffect(Unit) {
        val local = DesktopLocalScanner.scanLocalMusic()
        if (local.isNotEmpty()) {
            songs = local
            currentSong = local.first()
            queue = local.drop(1)
        } else {
            val onlineHits = DesktopOnlineRepository.searchSongs("Top Hits 2024", limit = 25)
            songs = onlineHits
            if (onlineHits.isNotEmpty()) {
                currentSong = onlineHits.first()
                queue = onlineHits.drop(1)
            }
        }
        currentSong?.let {
            lyrics = DesktopLyricsRepository.fetchLyrics(it)
        }
    }

    val folderCount = remember(songs) {
        songs.map { File(it.path).parent ?: "" }.distinct().size
    }

    val filteredSongs = remember(songs, searchQuery) {
        if (searchQuery.isBlank()) songs
        else songs.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
            it.artist.contains(searchQuery, ignoreCase = true) ||
            it.album.contains(searchQuery, ignoreCase = true)
        }
    }

    fun navigateTo(screen: DesktopScreen) {
        if (currentScreen != screen) {
            screenHistory = screenHistory + screen
            currentScreen = screen
        }
    }

    LiquidGlassContainer(
        blurRadius = 30f,
        chromaticAberration = 1.2f
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ─── UPPER BODY: SIDEBAR + MAIN CONTENT CANVAS + RIGHT DRAWER ─────────
            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                // 1. Apple Music Frosted Liquid Glass Sidebar
                DesktopSidebar(
                    currentScreen = currentScreen,
                    onSelectScreen = { navigateTo(it) },
                    songCount = songs.size,
                    favoriteCount = favoriteIds.size,
                    folderCount = folderCount,
                    onSearchClick = { isSearchDialogOpen = true }
                )

                // 2. Main Content Canvas
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    // ─── TOP APP BAR (from Home.png & Album.png) ─────────────────────
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .padding(horizontal = 24.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Left: Back & Forward History Arrows
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(GlassSurface)
                                    .border(GlassBorderSubtle, RoundedCornerShape(8.dp))
                                    .clickable {
                                        if (screenHistory.size > 1) {
                                            val prev = screenHistory[screenHistory.size - 2]
                                            screenHistory = screenHistory.dropLast(1)
                                            currentScreen = prev
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ChevronLeft,
                                    contentDescription = "Back",
                                    tint = if (screenHistory.size > 1) DarkTextPrimary else DarkTextTertiary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(GlassSurface)
                                    .border(GlassBorderSubtle, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ChevronRight,
                                    contentDescription = "Forward",
                                    tint = DarkTextTertiary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Center: Segmented Pill Tab Bar [ Home ] [ New ] [ Radio ] [ Songs ] 🔍
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(GlassSurface)
                                .border(GlassBorderSpecular, RoundedCornerShape(20.dp))
                                .padding(horizontal = 4.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val tabs = listOf("Home", "New", "Radio", "Songs")
                            tabs.forEachIndexed { idx, tabTitle ->
                                val isSelected = selectedTopTab == idx
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (isSelected) DarkAccent.copy(alpha = 0.18f) else Color.Transparent)
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) DarkAccent.copy(alpha = 0.5f) else Color.Transparent,
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                        .clickable {
                                            selectedTopTab = idx
                                            when (idx) {
                                                0 -> navigateTo(DesktopScreen.LIBRARY)
                                                1 -> navigateTo(DesktopScreen.EXPLORE)
                                                2 -> navigateTo(DesktopScreen.EXPLORE)
                                                3 -> navigateTo(DesktopScreen.LIBRARY)
                                            }
                                        }
                                        .padding(horizontal = 14.dp, vertical = 5.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = tabTitle,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) DarkTextPrimary else DarkTextSecondary
                                    )
                                }
                            }

                            // Search trigger icon inside the segmented bar
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .clickable { isSearchDialogOpen = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Search,
                                    contentDescription = "Search",
                                    tint = DarkTextSecondary,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }

                        // Right: User Profile Avatar 'JS' (from Home.png)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            DesktopClock(textColor = DarkTextSecondary)

                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(GlassSurfaceVariant)
                                    .border(GlassBorderSubtle, CircleShape)
                                    .clickable { navigateTo(DesktopScreen.SETTINGS) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("JS", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = DarkTextPrimary)
                            }
                        }
                    }

                    Divider(color = DarkBorderSubtle, thickness = 0.8.dp)

                    // ─── MAIN CONTENT VIEW SWITCHER ──────────────────────────────────
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        when (currentScreen) {
                            DesktopScreen.NOW_PLAYING -> {
                                DesktopNowPlayingStage(
                                    currentSong = currentSong,
                                    lyrics = lyrics,
                                    queue = queue,
                                    isFavorite = currentSong?.let { favoriteIds.contains(it.id) } == true,
                                    onToggleFavorite = {
                                        currentSong?.let {
                                            if (favoriteIds.contains(it.id)) favoriteIds.remove(it.id)
                                            else favoriteIds.add(it.id)
                                        }
                                    },
                                    isRadioMode = isRadioMode,
                                    isRadioLoading = isRadioLoading,
                                    onStartRadio = {
                                        val seed = currentSong
                                        if (seed != null) {
                                            startRadioForSong(seed)
                                        }
                                    },
                                    onExitRadio = {
                                        isRadioMode = false
                                        radioSeed = null
                                    },
                                    onPlaySong = { s -> playSong(s, queue) },
                                    onRemoveFromQueue = { target ->
                                        queue = queue.filter { it.id != target.id }
                                    },
                                    onClearQueue = { queue = emptyList() },
                                    onPrevious = playPrevious,
                                    onNext = playNext,
                                    isShuffleOn = isShuffleOn,
                                    onToggleShuffle = { isShuffleOn = !isShuffleOn },
                                    isRepeatOn = isRepeatOn,
                                    onToggleRepeat = { isRepeatOn = !isRepeatOn }
                                )
                            }

                            DesktopScreen.LIBRARY -> {
                                DesktopLibraryTable(
                                    songs = filteredSongs,
                                    currentSong = currentSong,
                                    favoriteIds = favoriteIds.toSet(),
                                    onToggleFavorite = { song ->
                                        if (favoriteIds.contains(song.id)) favoriteIds.remove(song.id)
                                        else favoriteIds.add(song.id)
                                    },
                                    onPlaySong = { s, q -> playSong(s, q) }
                                )
                            }

                            DesktopScreen.FOLDERS -> {
                                DesktopFolderBrowser(
                                    songs = songs,
                                    onPlaySong = { s, q -> playSong(s, q) }
                                )
                            }

                            DesktopScreen.FAVORITES -> {
                                DesktopLibraryTable(
                                    songs = songs.filter { favoriteIds.contains(it.id) },
                                    currentSong = currentSong,
                                    favoriteIds = favoriteIds.toSet(),
                                    onToggleFavorite = { song ->
                                        if (favoriteIds.contains(song.id)) favoriteIds.remove(song.id)
                                        else favoriteIds.add(song.id)
                                    },
                                    onPlaySong = { s, q -> playSong(s, q) }
                                )
                            }

                            DesktopScreen.EXPLORE -> {
                                DesktopExploreView(
                                    currentSong = currentSong,
                                    favoriteStationIds = favoriteStationIds.toSet(),
                                    onToggleStationFavorite = { id ->
                                        if (favoriteStationIds.contains(id)) favoriteStationIds.remove(id)
                                        else favoriteStationIds.add(id)
                                    },
                                    onPlaySong = { s, q -> playSong(s, q) },
                                    onStartRadioForSong = { s -> startRadioForSong(s) }
                                )
                            }

                            DesktopScreen.EQUALIZER -> {
                                DesktopEqualizerView()
                            }

                            DesktopScreen.SETTINGS -> {
                                DesktopSettingsView(
                                    onStorageDirChanged = { dir ->
                                        DesktopLocalScanner.saveStorageDir(dir)
                                        scope.launch {
                                            songs = DesktopLocalScanner.scanLocalMusic(dir)
                                        }
                                    }
                                )
                            }

                            else -> {
                                DesktopLibraryTable(
                                    songs = filteredSongs,
                                    currentSong = currentSong,
                                    favoriteIds = favoriteIds.toSet(),
                                    onToggleFavorite = { song ->
                                        if (favoriteIds.contains(song.id)) favoriteIds.remove(song.id)
                                        else favoriteIds.add(song.id)
                                    },
                                    onPlaySong = { s, q -> playSong(s, q) }
                                )
                            }
                        }
                    }
                }

                // 3. Right Upcoming Queue Drawer (Animated Glass Flyout)
                if (isQueueDrawerOpen) {
                    DesktopQueueDrawer(
                        queue = queue,
                        onPlayTrack = { song, index ->
                            playSong(song, queue.drop(index + 1))
                        },
                        onClose = { isQueueDrawerOpen = false }
                    )
                }
            }

            // ─── FLOATING LIQUID GLASS MINI PLAYER DOCK (from Album.png & Home.png) ─
            DesktopBottomPlayer(
                currentSong = currentSong,
                isFavorite = currentSong?.let { favoriteIds.contains(it.id) } == true,
                onToggleFavorite = {
                    currentSong?.let {
                        if (favoriteIds.contains(it.id)) favoriteIds.remove(it.id)
                        else favoriteIds.add(it.id)
                    }
                },
                onPrevious = playPrevious,
                onNext = playNext,
                isShuffleOn = isShuffleOn,
                onToggleShuffle = { isShuffleOn = !isShuffleOn },
                isRepeatOn = isRepeatOn,
                onToggleRepeat = { isRepeatOn = !isRepeatOn },
                isQueueOpen = isQueueDrawerOpen,
                onToggleQueue = { isQueueDrawerOpen = !isQueueDrawerOpen },
                isLyricsOpen = isLyricsOpen,
                onToggleLyrics = {
                    isLyricsOpen = !isLyricsOpen
                    if (isLyricsOpen) navigateTo(DesktopScreen.NOW_PLAYING)
                },
                onOpenNowPlaying = { navigateTo(DesktopScreen.NOW_PLAYING) }
            )
        }

        // Search Modal Dialog (Glass Overlay)
        if (isSearchDialogOpen) {
            DesktopSearchDialog(
                onClose = { isSearchDialogOpen = false },
                onPlaySong = { s -> playSong(s, null) }
            )
        }
    }
}

@Composable
private fun DesktopClock(textColor: Color = DarkTextSecondary) {
    var timeString by remember { mutableStateOf("") }
    val formatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    LaunchedEffect(Unit) {
        while (true) {
            timeString = formatter.format(Date())
            kotlinx.coroutines.delay(1000)
        }
    }

    Text(
        text = timeString,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = textColor
    )
}
