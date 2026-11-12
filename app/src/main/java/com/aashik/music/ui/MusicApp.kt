package com.aashik.music.ui

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aashik.music.service.NotificationPlaybackService
import com.aashik.music.theme.CarMusicTheme
import com.aashik.music.ui.components.AppUpdateDialog
import com.aashik.music.viewmodel.MusicViewModel

@Composable
fun MusicApp() {
    val viewModel: MusicViewModel = viewModel()
    val context = androidx.compose.ui.platform.LocalContext.current

    val isDarkMode by viewModel.isDarkTheme.collectAsState()
    val songs by viewModel.songs.collectAsState()

    val serviceConnection = remember {
        object : android.content.ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                val binder = service as? NotificationPlaybackService.LocalBinder
                binder?.getService()?.setViewModel(viewModel)
            }

            override fun onServiceDisconnected(name: ComponentName?) {}
        }
    }

    fun startAndBindService() {
        val intent = Intent(context, NotificationPlaybackService::class.java)
        NotificationPlaybackService.startService(context)
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        NotificationPlaybackService.instance?.setViewModel(viewModel)
    }

    DisposableEffect(context) {
        onDispose {
            try {
                context.unbindService(serviceConnection)
            } catch (_: Exception) {}
        }
    }

    // Load songs only if permission was already granted previously; start playback service
    LaunchedEffect(Unit) {
        val isAudioGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }

        if (isAudioGranted) {
            viewModel.loadSongs()
        }
        startAndBindService()
    }

    // Request notification permission on Android 13+ only when playback is actively initiated
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentSong by viewModel.currentSong.collectAsState()
    var hasRequestedNotificationPermission by remember { mutableStateOf(false) }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Notification permission response handled
    }

    LaunchedEffect(isPlaying, currentSong) {
        if (isPlaying && currentSong != null && !hasRequestedNotificationPermission) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val hasNotif = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
                if (!hasNotif) {
                    hasRequestedNotificationPermission = true
                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    // Sync only when songs are loaded
    LaunchedEffect(songs) {
        if (songs.isNotEmpty()) {
            viewModel.syncCurrentPlayingFromPlayer()
        }
    }

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isCarLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    CarMusicTheme(
        isDarkMode = isDarkMode
    ) {
        if (isCarLandscape) {
            // Automotive Headunit (1280×720): liquid glass car layout (CAR_LAYOUT_REDESIGN_PLAN.md)
            com.aashik.music.ui.car.glass.CarGlassApp(viewModel)
        } else {
            // Mobile Portrait: Liquid Glass UI
            com.aashik.music.ui.mobile.screens.MobileGlassMusicApp(viewModel)
        }
        AppUpdateDialog(viewModel = viewModel)
    }
}
