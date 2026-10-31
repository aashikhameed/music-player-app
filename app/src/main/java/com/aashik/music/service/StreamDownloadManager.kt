package com.aashik.music.service

import android.content.ContentValues
import android.content.Context
import android.media.MediaMetadataRetriever
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.aashik.music.cache.AlbumArtCache
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
 * Implemented with pure Android SDK (HttpURLConnection + MediaStore) to enforce strict zero-bloat
 * APK constraints (< 4.0 MB) and minimum memory overhead on low-spec hardware.
 *
 * Supports configurable custom storage location, Scoped Storage compatibility for modern
 * Android versions (Android 11–15 / Xiaomi HyperOS / Samsung OneUI), and multi-hop HTTP/HTTPS redirects.
 */
object StreamDownloadManager {
    private const val TAG = "StreamDownloadManager"
    private const val PREFS_NAME = "stream_downloads"
    private const val BUFFER_SIZE = 8192
    private val SANITIZE_FILENAME_REGEX = Regex("[^a-zA-Z0-9_-]")

    const val PREF_KEY_STORAGE_PRESET = "download_storage_preset"
    const val PREF_KEY_CUSTOM_PATH = "custom_download_path"

    const val PRESET_PUBLIC_MUSIC = "public_music"
    const val PRESET_PUBLIC_DOWNLOADS = "public_downloads"
    const val PRESET_APP_INTERNAL = "app_internal"
    const val PRESET_CUSTOM = "custom"

    private val downloadScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val _downloadStates = MutableStateFlow<Map<String, DownloadStatus>>(emptyMap())
    val downloadStates: StateFlow<Map<String, DownloadStatus>> = _downloadStates.asStateFlow()

    private val _downloadStoragePreset = MutableStateFlow(PRESET_PUBLIC_MUSIC)
    val downloadStoragePreset: StateFlow<String> = _downloadStoragePreset.asStateFlow()

    private val _downloadLocationPath = MutableStateFlow("")
    val downloadLocationPath: StateFlow<String> = _downloadLocationPath.asStateFlow()

    // Mapping of songId -> local absolute file path
    private val idToLocalPath = ConcurrentHashMap<String, String>()

    @Volatile
    private var isInitialized = false

    @Volatile
    private var cachedDownloadDir: File? = null

    fun init(context: Context, songDao: SongDao) {
        if (isInitialized) return
        isInitialized = true

        downloadScope.launch {
            try {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val savedPreset = prefs.getString(PREF_KEY_STORAGE_PRESET, PRESET_PUBLIC_MUSIC) ?: PRESET_PUBLIC_MUSIC
                _downloadStoragePreset.value = savedPreset
                _downloadLocationPath.value = getEffectiveDownloadPath(context)

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
                    val file = File(s.path)
                    if (file.exists() && (s.id.startsWith("download_") || s.path.startsWith(downloadDirPath))) {
                        val cleanId = s.id.removePrefix("download_")
                        initialMap[s.id] = DownloadStatus.Downloaded
                        initialMap["online_$cleanId"] = DownloadStatus.Downloaded
                        idToLocalPath[s.id] = s.path
                        idToLocalPath["online_$cleanId"] = s.path
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

    fun getStoragePreset(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(PREF_KEY_STORAGE_PRESET, PRESET_PUBLIC_MUSIC) ?: PRESET_PUBLIC_MUSIC
    }

    fun getCustomPath(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(PREF_KEY_CUSTOM_PATH, null)?.ifBlank { null }
    }

    fun setStoragePreset(context: Context, preset: String, customPath: String? = null): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putString(PREF_KEY_STORAGE_PRESET, preset)
        if (!customPath.isNullOrBlank()) {
            editor.putString(PREF_KEY_CUSTOM_PATH, customPath)
        }
        editor.apply()

        cachedDownloadDir = null
        _downloadStoragePreset.value = preset
        _downloadLocationPath.value = getEffectiveDownloadPath(context)
        return true
    }

    fun testDirectoryWritable(path: String): Pair<Boolean, String> {
        return try {
            val dir = File(path)
            if (!dir.exists() && !dir.mkdirs()) {
                return Pair(false, "Cannot create directory. Verify storage permissions.")
            }
            val probe = File(dir, ".probe_${System.currentTimeMillis()}.tmp")
            if (probe.createNewFile()) {
                probe.delete()
                Pair(true, "Directory is writable and ready for downloads")
            } else {
                Pair(false, "Directory is read-only on this Android version.")
            }
        } catch (e: Exception) {
            Pair(false, "Permission error: ${e.message}")
        }
    }

    private fun isDirectoryWritable(dir: File): Boolean {
        return try {
            if (!dir.exists() && !dir.mkdirs()) return false
            val probe = File(dir, ".probe_${System.currentTimeMillis()}.tmp")
            if (probe.createNewFile()) {
                probe.delete()
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    fun getDownloadsDir(context: Context): File {
        cachedDownloadDir?.let { if (it.exists() && isDirectoryWritable(it)) return it }

        val preset = getStoragePreset(context)
        val customPath = getCustomPath(context)

        // Custom path if selected and writable
        if (preset == PRESET_CUSTOM && !customPath.isNullOrBlank()) {
            val customDir = File(customPath)
            if (isDirectoryWritable(customDir)) {
                cachedDownloadDir = customDir
                return customDir
            }
        }

        // Public Downloads folder
        if (preset == PRESET_PUBLIC_DOWNLOADS) {
            try {
                val pubDownload = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "MusicPlayer"
                )
                if (isDirectoryWritable(pubDownload)) {
                    cachedDownloadDir = pubDownload
                    return pubDownload
                }
            } catch (_: Exception) {}
        }

        // Public Music folder
        if (preset == PRESET_PUBLIC_MUSIC) {
            try {
                val pubMusic = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
                    "MusicPlayer"
                )
                if (isDirectoryWritable(pubMusic)) {
                    cachedDownloadDir = pubMusic
                    return pubMusic
                }
            } catch (_: Exception) {}
        }

        // App-specific external storage (Guaranteed writable without permissions on all Android versions)
        val appPrivate = File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), "Downloads")
        if (!appPrivate.exists()) {
            appPrivate.mkdirs()
        }
        cachedDownloadDir = appPrivate
        return appPrivate
    }

    fun getEffectiveDownloadPath(context: Context): String {
        val preset = getStoragePreset(context)
        return when (preset) {
            PRESET_CUSTOM -> getCustomPath(context) ?: getDownloadsDir(context).absolutePath
            PRESET_PUBLIC_MUSIC -> {
                val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "MusicPlayer")
                if (isDirectoryWritable(dir) || Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    dir.absolutePath
                } else {
                    getDownloadsDir(context).absolutePath
                }
            }
            PRESET_PUBLIC_DOWNLOADS -> {
                val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "MusicPlayer")
                if (isDirectoryWritable(dir) || Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    dir.absolutePath
                } else {
                    getDownloadsDir(context).absolutePath
                }
            }
            else -> getDownloadsDir(context).absolutePath
        }
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

    /**
     * Connects to HTTP URL with multi-hop redirect handling across protocols (http -> https).
     * Standard HttpURLConnection fails when redirecting from HTTP to HTTPS due to Java security policy.
     */
    private fun openConnectionWithRedirects(initialUrl: String, maxRedirects: Int = 5): HttpURLConnection {
        var currentUrl = initialUrl
        var redirects = 0
        while (redirects < maxRedirects) {
            val url = URL(currentUrl)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15000
                readTimeout = 25000
                instanceFollowRedirects = false // Handle manually to follow cross-protocol redirects
                setRequestProperty("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:109.0) Gecko/109.0 Firefox/119.0")
                setRequestProperty("Accept", "*/*")
            }

            val code = conn.responseCode
            if (code in 300..399) {
                val location = conn.getHeaderField("Location")
                conn.disconnect()
                if (location.isNullOrBlank()) {
                    throw IllegalStateException("HTTP redirect $code without Location header")
                }
                currentUrl = if (location.startsWith("http://", ignoreCase = true) || location.startsWith("https://", ignoreCase = true)) {
                    location
                } else {
                    URL(url, location).toString()
                }
                redirects++
                continue
            }
            if (code !in 200..299) {
                conn.disconnect()
                throw IllegalStateException("Server returned HTTP $code")
            }
            return conn
        }
        throw IllegalStateException("Exceeded maximum redirects ($maxRedirects)")
    }

    private fun safeMoveFile(source: File, target: File): Boolean {
        try {
            if (target.exists()) target.delete()
            if (source.renameTo(target)) return true
        } catch (_: Exception) {}

        // Fallback to stream copy
        return try {
            source.inputStream().use { input ->
                FileOutputStream(target).use { output ->
                    input.copyTo(output)
                    output.flush()
                }
            }
            source.delete()
            target.exists() && target.length() > 0
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Downloads an audio stream into Android MediaStore on API 29+ (Android 10–15).
     * Bypasses Scoped Storage POSIX restrictions and requires zero runtime permissions.
     */
    private fun downloadViaMediaStore(
        context: Context,
        song: Song,
        fileName: String,
        ext: String,
        subDir: String,
        onProgress: (Float) -> Unit
    ): File? {
        val resolver = context.contentResolver
        val mimeType = if (ext.equals("mp3", ignoreCase = true)) "audio/mpeg" else "audio/mp4"

        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Audio.Media.TITLE, song.title)
            put(MediaStore.Audio.Media.ARTIST, song.artist)
            put(MediaStore.Audio.Media.ALBUM, song.album)
            put(MediaStore.Audio.Media.MIME_TYPE, mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Audio.Media.RELATIVE_PATH, subDir)
                put(MediaStore.Audio.Media.IS_PENDING, 1)
            }
        }

        val uri: Uri = try {
            resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
        } catch (e: Exception) {
            Log.w(TAG, "MediaStore insert failed: ${e.message}")
            null
        } ?: return null

        var success = false
        var conn: HttpURLConnection? = null
        try {
            conn = openConnectionWithRedirects(song.path)
            val totalBytes = conn.contentLengthLong.coerceAtLeast(1L)
            val outputStream = resolver.openOutputStream(uri) ?: return null

            conn.inputStream.use { input ->
                outputStream.use { out ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    var bytesRead: Int
                    var downloadedBytes = 0L
                    var lastProgressReportMs = System.currentTimeMillis()
                    var lastReportedProgress = 0f

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        out.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead

                        val now = System.currentTimeMillis()
                        val progress = (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                        if (progress - lastReportedProgress >= 0.05f || (now - lastProgressReportMs >= 250 && progress > lastReportedProgress)) {
                            lastReportedProgress = progress
                            lastProgressReportMs = now
                            onProgress(progress)
                        }
                    }
                    out.flush()
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val updateValues = ContentValues().apply {
                    put(MediaStore.Audio.Media.IS_PENDING, 0)
                }
                resolver.update(uri, updateValues, null, null)
            }
            success = true

            // Resolve raw file path on disk for native OpenSL ES C++ playback
            var resolvedPath: String? = null
            try {
                val projection = arrayOf(MediaStore.Audio.Media.DATA)
                resolver.query(uri, projection, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val colIndex = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
                        if (colIndex != -1) {
                            resolvedPath = cursor.getString(colIndex)
                        }
                    }
                }
            } catch (_: Exception) {}

            if (resolvedPath != null) {
                val file = File(resolvedPath!!)
                if (file.exists() && file.length() > 20_000L) {
                    return file
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed downloading to MediaStore uri: ${e.message}", e)
        } finally {
            try { conn?.disconnect() } catch (_: Exception) {}
            if (!success) {
                try { resolver.delete(uri, null, null) } catch (_: Exception) {}
            }
        }
        return null
    }

    fun downloadSong(
        context: Context,
        song: Song,
        songDao: SongDao,
        onSongDownloaded: (Song) -> Unit = {}
    ) {
        if (!song.isStream()) return

        val currentStatus = getStatus(song.id)
        if (currentStatus is DownloadStatus.Downloading || currentStatus is DownloadStatus.Downloaded) {
            return
        }

        downloadScope.launch {
            _downloadStates.update { it + (song.id to DownloadStatus.Downloading(0.01f)) }

            val cleanArtist = song.artist.replace(SANITIZE_FILENAME_REGEX, "_").trim('_').take(30)
            val cleanTitle = song.title.replace(SANITIZE_FILENAME_REGEX, "_").trim('_').take(40)
            val hash = Integer.toHexString(song.id.hashCode()).take(8)
            val ext = if (song.path.contains(".mp3", ignoreCase = true)) "mp3" else "m4a"
            val fileName = "${cleanArtist}_${cleanTitle}_$hash.$ext"

            var targetFile: File? = null
            var lastError: String? = null

            // Check if already present in effective download directory
            val activeDir = getDownloadsDir(context)
            val existingCheck = File(activeDir, fileName)
            if (existingCheck.exists() && existingCheck.length() > 20_000L) {
                val localSong = createLocalSong(song, existingCheck)
                songDao.insert(localSong)
                persistDownloadedRecord(context, song.id, existingCheck.absolutePath)
                _downloadStates.update {
                    it + (song.id to DownloadStatus.Downloaded) + (localSong.id to DownloadStatus.Downloaded)
                }
                withContext(Dispatchers.Main) { onSongDownloaded(localSong) }
                return@launch
            }

            val preset = getStoragePreset(context)
            val subDirName = if (preset == PRESET_PUBLIC_DOWNLOADS) "Download/MusicPlayer" else "Music/MusicPlayer"

            // Strategy 1: Android MediaStore API (Android 10+, zero permission needed for public storage)
            val canTryMediaStore = (preset == PRESET_PUBLIC_MUSIC || preset == PRESET_PUBLIC_DOWNLOADS) &&
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

            if (canTryMediaStore) {
                try {
                    targetFile = downloadViaMediaStore(
                        context = context,
                        song = song,
                        fileName = fileName,
                        ext = ext,
                        subDir = subDirName,
                        onProgress = { p ->
                            _downloadStates.update { it + (song.id to DownloadStatus.Downloading(p)) }
                        }
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "MediaStore download attempt failed: ${e.message}")
                    lastError = e.message
                }
            }

            // Strategy 2: Direct filesystem write to target directory
            if (targetFile == null || !targetFile.exists()) {
                val tempFile = File(activeDir, "$fileName.tmp")
                val finalFile = File(activeDir, fileName)

                try {
                    if (tempFile.exists()) tempFile.delete()
                    val conn = openConnectionWithRedirects(song.path)
                    val totalBytes = conn.contentLengthLong.coerceAtLeast(1L)
                    var downloadedBytes = 0L
                    var lastProgressReportMs = System.currentTimeMillis()
                    var lastReportedProgress = 0f

                    conn.inputStream.use { input ->
                        FileOutputStream(tempFile).use { output ->
                            val buffer = ByteArray(BUFFER_SIZE)
                            var bytesRead: Int
                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                output.write(buffer, 0, bytesRead)
                                downloadedBytes += bytesRead

                                val now = System.currentTimeMillis()
                                val progress = (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                                if (progress - lastReportedProgress >= 0.05f || (now - lastProgressReportMs >= 250 && progress > lastReportedProgress)) {
                                    lastReportedProgress = progress
                                    lastProgressReportMs = now
                                    _downloadStates.update { it + (song.id to DownloadStatus.Downloading(progress)) }
                                }
                            }
                            output.flush()
                        }
                    }
                    conn.disconnect()

                    if (tempFile.length() < 20_000L) {
                        throw IllegalStateException("Downloaded file is incomplete")
                    }

                    if (safeMoveFile(tempFile, finalFile)) {
                        targetFile = finalFile
                        lastError = null
                    } else {
                        throw IllegalStateException("Failed to move temporary download to target location")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Direct write failed: ${e.message}")
                    lastError = e.message
                    try { if (tempFile.exists()) tempFile.delete() } catch (_: Exception) {}

                    // Strategy 3: Safe fallback to app-internal storage (guaranteed writable on all devices)
                    val safeDir = File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), "Downloads")
                    if (activeDir.absolutePath != safeDir.absolutePath) {
                        val safeTemp = File(safeDir, "$fileName.tmp")
                        val safeFinal = File(safeDir, fileName)
                        try {
                            if (!safeDir.exists()) safeDir.mkdirs()
                            if (safeTemp.exists()) safeTemp.delete()
                            val conn = openConnectionWithRedirects(song.path)
                            val totalBytes = conn.contentLengthLong.coerceAtLeast(1L)
                            var downloadedBytes = 0L
                            conn.inputStream.use { input ->
                                FileOutputStream(safeTemp).use { output ->
                                    val buffer = ByteArray(BUFFER_SIZE)
                                    var bytesRead: Int
                                    while (input.read(buffer).also { bytesRead = it } != -1) {
                                        output.write(buffer, 0, bytesRead)
                                        downloadedBytes += bytesRead
                                        val progress = (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                                        _downloadStates.update { it + (song.id to DownloadStatus.Downloading(progress)) }
                                    }
                                    output.flush()
                                }
                            }
                            conn.disconnect()

                            if (safeTemp.length() >= 20_000L && safeMoveFile(safeTemp, safeFinal)) {
                                targetFile = safeFinal
                                lastError = null
                            }
                        } catch (fallbackEx: Exception) {
                            Log.e(TAG, "Fallback to app storage also failed: ${fallbackEx.message}")
                            lastError = fallbackEx.message
                            try { if (safeTemp.exists()) safeTemp.delete() } catch (_: Exception) {}
                        }
                    }
                }
            }

            val savedFile = targetFile
            if (savedFile != null && savedFile.exists() && savedFile.length() > 20_000L) {
                val localSong = createLocalSong(song, savedFile)
                songDao.insert(localSong)
                persistDownloadedRecord(context, song.id, savedFile.absolutePath)

                // Notify Android MediaStore of new audio file in public storage
                try {
                    MediaScannerConnection.scanFile(
                        context,
                        arrayOf(savedFile.absolutePath),
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
            } else {
                val errorMsg = lastError ?: "Download failed: storage unavailable"
                Log.e(TAG, "Download failed for ${song.title}: $errorMsg")
                _downloadStates.update {
                    it + (song.id to DownloadStatus.Failed(errorMsg))
                }
            }
        }
    }

    private fun createLocalSong(sourceSong: Song, targetFile: File): Song {
        val cleanId = sourceSong.id.removePrefix("online_")
        val artUrl = sourceSong.artworkUrl
        if (!artUrl.isNullOrBlank()) {
            AlbumArtCache.registerStreamArtwork(targetFile.absolutePath, artUrl)
        }

        var duration = sourceSong.duration
        if (duration <= 0) {
            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(targetFile.absolutePath)
                val durStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                durStr?.toLongOrNull()?.let { duration = it }
                retriever.release()
            } catch (_: Exception) {}
        }

        return Song(
            id = "download_$cleanId",
            title = sourceSong.title,
            artist = sourceSong.artist,
            album = sourceSong.album,
            duration = duration,
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

        // Also clean from MediaStore
        try {
            val resolver = context.contentResolver
            val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            val where = "${MediaStore.Audio.Media.DATA} = ?"
            resolver.delete(uri, where, arrayOf(localPath))
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
