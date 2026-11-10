package com.aashik.music

import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.aashik.music.ui.MusicApp

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setBackgroundDrawable(null) // Eliminates root window overdraw
        hideSystemBars()
        handleLaunchIntent(intent)
        setContent {
            MusicApp()
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleLaunchIntent(intent)
    }

    private fun handleLaunchIntent(intent: android.content.Intent?) {
        if (intent?.getBooleanExtra("open_player", false) == true) {
            com.aashik.music.service.NotificationPlaybackService.instance?.viewModel?.openDriveMode()
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemBars()
        }
    }

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            or View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        )
    }

    override fun onKeyDown(keyCode: Int, event: android.view.KeyEvent?): Boolean {
        val vm = com.aashik.music.service.NotificationPlaybackService.instance?.viewModel
        val player = com.aashik.music.service.MusicPlayer.getInstance(this)
        return when (keyCode) {
            android.view.KeyEvent.KEYCODE_MEDIA_NEXT,
            android.view.KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                if (vm != null) vm.playNextSong() else player.playNext()
                true
            }
            android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS,
            android.view.KeyEvent.KEYCODE_MEDIA_REWIND -> {
                if (vm != null) vm.playPreviousSong() else player.playPrevious()
                true
            }
            android.view.KeyEvent.KEYCODE_MEDIA_PLAY -> {
                if (vm != null) vm.resumeMusic() else player.resume()
                true
            }
            android.view.KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                if (vm != null) vm.pause() else player.pause()
                true
            }
            android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
            android.view.KeyEvent.KEYCODE_HEADSETHOOK -> {
                if (vm != null) vm.togglePlayPause() else player.togglePlayPause()
                true
            }
            android.view.KeyEvent.KEYCODE_MEDIA_STOP -> {
                if (vm != null) vm.pause() else player.stop()
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }
}
