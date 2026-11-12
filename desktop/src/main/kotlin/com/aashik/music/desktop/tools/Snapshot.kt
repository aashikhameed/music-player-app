package com.aashik.music.desktop.tools

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.layerBackdrop
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import com.aashik.music.desktop.model.DesktopSong
import com.aashik.music.desktop.service.DesktopLyricLine
import com.aashik.music.desktop.ui.shell.*
import com.aashik.music.desktop.ui.DesktopTheme
import org.jetbrains.skia.EncodedImageFormat
import java.io.File

/** Dev tool: renders the desktop UI off-screen to a PNG (no display needed). args: out.png [w h] */
@OptIn(ExperimentalComposeUiApi::class)
fun main(args: Array<String>) {
    val out = args.getOrElse(0) { "/tmp/desktop.png" }
    val w = args.getOrNull(1)?.toInt() ?: 1280
    val h = args.getOrNull(2)?.toInt() ?: 720
    val mode = args.getOrElse(3) { "library" }
    val demo = listOf("Cheap Thrills" to "Sia", "Unstoppable" to "Sia", "Blinding Lights" to "The Weeknd", "Levitating" to "Dua Lipa", "Heat Waves" to "Glass Animals", "As It Was" to "Harry Styles", "Stay" to "The Kid LAROI", "Bad Habits" to "Ed Sheeran")
        .mapIndexed { i, (t, a) -> DesktopSong("id$i", t, a, "Album ${i + 1}", 200_000L + i * 7_000L, "/demo/$i.mp3", artworkUrl = if (i % 2 == 0) com.aashik.music.desktop.utils.b64u("aHR0cHM6Ly9jLnNhYXZuY2RuLmNvbS8yOTEvT3J1LUthbC1PcnUtS2FubmFkaS1UYW1pbC0yMDEyLTIwMjYwMTIwMjAxMjU3LTUwMHg1MDAud2VicA==") else null) }
    val lyr = (0 until 14).map { DesktopLyricLine(it * 4000L, "Come on, come on, turn the radio on $it") }
    val preview = DeskPreview(demo, lyr,
        dest = when (mode) { "recent" -> DeskDest.RECENT; "home" -> DeskDest.HOME; "artists" -> DeskDest.ARTISTS; "albums" -> DeskDest.ALBUMS; "radio" -> DeskDest.RADIO
            "settings" -> DeskDest.SETTINGS; "eq" -> DeskDest.EQUALIZER; "search" -> DeskDest.SEARCH; "playlists" -> DeskDest.PLAYLISTS; else -> DeskDest.SONGS },
        playing = mode != "idle", panel = if (mode == "queue") RightPanel.QUEUE else RightPanel.LYRICS,
        sheet = mode == "sheet", query = if (mode == "search") "a" else "")
    val scene = ImageComposeScene(w, h, Density(1f)) {
        DesktopTheme { if (mode == "glass") GlassProbe() else DeskApp(preview) }
    }
    var t = 0L
    var image = scene.render(t)
    repeat(60) { Thread.sleep(60); t += 50_000_000L; image = scene.render(t) }
    val data = image.encodeToData(EncodedImageFormat.PNG)!!
    File(out).writeBytes(data.bytes)
    println("wrote $out")
    kotlin.system.exitProcess(0)
}

/** Probe: a capsule of real glass over striped content, to see whether lens/blur actually render. */
@Composable
private fun GlassProbe() {
    val page = com.kyant.backdrop.backdrops.rememberLayerBackdrop()
    androidx.compose.runtime.CompositionLocalProvider(LocalDesk provides DeskDark, LocalDeskBackdrop provides page, LocalDeskWallpaper provides page) {
        Box(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize().layerBackdrop(page)) {
                Row(Modifier.fillMaxSize()) {
                    listOf(0xFFE53935, 0xFFFDD835, 0xFF43A047, 0xFF1E88E5, 0xFF8E24AA, 0xFFFB8C00).forEach {
                        Box(Modifier.weight(1f).fillMaxHeight().background(androidx.compose.ui.graphics.Color(it)))
                    }
                }
                Box(Modifier.align(androidx.compose.ui.Alignment.Center).fillMaxWidth().height(30.dp).background(androidx.compose.ui.graphics.Color.White))
            }
            Box(Modifier.align(androidx.compose.ui.Alignment.Center).width(330.dp).height(120.dp).deskLiquid(60.dp, floating = true, surfaceAlpha = 0.12f, blurDp = 0f))
        }
    }
}
