package com.aashik.music.desktop.tools

import androidx.compose.runtime.Composable
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
        .mapIndexed { i, (t, a) -> DesktopSong("id$i", t, a, "Album ${i + 1}", 200_000L + i * 7_000L, "/demo/$i.mp3") }
    val lyr = (0 until 14).map { DesktopLyricLine(it * 4000L, "Come on, come on, turn the radio on $it") }
    val preview = DeskPreview(demo, lyr,
        dest = when (mode) { "recent" -> DeskDest.RECENT; "home" -> DeskDest.HOME; "artists" -> DeskDest.ARTISTS; "albums" -> DeskDest.ALBUMS; "radio" -> DeskDest.RADIO
            "settings" -> DeskDest.SETTINGS; "eq" -> DeskDest.EQUALIZER; "search" -> DeskDest.SEARCH; "playlists" -> DeskDest.PLAYLISTS; else -> DeskDest.SONGS },
        playing = mode != "idle", panel = if (mode == "queue") RightPanel.QUEUE else RightPanel.LYRICS,
        sheet = mode == "sheet", query = if (mode == "search") "a" else "")
    val scene = ImageComposeScene(w, h, Density(1f)) { DesktopTheme { DeskApp(preview) } }
    var t = 0L
    var image = scene.render(t)
    repeat(40) { Thread.sleep(50); t += 50_000_000L; image = scene.render(t) }
    val data = image.encodeToData(EncodedImageFormat.PNG)!!
    File(out).writeBytes(data.bytes)
    println("wrote $out")
    kotlin.system.exitProcess(0)
}
