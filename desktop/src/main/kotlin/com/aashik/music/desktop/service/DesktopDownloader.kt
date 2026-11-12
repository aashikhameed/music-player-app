package com.aashik.music.desktop.service

import com.aashik.music.desktop.model.DesktopSong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** Saves streamed songs into the music folder so they play offline and show up in the library. */
object DesktopDownloader {
    /** songId → progress 0..1 (1 = done). Absent = not downloaded in this session. */
    private val _progress = MutableStateFlow<Map<String, Float>>(emptyMap())
    val progress: StateFlow<Map<String, Float>> = _progress.asStateFlow()

    private val unsafe = Regex("[\\\\/:*?\"<>|\\p{Cntrl}]")

    fun targetFile(song: DesktopSong, dir: String): File {
        val ext = song.path.substringBefore('?').substringAfterLast('.', "").lowercase()
            .let { if (it == "mp4") "m4a" else it } // audio-only MP4 streams: .m4a so the library scanner picks them up
            .takeIf { it in setOf("mp3", "m4a", "aac", "flac", "ogg") } ?: "m4a"
        val name = "${song.artist.ifBlank { "Unknown Artist" }} - ${song.title.ifBlank { "Untitled" }}".replace(unsafe, "_").take(150)
        return File(dir, "$name.$ext")
    }

    fun isDownloaded(song: DesktopSong, dir: String): Boolean =
        !song.isStream() || (_progress.value[song.id] ?: 0f) >= 1f || targetFile(song, dir).let { it.exists() && it.length() > 0 }

    /** Downloads [song] into [dir]; returns the saved file, or null on failure. */
    suspend fun download(song: DesktopSong, dir: String): File? = withContext(Dispatchers.IO) {
        if (!song.isStream()) return@withContext File(song.path)
        val out = targetFile(song, dir)
        val tmp = File(out.parentFile, out.name + ".part")
        set(song.id, 0f)
        try {
            out.parentFile?.mkdirs()
            val conn = (URL(song.path).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000; readTimeout = 30_000; instanceFollowRedirects = true
                setRequestProperty("User-Agent", "Mozilla/5.0")
            }
            val total = conn.contentLengthLong
            conn.inputStream.use { input ->
                tmp.outputStream().use { output ->
                    val buf = ByteArray(64 * 1024)
                    var done = 0L; var lastPct = -1
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        output.write(buf, 0, n); done += n
                        if (total > 0) {
                            val pct = (done * 100 / total).toInt()
                            if (pct != lastPct) { lastPct = pct; set(song.id, (pct / 100f).coerceAtMost(0.99f)) }
                        }
                    }
                }
            }
            conn.disconnect()
            if (!tmp.renameTo(out)) { tmp.copyTo(out, overwrite = true); tmp.delete() }
            // Tag sidecar so the library shows the right title / artist / album for this download.
            runCatching { File(out.parentFile, out.nameWithoutExtension + ".tags").writeText(listOf(song.title, song.artist, song.album).joinToString("\n")) }
            song.artworkUrl?.takeIf { it.startsWith("http") }?.let { art ->
                runCatching {
                    val ext = art.substringBefore('?').substringAfterLast('.', "jpg").lowercase().takeIf { it in setOf("jpg", "jpeg", "png", "webp") } ?: "jpg"
                    val c = (URL(art).openConnection() as HttpURLConnection).apply { connectTimeout = 10_000; readTimeout = 15_000; setRequestProperty("User-Agent", "Mozilla/5.0") }
                    c.inputStream.use { i -> File(out.parentFile, out.nameWithoutExtension + "." + ext).outputStream().use { o -> i.copyTo(o) } }
                    c.disconnect()
                }
            }
            set(song.id, 1f)
            out
        } catch (e: Exception) {
            println("[DesktopDownloader] ${e.message}")
            tmp.delete()
            _progress.value = _progress.value - song.id
            null
        }
    }

    private fun set(id: String, v: Float) { _progress.value = _progress.value + (id to v) }
}
