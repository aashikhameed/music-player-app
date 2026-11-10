package com.aashik.music.ui.mobile.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aashik.music.model.Song
import com.aashik.music.model.domain.DomainAlbum
import com.aashik.music.model.domain.DomainArtist
import com.aashik.music.model.domain.DomainPlaylist
import com.aashik.music.ui.mobile.glass.*
import com.aashik.music.viewmodel.MusicViewModel

sealed interface MobileDetailDestination {
    data class Album(val album: DomainAlbum) : MobileDetailDestination
    data class Artist(val artist: DomainArtist) : MobileDetailDestination
    data class Playlist(val playlist: DomainPlaylist) : MobileDetailDestination
}

/**
 * Mobile Root Shell for Portrait Android Devices.
 * Implements 1:1 Apple Music landscape/portrait architecture:
 * - Fluid lava-lamp animated canvas (MobileLiquidContainer)
 * - 5-tab floating glass bottom bar (Home, New, Radio, Library, Search)
 * - Detail screen navigation stack (Album, Artist, Playlist)
 * - Floating mini-player capsule dock (361x56pt, r=28pt)
 * - Smooth spring modal presentation of MobileGlassNowPlaying
 * - AirPlay audio routing modal sheet
 */
@Composable
fun MobileAppleMusicApp(
    viewModel: MusicViewModel,
    modifier: Modifier = Modifier
) {
    val songs by viewModel.songs.collectAsState()
    val currentSong by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val positionMs by viewModel.positionFlow.collectAsState()
    val durationMs by viewModel.durationFlow.collectAsState()

    var selectedTab by remember { mutableStateOf(MobileNavTab.HOME) }
    var activeDetail by remember { mutableStateOf<MobileDetailDestination?>(null) }
    var isNowPlayingExpanded by remember { mutableStateOf(false) }
    var showAudioRouteSheet by remember { mutableStateOf(false) }

    // System Back Handler
    BackHandler(enabled = isNowPlayingExpanded || showAudioRouteSheet || activeDetail != null) {
        when {
            showAudioRouteSheet -> showAudioRouteSheet = false
            isNowPlayingExpanded -> isNowPlayingExpanded = false
            activeDetail != null -> activeDetail = null
        }
    }

    MobileLiquidContainer(modifier = modifier) {
        Box(modifier = Modifier.fillMaxSize()) {
            // ─── MAIN CONTENT HOST / DETAIL NAVIGATION ─────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
            ) {
                if (activeDetail != null) {
                    when (val dest = activeDetail!!) {
                        is MobileDetailDestination.Album -> {
                            MobileAlbumScreen(
                                album = dest.album,
                                onBack = { activeDetail = null },
                                onSongClick = { song -> viewModel.play(song) },
                                onArtistClick = { artistName ->
                                    val artist = DomainArtist(
                                        name = artistName,
                                        genre = dest.album.genre,
                                        topSongs = songs.filter { it.artist.equals(artistName, ignoreCase = true) },
                                        albums = listOf(dest.album)
                                    )
                                    activeDetail = MobileDetailDestination.Artist(artist)
                                }
                            )
                        }
                        is MobileDetailDestination.Artist -> {
                            MobileArtistScreen(
                                artist = dest.artist,
                                onBack = { activeDetail = null },
                                onSongClick = { song -> viewModel.play(song) },
                                onAlbumClick = { album ->
                                    activeDetail = MobileDetailDestination.Album(album)
                                }
                            )
                        }
                        is MobileDetailDestination.Playlist -> {
                            MobilePlaylistScreen(
                                playlist = dest.playlist,
                                onBack = { activeDetail = null },
                                onSongClick = { song -> viewModel.play(song) },
                                onArtistClick = { artist ->
                                    activeDetail = MobileDetailDestination.Artist(artist)
                                }
                            )
                        }
                    }
                } else {
                    // TAB DESTINATIONS
                    when (selectedTab) {
                        MobileNavTab.HOME -> {
                            MobileHomeScreen(
                                songs = songs,
                                onSongClick = { song -> viewModel.play(song) },
                                onAlbumClick = { album ->
                                    activeDetail = MobileDetailDestination.Album(album)
                                },
                                onSeeAllTopPicks = {},
                                onSeeAllRecentlyPlayed = {}
                            )
                        }
                        MobileNavTab.NEW -> {
                            MobileNewScreen(
                                songs = songs,
                                onSongClick = { song -> viewModel.play(song) },
                                onPlaylistClick = { playlist ->
                                    activeDetail = MobileDetailDestination.Playlist(playlist)
                                },
                                onSeeAllCharts = {}
                            )
                        }
                        MobileNavTab.RADIO -> {
                            MobileRadioScreen(
                                onStationClick = { station ->
                                    songs.firstOrNull()?.let { viewModel.play(it) }
                                }
                            )
                        }
                        MobileNavTab.LIBRARY -> {
                            MobileLibraryScreen(
                                songs = songs,
                                onCategoryClick = { category ->
                                    when (category) {
                                        LibraryCategory.ALBUMS -> {
                                            val firstSong = songs.firstOrNull()
                                            if (firstSong != null) {
                                                val album = DomainAlbum(
                                                    title = firstSong.album,
                                                    artist = firstSong.artist,
                                                    tracks = songs.filter { it.album == firstSong.album }
                                                )
                                                activeDetail = MobileDetailDestination.Album(album)
                                            }
                                        }
                                        LibraryCategory.ARTISTS -> {
                                            val firstSong = songs.firstOrNull()
                                            if (firstSong != null) {
                                                val artist = DomainArtist(
                                                    name = firstSong.artist,
                                                    topSongs = songs.filter { it.artist == firstSong.artist }
                                                )
                                                activeDetail = MobileDetailDestination.Artist(artist)
                                            }
                                        }
                                        LibraryCategory.PLAYLISTS -> {
                                            val playlist = DomainPlaylist(
                                                id = "favorites",
                                                title = "Favorite Songs",
                                                songs = songs.take(15)
                                            )
                                            activeDetail = MobileDetailDestination.Playlist(playlist)
                                        }
                                        else -> {}
                                    }
                                },
                                onAlbumClick = { album ->
                                    activeDetail = MobileDetailDestination.Album(album)
                                }
                            )
                        }
                        MobileNavTab.SEARCH -> {
                            MobileSearchScreen(
                                songs = songs,
                                onSongClick = { song -> viewModel.play(song) }
                            )
                        }
                    }
                }
            }

            // ─── FLOATING MINI-PLAYER CAPSULE DOCK (10dp above bottom bar) ───────
            AnimatedVisibility(
                visible = !isNowPlayingExpanded && currentSong != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 80.dp) // Sits 10dp above 54dp + 16dp nav bar
            ) {
                MobileGlassMiniPlayer(
                    currentSong = currentSong,
                    isPlaying = isPlaying,
                    albumArt = null,
                    onPlayPause = { viewModel.togglePlayPause() },
                    onNext = { viewModel.playNextSong() },
                    onOpenNowPlaying = { isNowPlayingExpanded = true }
                )
            }

            // ─── FLOATING 5-TAB GLASS BOTTOM NAVIGATION BAR (16dp above edge) ──
            AnimatedVisibility(
                visible = !isNowPlayingExpanded,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
            ) {
                MobileGlassBottomNavBar(
                    selectedTab = selectedTab,
                    onTabSelected = {
                        activeDetail = null // Return to root tab view
                        selectedTab = it
                    }
                )
            }

            // ─── FULLSCREEN NOW PLAYING MODAL TAKEOVER OVERLAY ───────────────────
            AnimatedVisibility(
                visible = isNowPlayingExpanded,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                ) + fadeIn(),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                ) + fadeOut()
            ) {
                MobileGlassNowPlaying(
                    currentSong = currentSong,
                    isPlaying = isPlaying,
                    positionMs = positionMs,
                    durationMs = durationMs,
                    albumArt = null,
                    isFavorite = false,
                    onToggleFavorite = { currentSong?.let { viewModel.toggleFavorite(it.path) } },
                    onClose = { isNowPlayingExpanded = false },
                    onPlayPause = { viewModel.togglePlayPause() },
                    onPrevious = { viewModel.playPreviousSong() },
                    onNext = { viewModel.playNextSong() },
                    onSeek = { viewModel.seekTo(it) },
                    onToggleLyrics = {},
                    onToggleQueue = {},
                    onOpenAudioRoute = { showAudioRouteSheet = true }
                )
            }

            // ─── AIRPLAY AUDIO ROUTE MODAL SHEET ────────────────────────────────
            AnimatedVisibility(
                visible = showAudioRouteSheet,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                MobileGlassAudioRouteSheet(
                    devices = listOf(
                        AudioOutputDevice("1", "AirPods Max", Icons.Rounded.Radio, 95, true),
                        AudioOutputDevice("2", "Phone Speaker", Icons.Rounded.GraphicEq, null, false),
                        AudioOutputDevice("3", "MacBook Pro", Icons.Rounded.Settings, null, false)
                    ),
                    selectedDeviceId = "1",
                    onSelectDevice = { showAudioRouteSheet = false },
                    onDismiss = { showAudioRouteSheet = false }
                )
            }
        }
    }
}
