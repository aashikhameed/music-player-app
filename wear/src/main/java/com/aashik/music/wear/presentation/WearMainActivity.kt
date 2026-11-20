package com.aashik.music.wear.presentation

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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

                // Scan watch storage on initial launch
                LaunchedEffect(Unit) {
                    val localSongs = WearMusicPlayer.scanLocalMusic(this@WearMainActivity)
                    if (localSongs.isNotEmpty()) {
                        WearMusicPlayer.setPlaylist(localSongs, 0)
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
                            WearNowPlayingScreen(
                                state = playerState,
                                onPlayPauseClicked = {
                                    if (!playerState.isPlaying && !BluetoothRoutingHelper.isBluetoothAudioConnected(this@WearMainActivity)) {
                                        navController.navigate("bluetooth_prompt")
                                    } else {
                                        WearMusicPlayer.togglePlayPause()
                                    }
                                },
                                onNextClicked = { WearMusicPlayer.next() },
                                onPreviousClicked = { WearMusicPlayer.previous() },
                                onVolumeAdjusted = { delta -> WearMusicPlayer.adjustVolume(delta) },
                                onLibraryClicked = { navController.navigate("library") },
                                onQueueClicked = { navController.navigate("queue") }
                            )
                        }

                        composable("library") {
                            WearLibraryScreen(
                                songs = playerState.playlist,
                                onSongSelected = { song, allSongs ->
                                    val index = allSongs.indexOf(song)
                                    WearMusicPlayer.setPlaylist(allSongs, index)
                                    if (!BluetoothRoutingHelper.isBluetoothAudioConnected(this@WearMainActivity)) {
                                        navController.navigate("bluetooth_prompt")
                                    } else {
                                        navController.popBackStack()
                                    }
                                },
                                onScanStorageClicked = {
                                    val updated = WearMusicPlayer.scanLocalMusic(this@WearMainActivity)
                                    if (updated.isNotEmpty()) {
                                        WearMusicPlayer.setPlaylist(updated, 0)
                                    }
                                },
                                onQueueClicked = { navController.navigate("queue") },
                                onSearchClicked = { navController.navigate("search") }
                            )
                        }

                        composable("queue") {
                            WearQueueScreen(
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
                                onSongSelected = { song ->
                                    WearMusicPlayer.playSong(song)
                                    if (!BluetoothRoutingHelper.isBluetoothAudioConnected(this@WearMainActivity)) {
                                        navController.navigate("bluetooth_prompt")
                                    } else {
                                        navController.popBackStack("now_playing", inclusive = false)
                                    }
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
