package com.aashik.music.wear.storage

import android.content.Context
import android.os.Environment
import com.aashik.music.wear.model.WearSong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

sealed class WearDownloadStatus {
    object Idle : WearDownloadStatus()
    data class Downloading(val progress: Float) : WearDownloadStatus()
    object Downloaded : WearDownloadStatus()
    data class Failed(val error: String) : WearDownloadStatus()
}

object WearDownloadManager {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val _downloadStates = MutableStateFlow<Map<String, WearDownloadStatus>>(emptyMap())
    val downloadStates: StateFlow<Map<String, WearDownloadStatus>> = _downloadStates.asStateFlow()

    fun getDownloadDir(context: Context): File {
        val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: context.filesDir, "MusicPlayer")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun isSongDownloaded(context: Context, song: WearSong): Boolean {
        if (!song.path.startsWith("http://", ignoreCase = true) && !song.path.startsWith("https://", ignoreCase = true)) {
            return true
        }
        val file = getTargetFile(context, song)
        return file.exists() && file.length() > 50_000L
    }

    fun getDownloadedSong(context: Context, song: WearSong): WearSong? {
        val file = getTargetFile(context, song)
        if (file.exists() && file.length() > 50_000L) {
            return song.copy(path = file.absolutePath)
        }
        return null
    }

    private fun getTargetFile(context: Context, song: WearSong): File {
        val illegalChars = Regex("""[\\\\/:*?"<>|\r\n\t]""")
        val cleanTitle = song.title.replace(illegalChars, " ").replace(Regex("""\s+"""), " ").trim().take(50)
        val cleanArtist = song.artist.replace(illegalChars, " ").replace(Regex("""\s+"""), " ").trim().take(30)
        val ext = if (song.path.contains(".mp3", ignoreCase = true)) "mp3" else "m4a"
        val fileName = if (cleanArtist.isNotBlank() && cleanArtist != "Unknown Artist") {
            "$cleanTitle - $cleanArtist.$ext"
        } else {
            "$cleanTitle.$ext"
        }
        return File(getDownloadDir(context), fileName)
    }

    fun downloadSong(context: Context, song: WearSong, onComplete: ((WearSong) -> Unit)? = null) {
        val isNetwork = song.path.startsWith("http://", ignoreCase = true) || song.path.startsWith("https://", ignoreCase = true)
        if (!isNetwork) return

        if (isSongDownloaded(context, song)) {
            _downloadStates.update { it + (song.id to WearDownloadStatus.Downloaded) }
            val downloaded = getDownloadedSong(context, song) ?: song
            onComplete?.invoke(downloaded)
            return
        }

        if (_downloadStates.value[song.id] is WearDownloadStatus.Downloading) return

        _downloadStates.update { it + (song.id to WearDownloadStatus.Downloading(0.05f)) }

        scope.launch {
            val targetFile = getTargetFile(context, song)
            val tempFile = File(targetFile.parentFile, "${targetFile.name}.tmp")
            var connection: HttpURLConnection? = null
            try {
                val url = URL(song.path)
                connection = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 10000
                    readTimeout = 15000
                    instanceFollowRedirects = true
                    setRequestProperty("User-Agent", "Mozilla/5.0 (WearOS Downloader)")
                }

                val totalLength = connection.contentLength.toLong()
                connection.inputStream.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        val buffer = ByteArray(8 * 1024)
                        var bytesRead: Int
                        var totalDownloaded = 0L
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            totalDownloaded += bytesRead
                            if (totalLength > 0) {
                                val progress = (totalDownloaded.toFloat() / totalLength.toFloat()).coerceIn(0.05f, 0.95f)
                                _downloadStates.update { it + (song.id to WearDownloadStatus.Downloading(progress)) }
                            }
                        }
                        output.flush()
                    }
                }

                if (tempFile.exists() && tempFile.length() > 50_000L) {
                    if (targetFile.exists()) targetFile.delete()
                    tempFile.renameTo(targetFile)
                    _downloadStates.update { it + (song.id to WearDownloadStatus.Downloaded) }
                    val localSong = song.copy(path = targetFile.absolutePath)
                    withContext(Dispatchers.Main) {
                        onComplete?.invoke(localSong)
                    }
                } else {
                    tempFile.delete()
                    _downloadStates.update { it + (song.id to WearDownloadStatus.Failed("Download incomplete")) }
                }
            } catch (e: Exception) {
                tempFile.delete()
                _downloadStates.update { it + (song.id to WearDownloadStatus.Failed(e.message ?: "Download failed")) }
            } finally {
                connection?.disconnect()
            }
        }
    }

    fun getDownloadedTracks(context: Context): List<WearSong> {
        val dir = getDownloadDir(context)
        if (!dir.exists()) return emptyList()
        val files = dir.listFiles { file ->
            file.isFile && (file.extension.equals("mp3", true) || file.extension.equals("m4a", true)) && file.length() > 50_000L
        } ?: return emptyList()

        return files.map { file ->
            val nameWithoutExt = file.nameWithoutExtension
            val parts = nameWithoutExt.split(" - ")
            val title = parts.getOrNull(0) ?: nameWithoutExt
            val artist = parts.getOrNull(1) ?: "Downloaded"
            WearSong(
                id = "download_${file.name.hashCode()}",
                title = title,
                artist = artist,
                durationMs = 0L,
                path = file.absolutePath,
                album = "Downloads"
            )
        }
    }
}
