// MusicApp.kt
package com.aashik.music.ui

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aashik.music.service.NotificationPlaybackService
import com.aashik.music.viewmodel.MusicViewModel
import com.aashik.music.viewmodel.ThemeViewModel

@Composable
fun MusicApp() {
    val viewModel: MusicViewModel = viewModel()

    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    val context = androidx.compose.ui.platform.LocalContext.current

    val themedViewModel: ThemeViewModel = viewModel()
    val isDarkMode by themedViewModel.isDarkMode.collectAsState()
    val songs by viewModel.songs.collectAsState() // assuming you expose songs as StateFlow<List<Song>>


    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.loadSongs()

            val intent = Intent(context, NotificationPlaybackService::class.java)

            NotificationPlaybackService.startService(context)

            val connection = object : android.content.ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                    val binder = service as? NotificationPlaybackService.LocalBinder
                    binder?.getService()?.setViewModel(viewModel)
                }

                override fun onServiceDisconnected(name: ComponentName?) {}
            }

            context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
            NotificationPlaybackService.instance?.setViewModel(viewModel)

        }
    }


    // 🔁 Sync only when songs are loaded
    LaunchedEffect(songs) {
        if (songs.isNotEmpty()) {
            viewModel.syncCurrentPlayingFromPlayer() // no need to pass songList separately
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(permission)
    }
    val colorScheme = if (isDarkMode) darkColorScheme() else lightColorScheme()

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = {
            MusicListScreen(viewModel)
        }
    )
}
