package com.aashik.music.service

import android.content.Context
import android.media.MediaScannerConnection
import android.os.Environment
import android.util.Log
import com.aashik.music.data.SongDao
import com.aashik.music.model.Song
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
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

sealed class DownloadStatus {
    object Idle : DownloadStatus()
    data class Downloading(val progress: Float) : DownloadStatus()
    object Downloaded : DownloadStatus()
    data class Failed(val error: String) : DownloadStatus()
}

/**
 * Automotive Headunit Offline Download Manager.
 * Downloads online audio streams and saves them to local storage,
 * indexing them into the Room database for full offline playback via C++ OpenSL ES.
 *
 * Implemented with pure Android SDK (HttpURLConnection) to enforce strict zero-bloat
 * APK constraints (< 4.0 MB) and minimum memory overhead on low-spec hardware.
 */
object StreamDownloadManager {
    private const val TAG = "StreamDownloadManager"
    private const val PREFS_NAME = "stream_downloads"
    private const val BUFFER_SIZE = 8192

    private val downloadScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val _downloadStates = MutableStateFlow<Map<String, DownloadStatus>>(emptyMap())
    val downloadStates: StateFlow<Map<String, DownloadStatus>> = _downloadStates.asStateFlow()

    // Mapping of songId -> local absolute file path
    private val idToLocalPath = ConcurrentHashMap<String, String>()

    @Volatile
    private var isInitialized = false

    fun init(context: Context, songDao: SongDao) {
        if (isInitialized) return
        isInitialized = true

        downloadScope.launch {
            try {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val allPrefs = prefs.all
                val downloadDir = getDownloadsDir(context)
                val initialMap = HashMap<String, DownloadStatus>()

                for ((key, value) in allPrefs) {
                    if (key.startsWith("dl_") && value is String) {
                        val songId = key.removePrefix("dl_")
                        val file = File(value)
                        if (file.exists() && file.length() > 20_000L) {
                            idToLocalPath[songId] = value
                            initialMap[songId] = DownloadStatus.Downloaded
                        }
                    }
                }

                // Also inspect existing Room DB for downloaded tracks
                val existingSongs = songDao.getAllSongs()
                val downloadDirPath = downloadDir.absolutePath
                for (s in existingSongs) {
                    if (s.path.startsWith(downloadDirPath) && File(s.path).exists()) {
                        initialMap[s.id] = DownloadStatus.Downloaded
                        idToLocalPath[s.id] = s.path
                    }
                }

                _downloadStates.update { current ->
                    val combined = HashMap(current)
                    combined.putAll(initialMap)
                    combined
                }
            } catch (e: Exception) {
                Log.e(TAG, "Initialization failed: ${e.message}", e)
            }
        }
    }

    fun getDownloadsDir(context: Context): File {
        // Priority 1: Standard Public Music storage (/storage/emulated/0/Music/BitChord)
        // Accessible by all file managers, PC transfers, and media players (matching BitChord)
        try {
            val publicMusic = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
                "BitChord"
            )
            if (publicMusic.exists() || publicMusic.mkdirs()) {
                if (publicMusic.canWrite()) {
                    return publicMusic
                }
            }
        } catch (_: Exception) {}

        // Priority 2: Standard Public Download storage (/storage/emulated/0/Download/BitChord)
        try {
            val publicDownload = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "BitChord"
            )
            if (publicDownload.exists() || publicDownload.mkdirs()) {
                if (publicDownload.canWrite()) {
                    return publicDownload
                }
            }
        } catch (_: Exception) {}

        // Priority 3: App-specific external files storage (/storage/emulated/0/Android/data/com.aashik.music/files/Music/Downloads)
        val appPrivate = File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), "Downloads")
        if (!appPrivate.exists()) {
            appPrivate.mkdirs()
        }
        return appPrivate
    }

    fun getStatus(songId: String): DownloadStatus {
        return _downloadStates.value[songId] ?: DownloadStatus.Idle
    }

    fun isDownloaded(songId: String): Boolean {
        return getStatus(songId) is DownloadStatus.Downloaded
    }

    fun getLocalPathForSong(songId: String): String? {
        val path = idToLocalPath[songId] ?: return null
        return if (File(path).exists()) path else null
    }

    fun downloadSong(
        context: Context,
        song: Song,
        songDao: SongDao,
        onSongDownloaded: (Song) -> Unit = {}
    ) {
        // Only streamable tracks with valid length can be downloaded
        if (!song.isStream() || song.duration <= 0) return

        val currentStatus = getStatus(song.id)
        if (currentStatus is DownloadStatus.Downloading || currentStatus is DownloadStatus.Downloaded) {
            return
        }

        downloadScope.launch {
            _downloadStates.update { it + (song.id to DownloadStatus.Downloading(0.01f)) }

            val downloadDir = getDownloadsDir(context)
            val cleanArtist = song.artist.replace(Regex("[^a-zA-Z0-9_-]"), "_").trim('_').take(30)
            val cleanTitle = song.title.replace(Regex("[^a-zA-Z0-9_-]"), "_").trim('_').take(40)
            val hash = Integer.toHexString(song.id.hashCode()).take(8)
            val ext = if (song.path.contains(".mp3", ignoreCase = true)) "mp3" else "m4a"
            val targetFile = File(downloadDir, "${cleanArtist}_${cleanTitle}_$hash.$ext")
            val tempFile = File(downloadDir, "${targetFile.name}.tmp")

            var connection: HttpURLConnection? = null
            var inputStream: InputStream? = null
            var outputStream: FileOutputStream? = null

            try {
                // If destination already exists with valid size, mark completed immediately
                if (targetFile.exists() && targetFile.length() > 50_000L) {
                    val localSong = createLocalSong(song, targetFile)
                    songDao.insert(localSong)
                    persistDownloadedRecord(context, song.id, targetFile.absolutePath)
                    _downloadStates.update {
                        it + (song.id to DownloadStatus.Downloaded) + (localSong.id to DownloadStatus.Downloaded)
                    }
                    withContext(Dispatchers.Main) { onSongDownloaded(localSong) }
                    return@launch
                }

                if (tempFile.exists()) tempFile.delete()

                val url = URL(song.path)
                connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 12000
                    readTimeout = 20000
                    instanceFollowRedirects = true
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:109.0) Gecko/109.0 Firefox/119.0")
                    setRequestProperty("Accept", "*/*")
                }

                val responseCode = connection.responseCode
                if (responseCode !in 200..299) {
                    throw IllegalStateException("Server returned HTTP $responseCode")
                }

                val totalBytes = connection.contentLengthLong.coerceAtLeast(1L)
                inputStream = connection.inputStream
                outputStream = FileOutputStream(tempFile)

                val buffer = ByteArray(BUFFER_SIZE)
                var bytesRead: Int
                var downloadedBytes = 0L
                var lastProgressReportMs = System.currentTimeMillis()
                var lastReportedProgress = 0f

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    downloadedBytes += bytesRead

                    val now = System.currentTimeMillis()
                    val progress = (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                    // Throttle state flow updates: update only every 5% progress or 250ms
                    if (progress - lastReportedProgress >= 0.05f || (now - lastProgressReportMs >= 250 && progress > lastReportedProgress)) {
                        lastReportedProgress = progress
                        lastProgressReportMs = now
                        _downloadStates.update { it + (song.id to DownloadStatus.Downloading(progress)) }
                    }
                }

                outputStream.flush()
                outputStream.close()
                outputStream = null
                inputStream.close()
                inputStream = null

                if (tempFile.length() < 20_000L) {
                    throw IllegalStateException("Downloaded file is incomplete or corrupted")
                }

                if (targetFile.exists()) targetFile.delete()
                val renamed = tempFile.renameTo(targetFile)
                if (!renamed) {
                    throw IllegalStateException("Failed to move temporary download to target location")
                }

                val localSong = createLocalSong(song, targetFile)
                songDao.insert(localSong)
                persistDownloadedRecord(context, song.id, targetFile.absolutePath)

                // Notify Android MediaStore of new audio file in public storage
                try {
                    MediaScannerConnection.scanFile(
                        context,
                        arrayOf(targetFile.absolutePath),
                        arrayOf(if (ext == "mp3") "audio/mpeg" else "audio/mp4"),
                        null
                    )
                } catch (_: Exception) {}

                _downloadStates.update {
                    it + (song.id to DownloadStatus.Downloaded) + (localSong.id to DownloadStatus.Downloaded)
                }

                withContext(Dispatchers.Main) {
                    onSongDownloaded(localSong)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Download failed for ${song.title}: ${e.message}", e)
                try {
                    if (tempFile.exists()) tempFile.delete()
                } catch (_: Throwable) {}
                _downloadStates.update {
                    it + (song.id to DownloadStatus.Failed(e.message ?: "Download failed"))
                }
            } finally {
                try { outputStream?.close() } catch (_: Throwable) {}
                try { inputStream?.close() } catch (_: Throwable) {}
                try { connection?.disconnect() } catch (_: Throwable) {}
            }
        }
    }

    private fun createLocalSong(sourceSong: Song, targetFile: File): Song {
        val cleanId = sourceSong.id.removePrefix("online_")
        val artUrl = sourceSong.artworkUrl
        if (!artUrl.isNullOrBlank()) {
            com.aashik.music.cache.AlbumArtCache.registerStreamArtwork(targetFile.absolutePath, artUrl)
        }
        return Song(
            id = "download_$cleanId",
            title = sourceSong.title,
            artist = sourceSong.artist,
            album = sourceSong.album,
            duration = sourceSong.duration,
            path = targetFile.absolutePath,
            artworkUrl = artUrl
        )
    }

    private fun persistDownloadedRecord(context: Context, songId: String, localPath: String) {
        idToLocalPath[songId] = localPath
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString("dl_$songId", localPath).apply()
        } catch (_: Exception) {}
    }

    fun onSongDeleted(context: Context, song: Song) {
        val cleanId = song.id.removePrefix("download_").removePrefix("online_")
        val streamId = "online_$cleanId"
        val localId = "download_$cleanId"

        val localPath = idToLocalPath.remove(song.id)
            ?: idToLocalPath.remove(streamId)
            ?: idToLocalPath.remove(localId)
            ?: song.path

        try {
            val file = File(localPath)
            if (file.exists()) {
                file.delete()
            }
        } catch (_: Exception) {}

        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .remove("dl_${song.id}")
                .remove("dl_$streamId")
                .remove("dl_$localId")
                .apply()
        } catch (_: Exception) {}

        _downloadStates.update { current ->
            val updated = HashMap(current)
            updated.remove(song.id)
            updated.remove(streamId)
            updated.remove(localId)
            updated
        }
    }
}

