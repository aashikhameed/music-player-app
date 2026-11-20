package com.aashik.music.wear.presentation

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.aashik.music.wear.audio.BluetoothRoutingHelper
import com.aashik.music.wear.player.WearMusicPlayer
import com.aashik.music.wear.presentation.dialog.BluetoothPromptDialog
import com.aashik.music.wear.presentation.library.WearLibraryScreen
import com.aashik.music.wear.presentation.player.WearNowPlayingScreen
import com.aashik.music.wear.presentation.queue.WearQueueScreen
import com.aashik.music.wear.presentation.search.WearSearchScreen
import com.aashik.music.wear.presentation.util.isScreenRound
import com.aashik.music.wear.service.WearPlaybackService
import com.aashik.music.wear.storage.WearDownloadManager
import com.aashik.music.wear.storage.WearDownloadStatus
import com.aashik.music.wear.theme.WearMusicTheme

class WearMainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize standalone player & start background service
        WearMusicPlayer.initialize(this)
        val serviceIntent = Intent(this, WearPlaybackService::class.java)
        startService(serviceIntent)

        setContent {
            WearMusicTheme {
                val isRound = isScreenRound()
                val navController = rememberSwipeDismissableNavController()
                val playerState by WearMusicPlayer.state.collectAsState()
                val downloadStates by WearDownloadManager.downloadStates.collectAsState()
                var downloadedTracks by remember { mutableStateOf(emptyList<com.aashik.music.wear.model.WearSong>()) }

                fun refreshDownloads() {
                    downloadedTracks = WearDownloadManager.getDownloadedTracks(this@WearMainActivity)
                }

                // Scan watch storage & downloads on initial launch
                LaunchedEffect(Unit) {
                    refreshDownloads()
                    val localSongs = WearMusicPlayer.scanLocalMusic(this@WearMainActivity)
                    val combined = if (downloadedTracks.isNotEmpty()) {
                        val ids = localSongs.map { it.id }.toSet()
                        localSongs + downloadedTracks.filter { !ids.contains(it.id) }
                    } else {
                        localSongs
                    }
                    if (combined.isNotEmpty()) {
                        WearMusicPlayer.setPlaylist(combined, 0)
                    }
                }

                Scaffold(
                    timeText = { TimeText() },
                    vignette = {
                        if (isRound) {
                            Vignette(vignettePosition = VignettePosition.TopAndBottom)
                        }
                    }
                ) {
                    SwipeDismissableNavHost(
                        navController = navController,
                        startDestination = "now_playing"
                    ) {
                        composable("now_playing") {
                            val curSong = playerState.currentSong
                            val currentStatus = if (curSong != null) {
                                downloadStates[curSong.id] ?: WearDownloadStatus.Idle
                            } else {
                                WearDownloadStatus.Idle
                            }
                            val isDownloaded = if (curSong != null) {
                                WearDownloadManager.isSongDownloaded(this@WearMainActivity, curSong)
                            } else false

                            WearNowPlayingScreen(
                                state = playerState,
                                onPlayPauseClicked = {
                                    WearMusicPlayer.togglePlayPause()
                                },
                                onNextClicked = { WearMusicPlayer.next() },
                                onPreviousClicked = { WearMusicPlayer.previous() },
                                onSeekAdjusted = { deltaMs -> WearMusicPlayer.seekBy(deltaMs) },
                                onLibraryClicked = { navController.navigate("library") },
                                onQueueClicked = { navController.navigate("queue") },
                                onDownloadClicked = { song ->
                                    WearDownloadManager.downloadSong(this@WearMainActivity, song) { downloadedSong ->
                                        refreshDownloads()
                                    }
                                },
                                downloadStatus = currentStatus,
                                isDownloaded = isDownloaded
                            )
                        }

                        composable("library") {
                            WearLibraryScreen(
                                songs = playerState.playlist,
                                onSongSelected = { song, allSongs ->
                                    val index = allSongs.indexOf(song)
                                    WearMusicPlayer.setPlaylist(allSongs, index)
                                    navController.popBackStack()
                                },
                                onScanStorageClicked = {
                                    refreshDownloads()
                                    val updated = WearMusicPlayer.scanLocalMusic(this@WearMainActivity)
                                    val combined = if (downloadedTracks.isNotEmpty()) {
                                        val ids = updated.map { it.id }.toSet()
                                        updated + downloadedTracks.filter { !ids.contains(it.id) }
                                    } else {
                                        updated
                                    }
                                    if (combined.isNotEmpty()) {
                                        WearMusicPlayer.setPlaylist(combined, 0)
                                    }
                                },
                                onQueueClicked = { navController.navigate("queue") },
                                onSearchClicked = { navController.navigate("search") },
                                onDownloadsClicked = {
                                    refreshDownloads()
                                    if (downloadedTracks.isNotEmpty()) {
                                        WearMusicPlayer.setPlaylist(downloadedTracks, 0)
                                        navController.popBackStack("now_playing", inclusive = false)
                                    }
                                },
                                downloadedCount = downloadedTracks.size
                            )
                        }

                        composable("queue") {
                            com.aashik.music.wear.presentation.queue.WearQueueScreen(
                                playlist = playerState.playlist,
                                currentIndex = playerState.currentIndex,
                                isPlaying = playerState.isPlaying,
                                onSongSelected = { index ->
                                    WearMusicPlayer.setPlaylist(playerState.playlist, index)
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable("search") {
                            WearSearchScreen(
                                onSongSelected = { song, playlist, category ->
                                    if (playlist.isNotEmpty()) {
                                        val index = playlist.indexOf(song).coerceAtLeast(0)
                                        WearMusicPlayer.setPlaylist(playlist, index, category)
                                    } else {
                                        WearMusicPlayer.playSong(song)
                                    }
                                    navController.popBackStack("now_playing", inclusive = false)
                                },
                                onPlayLiveStation = { stationSong ->
                                    WearMusicPlayer.setPlaylist(listOf(stationSong), 0, null)
                                    navController.popBackStack("now_playing", inclusive = false)
                                }
                            )
                        }

                        composable("bluetooth_prompt") {
                            BluetoothPromptDialog(
                                onConnectClicked = {
                                    BluetoothRoutingHelper.openBluetoothSettings(this@WearMainActivity)
                                    navController.popBackStack()
                                },
                                onDismissClicked = {
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
