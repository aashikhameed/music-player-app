package com.aashik.music.desktop.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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

    var currentScreen by remember { mutableStateOf(DesktopScreen.LIBRARY) }
    var songs by remember { mutableStateOf<List<DesktopSong>>(emptyList()) }
    var queue by remember { mutableStateOf<List<DesktopSong>>(emptyList()) }
    var currentSong by remember { mutableStateOf<DesktopSong?>(null) }
    var lyrics by remember { mutableStateOf<List<DesktopLyricLine>>(emptyList()) }
    val favoriteIds = remember { mutableStateListOf<String>() }

    var isQueueOpen by remember { mutableStateOf(false) }
    var isSearchOpen by remember { mutableStateOf(false) }
    var isShuffleOn by remember { mutableStateOf(false) }
    var isRepeatOn by remember { mutableStateOf(false) }
    var isRadioLoading by remember { mutableStateOf(false) }

    var searchQuery by remember { mutableStateOf("") }

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

    // Play next track
    fun playNext() {
        if (queue.isNotEmpty()) {
            val next = if (isShuffleOn) queue.random() else queue.first()
            queue = queue.filter { it.id != next.id }
            playSong(next)
        } else if (isRepeatOn && currentSong != null) {
            DesktopAudioPlayer.seekTo(0L)
            DesktopAudioPlayer.resume()
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
            .background(Color(0xFF090C12))
    ) {
        // ─── DESKTOP UPPER BODY: SIDEBAR + MAIN CONTENT + RIGHT DRAWER ────────
        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
            // 1. Fixed Left Navigation Sidebar
            DesktopSidebar(
                currentScreen = currentScreen,
                onSelectScreen = { currentScreen = it },
                songCount = songs.size,
                favoriteCount = favoriteIds.size,
                folderCount = folderCount
            )

            // 2. Main Content Stage
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(Color(0xFF0C0E14))
            ) {
                // Desktop Header (Search Bar + Clock)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Search Field
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search title, artist, album...", fontSize = 13.sp, color = Color(0xFF64748B)) },
                        singleLine = true,
                        shape = RoundedCornerShape(20.dp),
                        leadingIcon = {
                            Icon(Icons.Rounded.Search, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(18.dp))
                        },
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
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF141822),
                            unfocusedContainerColor = Color(0xFF141822),
                            focusedBorderColor = Color(0xFF00E5FF),
                            unfocusedBorderColor = Color(0xFF222938),
                            focusedTextColor = Color(0xFFF1F5F9),
                            unfocusedTextColor = Color(0xFFF1F5F9)
                        ),
                        modifier = Modifier.width(320.dp).height(42.dp)
                    )

                    // Desktop System Time
                    DesktopClock(textColor = Color(0xFF94A3B8))
                }

                Divider(color = Color(0xFF1A2230), thickness = 0.8.dp)

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
                                isRadioLoading = isRadioLoading,
                                onStartRadio = {
                                    val seed = currentSong
                                    if (seed != null) {
                                        isRadioLoading = true
                                        scope.launch {
                                            val rQueue = DesktopOnlineRepository.fetchRadioQueue(seed)
                                            queue = rQueue
                                            isRadioLoading = false
                                        }
                                    }
                                },
                                onPlayNextSong = { next ->
                                    queue = queue.filter { it.id != next.id }
                                    playSong(next)
                                }
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
                            DesktopExploreGrid(
                                onSearchCategory = { category ->
                                    scope.launch {
                                        val results = DesktopOnlineRepository.searchSongs(category, limit = 30)
                                        if (results.isNotEmpty()) {
                                            songs = results
                                            currentScreen = DesktopScreen.LIBRARY
                                        }
                                    }
                                }
                            )
                        }
                        DesktopScreen.EQUALIZER -> {
                            DesktopEqualizerView()
                        }
                        DesktopScreen.SETTINGS -> {
                            DesktopSettingsView()
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

        // ─── PERSISTENT BOTTOM PLAYER DOCK (SPOTIFY / APPLE MUSIC STYLE) ──────
        Divider(color = Color(0xFF1C2433), thickness = 1.dp)
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
            isLyricsOpen = currentScreen == DesktopScreen.NOW_PLAYING,
            onToggleLyrics = {
                currentScreen = if (currentScreen == DesktopScreen.NOW_PLAYING) DesktopScreen.LIBRARY else DesktopScreen.NOW_PLAYING
            },
            onOpenNowPlaying = {
                currentScreen = if (currentScreen == DesktopScreen.NOW_PLAYING) DesktopScreen.LIBRARY else DesktopScreen.NOW_PLAYING
            }
        )

        // Modal Search Overlay if needed
        if (isSearchOpen) {
            DesktopSearchDialog(
                onPlaySong = { s -> playSong(s) },
                onClose = { isSearchOpen = false }
            )
        }
    }
}
