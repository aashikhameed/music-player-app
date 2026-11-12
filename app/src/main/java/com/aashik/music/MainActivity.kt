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
        lockOrientation()
        hideSystemBars()
        handleLaunchIntent(intent)
        setContent {
            MusicApp()
        }
    }

    /**
     * Phones stay in portrait; tablets (and headunits, which are 600dp+ wide) stay in landscape.
     * Tablet = smallest screen width of 600dp or more.
     */
    private fun lockOrientation() {
        val isTablet = resources.configuration.smallestScreenWidthDp >= 600
        requestedOrientation = if (isTablet) {
            android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleLaunchIntent(intent)
    }

    private fun handleLaunchIntent(intent: android.content.Intent?) {
        if (intent?.getBooleanExtra("open_player", false) == true) {
            // Tapping playback notification opens the normal app interface, never forcing HUD / drive mode
            com.aashik.music.service.NotificationPlaybackService.instance?.viewModel?.closeDriveMode()
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemBars()
        }
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        hideSystemBars()
    }

    /**
     * Landscape (automotive headunit): fully immersive, unchanged.
     * Portrait (mobile): edge-to-edge with the status/navigation bars visible; the mobile UI
     * keeps its content inside the safe area via window insets.
     */
    private fun hideSystemBars() {
        val isPortrait = resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT
        if (isPortrait) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            )
            @Suppress("DEPRECATION")
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            @Suppress("DEPRECATION")
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            if (android.os.Build.VERSION.SDK_INT >= 29) window.isNavigationBarContrastEnforced = false
            WindowInsetsControllerCompat(window, window.decorView).show(WindowInsetsCompat.Type.systemBars())
            return
        }
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
