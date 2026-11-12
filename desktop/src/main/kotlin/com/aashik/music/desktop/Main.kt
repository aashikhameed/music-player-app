package com.aashik.music.desktop

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.aashik.music.desktop.ui.DesktopMainView
import com.aashik.music.desktop.ui.DesktopTheme

private fun iconBytes(): ByteArray? = Thread.currentThread().contextClassLoader.getResourceAsStream("icon.png")?.use { it.readBytes() }

fun main() = application {
    val windowState = rememberWindowState(width = 1280.dp, height = 760.dp)
    val icon = remember { iconBytes()?.let { BitmapPainter(org.jetbrains.skia.Image.makeFromEncoded(it).toComposeImageBitmap()) } }
    // Dock icon while running from Gradle (the packaged app uses icons/icon.icns).
    remember {
        runCatching {
            if (java.awt.Taskbar.isTaskbarSupported()) iconBytes()?.let { java.awt.Taskbar.getTaskbar().iconImage = javax.imageio.ImageIO.read(it.inputStream()) }
        }
    }

    Window(
        onCloseRequest = ::exitApplication,
        title = "Music",
        icon = icon,
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
