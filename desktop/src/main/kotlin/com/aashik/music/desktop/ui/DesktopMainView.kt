package com.aashik.music.desktop.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aashik.music.desktop.audio.DesktopAudioPlayer
import com.aashik.music.desktop.model.DesktopSong
import com.aashik.music.desktop.service.DesktopLocalScanner
import com.aashik.music.desktop.service.DesktopLyricLine
import com.aashik.music.desktop.service.DesktopLyricsRepository
import com.aashik.music.desktop.ui.glass.*
import kotlinx.coroutines.launch

/**
 * 1:1 Apple Music macOS Native Shell (Landscape Tablet / Desktop Architecture).
 * Fully wired to the centralized Backdrop Liquid Glass components under `ui.glass.*`.
 */
@Composable
fun DesktopMainView() {
    val scope = rememberCoroutineScope()

    var songs by remember { mutableStateOf<List<DesktopSong>>(emptyList()) }
    var currentSong by remember { mutableStateOf<DesktopSong?>(null) }
    var queue by remember { mutableStateOf<List<DesktopSong>>(emptyList()) }
    var lyrics by remember { mutableStateOf<List<DesktopLyricLine>>(emptyList()) }

    var favoriteIds by remember { mutableStateOf(mutableSetOf<String>()) }
    var currentScreen by remember { mutableStateOf(DesktopScreen.NOW_PLAYING) }
    var screenHistory by remember { mutableStateOf(listOf(DesktopScreen.NOW_PLAYING)) }

    // Dynamic Album Art
    var albumArtBitmap by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }

    // Audio Playback State from DesktopAudioPlayer
    val isPlaying by DesktopAudioPlayer.isPlaying.collectAsState()
    val positionMs by DesktopAudioPlayer.currentPositionMs.collectAsState()
    val durationMs by DesktopAudioPlayer.durationMs.collectAsState()
    var isShuffleOn by remember { mutableStateOf(false) }
    var isRepeatOn by remember { mutableStateOf(false) }
    var isQueueDrawerOpen by remember { mutableStateOf(false) }
    var isLyricsOpen by remember { mutableStateOf(false) }
    var volume by remember { mutableStateOf(1.0f) }

    // Fullscreen Now Playing overlay toggle
    var isNowPlayingFullscreen by remember { mutableStateOf(false) }

    // Load music & artwork on launch
    LaunchedEffect(Unit) {
        val savedDir = DesktopLocalScanner.getStoredStorageDir()
        val loaded = DesktopLocalScanner.scanLocalMusic(savedDir)
        songs = loaded
        if (loaded.isNotEmpty() && currentSong == null) {
            currentSong = loaded.first()
            queue = loaded.drop(1)
        }
    }

    LaunchedEffect(currentSong) {
        currentSong?.let { song ->
            val artUrl = song.artworkUrl ?: song.path
            albumArtBitmap = DesktopImageCache.load(artUrl)
            lyrics = DesktopLyricsRepository.fetchLyrics(song)
        }
    }

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
            val next = queue.first()
            val remaining = queue.drop(1)
            playSong(next, remaining)
        }
    }

    val playPrevious: () -> Unit = {
        if (currentSong != null) {
            DesktopAudioPlayer.seekTo(0)
        }
    }

    val navigateTo: (DesktopScreen) -> Unit = { screen ->
        if (screen != currentScreen) {
            screenHistory = screenHistory + screen
            currentScreen = screen
        }
    }

    val navigateBack: () -> Unit = {
        if (screenHistory.size > 1) {
            val prev = screenHistory[screenHistory.size - 2]
            screenHistory = screenHistory.dropLast(1)
            currentScreen = prev
        }
    }

    DesktopLiquidContainer {
        Box(modifier = Modifier.fillMaxSize()) {
            // ─── STANDARD 3-ZONE DESKTOP SHELL ─────────────────────────────────────
            Row(modifier = Modifier.fillMaxSize()) {
                // Left Navigation Sidebar (ui.glass.DesktopGlassSidebar)
                DesktopGlassSidebar(
                    currentScreen = currentScreen,
                    onScreenChange = { navigateTo(it) }
                )

                // Main Stage Viewport
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    // Top App Navigation Bar (ui.glass.DesktopGlassTopBar)
                    DesktopGlassTopBar(
                        currentScreen = currentScreen,
                        onScreenChange = { navigateTo(it) },
                        onBackClick = navigateBack,
                        onForwardClick = {},
                        canGoBack = screenHistory.size > 1,
                        canGoForward = false
                    )

                    // Content Viewport Switcher
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
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
                                    isRadioMode = false,
                                    isRadioLoading = false,
                                    onStartRadio = {},
                                    onExitRadio = {},
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
                                    songs = songs,
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
            }

            // ─── FLOATING MINI-PLAYER CAPSULE DOCK (ui.glass.DesktopGlassBottomBar) ─
            DesktopGlassBottomBar(
                currentSong = currentSong,
                isPlaying = isPlaying,
                isShuffle = isShuffleOn,
                isRepeat = isRepeatOn,
                isLyricsOpen = isLyricsOpen,
                isQueueOpen = isQueueDrawerOpen,
                volume = volume,
                albumArt = albumArtBitmap,
                onPlayPause = {
                    if (isPlaying) DesktopAudioPlayer.pause()
                    else currentSong?.let { DesktopAudioPlayer.play(it) }
                },
                onPrevious = playPrevious,
                onNext = playNext,
                onToggleShuffle = { isShuffleOn = !isShuffleOn },
                onToggleRepeat = { isRepeatOn = !isRepeatOn },
                onToggleLyrics = {
                    isLyricsOpen = !isLyricsOpen
                    navigateTo(DesktopScreen.NOW_PLAYING)
                },
                onToggleQueue = { isQueueDrawerOpen = !isQueueDrawerOpen },
                onVolumeChange = { volume = it },
                onOpenNowPlaying = { isNowPlayingFullscreen = true },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
            )

            // ─── FULLSCREEN NOW PLAYING TAKEOVER OVERLAY (ui.glass.DesktopGlassNowPlaying) ─
            if (isNowPlayingFullscreen) {
                DesktopGlassNowPlaying(
                    currentSong = currentSong,
                    isPlaying = isPlaying,
                    positionMs = positionMs,
                    durationMs = durationMs,
                    albumArt = albumArtBitmap,
                    queue = queue,
                    lyricsLines = lyrics.map { it.text },
                    onClose = { isNowPlayingFullscreen = false },
                    onPlayPause = {
                        if (isPlaying) DesktopAudioPlayer.pause()
                        else currentSong?.let { DesktopAudioPlayer.play(it) }
                    },
                    onPrevious = playPrevious,
                    onNext = playNext,
                    onSeek = { DesktopAudioPlayer.seekTo(it) },
                    onSongSelect = { song -> playSong(song, queue) }
                )
            }
        }
    }
}
