package com.aashik.music.desktop

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.aashik.music.desktop.ui.DesktopMainView
import com.aashik.music.desktop.ui.DesktopTheme

fun main() = application {
    val windowState = rememberWindowState(width = 1280.dp, height = 760.dp)

    Window(
        onCloseRequest = ::exitApplication,
        title = "Music Player (macOS)",
        state = windowState
    ) {
        DesktopTheme {
            DesktopMainView()
        }
    }
}
