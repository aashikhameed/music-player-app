package com.aashik.music.desktop

import androidx.compose.runtime.LaunchedEffect
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
        title = "Music",
        state = windowState
    ) {
        // No system title bar: content runs under a transparent title bar, so the close / minimise / zoom
        // buttons sit inside the app (top-left of the sidebar), like the macOS Music app.
        LaunchedEffect(window) {
            window.rootPane.putClientProperty("apple.awt.fullWindowContent", true)
            window.rootPane.putClientProperty("apple.awt.transparentTitleBar", true)
            window.rootPane.putClientProperty("apple.awt.windowTitleVisible", false)
        }
        DesktopTheme {
            DesktopMainView()
        }
    }
}
