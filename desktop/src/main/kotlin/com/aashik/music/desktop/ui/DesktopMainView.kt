package com.aashik.music.desktop.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Search
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
import com.aashik.music.desktop.model.DesktopSong
import com.aashik.music.desktop.service.DesktopLocalScanner
import com.aashik.music.desktop.service.DesktopLyricLine
import com.aashik.music.desktop.service.DesktopLyricsRepository
import com.aashik.music.desktop.service.DesktopOnlineRepository
import com.aashik.music.desktop.ui.components.DesktopClock
import com.aashik.music.desktop.ui.components.DesktopQueueDrawer
import com.aashik.music.desktop.ui.components.DesktopSearchDialog
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun DesktopMainView() {
    val scope = rememberCoroutineScope()

    var currentScreen by remember { mutableStateOf(DesktopScreen.NOW_PLAYING) }
    var screenHistory by remember { mutableStateOf(listOf(DesktopScreen.NOW_PLAYING)) }

    var songs by remember { mutableStateOf<List<DesktopSong>>(emptyList()) }
    var queue by remember { mutableStateOf<List<DesktopSong>>(emptyList()) }
    var currentSong by remember { mutableStateOf<DesktopSong?>(null) }
    var lyrics by remember { mutableStateOf<List<DesktopLyricLine>>(emptyList()) }
    val favoriteIds = remember { mutableStateListOf<String>() }
    val favoriteCategoryIds = remember { mutableStateListOf<String>() }
    val favoriteStationIds = remember { mutableStateListOf<String>() }

    var isQueueOpen by remember { mutableStateOf(false) }
    var isSearchOpen by remember { mutableStateOf(false) }
    var isShuffleOn by remember { mutableStateOf(false) }
    var isRepeatOn by remember { mutableStateOf(false) }

    var isRadioMode by remember { mutableStateOf(false) }
    var radioSeed by remember { mutableStateOf<DesktopSong?>(null) }
    var isRadioLoading by remember { mutableStateOf(false) }

    var searchQuery by remember { mutableStateOf("") }

    // Helper: switch screen and track navigation history
    fun navigateTo(screen: DesktopScreen) {
        if (currentScreen != screen) {
            screenHistory = screenHistory + screen
            currentScreen = screen
        }
    }

    // Play track helper
    fun playSong(song: DesktopSong, newQueue: List<DesktopSong>? = null) {
        currentSong = song
        if (newQueue != null) {
            queue = newQueue
        }
        DesktopAudioPlayer.play(song)
        scope.launch {
            lyrics = DesktopLyricsRepository.fetchLyrics(song)
        }
    }

    // Refill radio queue when running low
    fun checkAndRefillRadioQueue() {
        if (!isRadioMode) return
        val seed = radioSeed ?: currentSong ?: return
        scope.launch {
            val more = DesktopOnlineRepository.fetchRadioQueue(seed, limit = 15)
            val existingIds = queue.map { it.id }.toSet()
            val newItems = more.filter { it.id != currentSong?.id && !existingIds.contains(it.id) }
            if (newItems.isNotEmpty()) {
                queue = queue + newItems
            }
        }
    }

    // Play next track
    fun playNext() {
        if (queue.isNotEmpty()) {
            val next = if (isShuffleOn) queue.random() else queue.first()
            queue = queue.filter { it.id != next.id }
            playSong(next)
            if (isRadioMode && queue.size < 3) {
                checkAndRefillRadioQueue()
            }
        } else if (isRepeatOn && currentSong != null) {
            DesktopAudioPlayer.seekTo(0L)
            DesktopAudioPlayer.resume()
        } else if (isRadioMode) {
            checkAndRefillRadioQueue()
        }
    }

    // Start Continuous Algorithmic Radio for any song
    fun startRadioForSong(seed: DesktopSong) {
        isRadioMode = true
        radioSeed = seed
        isRadioLoading = true
        scope.launch {
            val rQueue = DesktopOnlineRepository.fetchRadioQueue(seed, limit = 20)
            isRadioLoading = false
            if (rQueue.isNotEmpty()) {
                if (currentSong?.id == seed.id) {
                    queue = rQueue
                } else {
                    playSong(seed, rQueue)
                }
            }
        }
    }

    // Auto next track on end
    LaunchedEffect(Unit) {
        DesktopAudioPlayer.setOnTrackFinished {
            playNext()
        }
    }

    // Initial load: scan local music or load popular hits
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppleMusicBg)
    ) {
        // ─── UPPER BODY: SIDEBAR + MAIN CONTENT + RIGHT DRAWER ─────────────
        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
            // 1. Apple Music Left Navigation Sidebar
            DesktopSidebar(
                currentScreen = currentScreen,
                onSelectScreen = { navigateTo(it) },
                songCount = songs.size,
                favoriteCount = favoriteIds.size,
                folderCount = folderCount
            )

            // 2. Main Content Canvas
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(AppleMusicBg)
            ) {
                // Apple Music Header Bar (Navigation Arrows + Search Box + System Clock)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left: macOS Navigation History Arrows
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(AppleMusicSurface)
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
                                tint = if (screenHistory.size > 1) AppleMusicTextPrimary else AppleMusicTextTertiary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(AppleMusicSurface),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ChevronRight,
                                contentDescription = "Forward",
                                tint = AppleMusicTextTertiary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Center: Apple Music Search Input (TEXT 100% VISIBLE WITH EXPLICIT STYLING)
                    Box(
                        modifier = Modifier
                            .width(340.dp)
                            .height(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(AppleMusicCard)
                            .border(1.dp, AppleMusicBorderSubtle, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = null,
                                tint = AppleMusicTextTertiary,
                                modifier = Modifier.size(16.dp)
                            )

                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "Search in library or explore...",
                                        fontSize = 13.sp,
                                        color = AppleMusicTextTertiary
                                    )
                                }
                                BasicTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    singleLine = true,
                                    textStyle = TextStyle(
                                        color = AppleMusicTextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Normal
                                    ),
                                    cursorBrush = SolidColor(AppleMusicRed),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                    keyboardActions = KeyboardActions(
                                        onSearch = {
                                            if (searchQuery.isNotBlank()) {
                                                scope.launch {
                                                    val results = DesktopOnlineRepository.searchSongs(searchQuery)
                                                    if (results.isNotEmpty()) {
                                                        songs = results
                                                        currentScreen = DesktopScreen.LIBRARY
                                                    }
                                                }
                                            }
                                        }
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            if (searchQuery.isNotEmpty()) {
                                Icon(
                                    imageVector = Icons.Rounded.Cancel,
                                    contentDescription = "Clear",
                                    tint = AppleMusicTextTertiary,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable { searchQuery = "" }
                                )
                            }
                        }
                    }

                    // Right: Apple Music System Clock
                    DesktopClock(textColor = AppleMusicTextSecondary)
                }

                Divider(color = AppleMusicBorderSubtle, thickness = 0.8.dp)

                // Screen Switching
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when (currentScreen) {
                        DesktopScreen.LIBRARY -> {
                            val filteredSongs = if (searchQuery.isBlank()) songs else {
                                songs.filter {
                                    it.title.contains(searchQuery, ignoreCase = true) ||
                                            it.artist.contains(searchQuery, ignoreCase = true) ||
                                            it.album.contains(searchQuery, ignoreCase = true)
                                }
                            }
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
                                onPlaySong = { s -> playSong(s) },
                                onRemoveFromQueue = { target ->
                                    queue = queue.filter { it.id != target.id }
                                },
                                onClearQueue = {
                                    queue = emptyList()
                                },
                                onPrevious = {
                                    val pos = DesktopAudioPlayer.currentPositionMs.value
                                    if (pos > 3000L) {
                                        DesktopAudioPlayer.seekTo(0L)
                                    } else {
                                        DesktopAudioPlayer.seekTo(0L)
                                    }
                                },
                                onNext = { playNext() },
                                isShuffleOn = isShuffleOn,
                                onToggleShuffle = { isShuffleOn = !isShuffleOn },
                                isRepeatOn = isRepeatOn,
                                onToggleRepeat = { isRepeatOn = !isRepeatOn }
                            )
                        }
                        DesktopScreen.FOLDERS -> {
                            DesktopFolderBrowser(
                                songs = songs,
                                onPlaySong = { s, q -> playSong(s, q) }
                            )
                        }
                        DesktopScreen.FAVORITES -> {
                            val favSongs = songs.filter { favoriteIds.contains(it.id) }
                            DesktopLibraryTable(
                                songs = favSongs,
                                currentSong = currentSong,
                                favoriteIds = favoriteIds.toSet(),
                                onToggleFavorite = { s -> favoriteIds.remove(s.id) },
                                onPlaySong = { s, q -> playSong(s, q) }
                            )
                        }
                        DesktopScreen.EXPLORE -> {
                            DesktopExploreView(
                                currentSong = currentSong,
                                favoriteCategoryIds = favoriteCategoryIds.toSet(),
                                onToggleCategoryFavorite = { id ->
                                    if (favoriteCategoryIds.contains(id)) favoriteCategoryIds.remove(id)
                                    else favoriteCategoryIds.add(id)
                                },
                                favoriteStationIds = favoriteStationIds.toSet(),
                                onToggleStationFavorite = { id ->
                                    if (favoriteStationIds.contains(id)) favoriteStationIds.remove(id)
                                    else favoriteStationIds.add(id)
                                },
                                onPlaySong = { s, q -> playSong(s, q) },
                                onStartRadioForSong = { seed -> startRadioForSong(seed) }
                            )
                        }
                        DesktopScreen.EQUALIZER -> {
                            DesktopEqualizerView()
                        }
                        DesktopScreen.SETTINGS -> {
                            DesktopSettingsView(
                                onStorageDirChanged = { newDir ->
                                    scope.launch {
                                        val rescanned = DesktopLocalScanner.scanLocalMusic(newDir)
                                        if (rescanned.isNotEmpty()) {
                                            songs = rescanned
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // 3. Slide-in Right Queue Drawer
            AnimatedVisibility(
                visible = isQueueOpen,
                enter = slideInHorizontally(initialOffsetX = { fullWidth -> fullWidth }),
                exit = slideOutHorizontally(targetOffsetX = { fullWidth -> fullWidth })
            ) {
                DesktopQueueDrawer(
                    queue = queue,
                    onPlayTrack = { song, index ->
                        val newQueue = queue.drop(index + 1)
                        playSong(song, newQueue)
                    },
                    onClose = { isQueueOpen = false }
                )
            }
        }

        // ─── APPLE MUSIC BOTTOM PLAYER DOCK (ONLY ON NON-NOW_PLAYING SCREENS) ─
        if (currentScreen != DesktopScreen.NOW_PLAYING) {
            Divider(color = AppleMusicBorderSubtle, thickness = 1.dp)
            DesktopBottomPlayer(
                currentSong = currentSong,
                isFavorite = currentSong?.let { favoriteIds.contains(it.id) } == true,
                onToggleFavorite = {
                    currentSong?.let {
                        if (favoriteIds.contains(it.id)) favoriteIds.remove(it.id)
                        else favoriteIds.add(it.id)
                    }
                },
                onPrevious = {
                    val pos = DesktopAudioPlayer.currentPositionMs.value
                    if (pos > 3000L) {
                        DesktopAudioPlayer.seekTo(0L)
                    } else {
                        DesktopAudioPlayer.seekTo(0L)
                    }
                },
                onNext = { playNext() },
                isShuffleOn = isShuffleOn,
                onToggleShuffle = { isShuffleOn = !isShuffleOn },
                isRepeatOn = isRepeatOn,
                onToggleRepeat = { isRepeatOn = !isRepeatOn },
                isQueueOpen = isQueueOpen,
                onToggleQueue = { isQueueOpen = !isQueueOpen },
                isLyricsOpen = false,
                onToggleLyrics = {
                    currentScreen = DesktopScreen.NOW_PLAYING
                },
                onOpenNowPlaying = {
                    currentScreen = DesktopScreen.NOW_PLAYING
                }
            )
        }

        // Modal Search Overlay if needed
        if (isSearchOpen) {
            DesktopSearchDialog(
                onPlaySong = { s -> playSong(s) },
                onClose = { isSearchOpen = false }
            )
        }
    }
}
