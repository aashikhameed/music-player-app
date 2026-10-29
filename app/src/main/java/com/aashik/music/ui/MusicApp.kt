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
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aashik.music.service.NotificationPlaybackService
import com.aashik.music.theme.CarMusicTheme
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

    val permissions = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_AUDIO,
                Manifest.permission.POST_NOTIFICATIONS,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
    }

    fun startAndBindService() {
        val intent = Intent(context, NotificationPlaybackService::class.java)
        NotificationPlaybackService.startService(context)
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        NotificationPlaybackService.instance?.setViewModel(viewModel)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val audioGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            grants[Manifest.permission.READ_MEDIA_AUDIO] == true
        } else {
            grants[Manifest.permission.READ_EXTERNAL_STORAGE] == true
        }
        if (grants[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            viewModel.setupLocationUpdates()
        }
        if (audioGranted) {
            viewModel.loadSongs()
            startAndBindService()
        }
    }

    DisposableEffect(context) {
        onDispose {
            try {
                context.unbindService(serviceConnection)
            } catch (_: Exception) {}
        }
    }

    // Initial check & auto-launch if already granted
    LaunchedEffect(Unit) {
        val isAudioGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }

        if (isAudioGranted) {
            viewModel.loadSongs()
            startAndBindService()
        }

        // Request missing permissions (e.g. storage or POST_NOTIFICATIONS on Android 13+)
        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            permissionLauncher.launch(permissions)
        }
    }

    // Sync only when songs are loaded
    LaunchedEffect(songs) {
        if (songs.isNotEmpty()) {
            viewModel.syncCurrentPlayingFromPlayer()
        }
    }

    CarMusicTheme(
        isDarkMode = isDarkMode
    ) {
        MusicListScreen(viewModel)
    }
}
