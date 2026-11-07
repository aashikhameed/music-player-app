package com.aashik.music.service

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import org.json.JSONObject

data class DownloadedMetadata(
    val title: String,
    val artist: String,
    val album: String,
    val artworkUrl: String?,
    val duration: Long,
    val streamUrl: String? = null
)

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

    // Mapping of local absolute file path -> clean metadata
    private val pathToMetadata = ConcurrentHashMap<String, DownloadedMetadata>()

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
                    } else if (key.startsWith("meta_") && value is String) {
                        val localPath = key.removePrefix("meta_")
                        try {
                            val json = JSONObject(value)
                            val meta = DownloadedMetadata(
                                title = json.optString("title"),
                                artist = json.optString("artist"),
                                album = json.optString("album"),
                                artworkUrl = json.optString("art").takeIf { it.isNotBlank() },
                                duration = json.optLong("dur", 0L),
                                streamUrl = json.optString("stream_url").takeIf { it.isNotBlank() }
                            )
                            pathToMetadata[localPath] = meta
                            if (!meta.artworkUrl.isNullOrBlank()) {
                                AlbumArtCache.registerStreamArtwork(localPath, meta.artworkUrl)
                            }
                        } catch (_: Exception) {}
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
                        if (!pathToMetadata.containsKey(s.path) && s.artist != "<unknown>") {
                            val meta = DownloadedMetadata(
                                title = s.title,
                                artist = s.artist,
                                album = s.album,
                                artworkUrl = s.artworkUrl,
                                duration = s.duration
                            )
                            pathToMetadata[s.path] = meta
                            if (!s.artworkUrl.isNullOrBlank()) {
                                AlbumArtCache.registerStreamArtwork(s.path, s.artworkUrl)
                            }
                        }
                    }
                }

                _downloadStates.update { current ->
                    val combined = HashMap(current)
                    combined.putAll(initialMap)
                    combined
                }

                // Background check: retro-tag any existing downloaded files that have companion art or metadata
                downloadScope.launch(Dispatchers.IO) {
                    retroTagExistingDownloads(context)
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
                    put(MediaStore.Audio.Media.TITLE, song.title)
                    put(MediaStore.Audio.Media.ARTIST, song.artist)
                    put(MediaStore.Audio.Media.ALBUM, song.album)
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
        forceRedownload: Boolean = false,
        onSongDownloaded: (Song) -> Unit = {}
    ) {
        if (!song.isStream()) return

        val currentStatus = getStatus(song.id)
        if (currentStatus is DownloadStatus.Downloading) {
            return
        }
        if (!forceRedownload && currentStatus is DownloadStatus.Downloaded) {
            return
        }

        downloadScope.launch {
            _downloadStates.update { it + (song.id to DownloadStatus.Downloading(0.01f)) }

            val illegalChars = Regex("""[\\\\/:*?"<>|\r\n\t]""")
            val cleanTitle = song.title.replace(illegalChars, " ").replace(Regex("""\s+"""), " ").trim().take(60)
            val cleanArtist = song.artist.replace(illegalChars, " ").replace(Regex("""\s+"""), " ").trim().take(40)
            val ext = if (song.path.contains(".mp3", ignoreCase = true)) "mp3" else "m4a"
            val fileName = if (cleanArtist.isNotBlank() && cleanArtist != "Unknown Artist" && cleanArtist != "<unknown>") {
                "$cleanTitle - $cleanArtist.$ext"
            } else {
                "$cleanTitle.$ext"
            }

            var targetFile: File? = null
            var lastError: String? = null

            // Check if already present in effective download directory
            val activeDir = getDownloadsDir(context)
            val existingCheck = File(activeDir, fileName)
            if (!forceRedownload && existingCheck.exists() && existingCheck.length() > 20_000L) {
                val localSong = createLocalSong(song, existingCheck)
                songDao.insert(localSong)
                persistDownloadedRecord(context, song, existingCheck.absolutePath)
                _downloadStates.update {
                    it + (song.id to DownloadStatus.Downloaded) + (localSong.id to DownloadStatus.Downloaded)
                }
                withContext(Dispatchers.Main) { onSongDownloaded(localSong) }
                return@launch
            }
            if (forceRedownload && existingCheck.exists()) {
                try { existingCheck.delete() } catch (_: Exception) {}
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
                saveArtworkAndMetadataForDownload(context, song, savedFile, ext)
                val localSong = createLocalSong(song, savedFile)
                songDao.insert(localSong)
                persistDownloadedRecord(context, song, savedFile.absolutePath)

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

    private fun saveArtworkAndMetadataForDownload(context: Context, song: Song, savedFile: File, ext: String) {
        val artUrl = song.artworkUrl
        var artBytes: ByteArray? = null

        if (!artUrl.isNullOrBlank()) {
            try {
                val conn = openConnectionWithRedirects(artUrl)
                val bytes = conn.inputStream.use { it.readBytes() }
                conn.disconnect()
                if (bytes.isNotEmpty()) {
                    val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    if (bmp != null) {
                        val jpegOut = java.io.ByteArrayOutputStream()
                        bmp.compress(Bitmap.CompressFormat.JPEG, 85, jpegOut)
                        val jpegBytes = jpegOut.toByteArray()
                        artBytes = jpegBytes

                        AlbumArtCache.saveCustomArt(context, savedFile.absolutePath, bmp)
                        AlbumArtCache.saveCustomArt(context, song.path, bmp)

                        // 1. Save companion image file next to downloaded audio file (standard JPEG)
                        val parent = savedFile.parentFile
                        if (parent != null && parent.exists()) {
                            val companion = File(parent, "${savedFile.nameWithoutExtension}.jpg")
                            try {
                                FileOutputStream(companion).use { it.write(jpegBytes) }
                            } catch (_: Exception) {}
                        }
                    } else {
                        artBytes = bytes
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed downloading artwork for ${song.title}: ${e.message}")
            }
        }

        // 3. Embed metadata + art in audio container natively (APIC for MP3, ilst for M4A)
        try {
            if (ext.equals("mp3", ignoreCase = true)) {
                if (artBytes != null) {
                    embedId3Tag(savedFile, song, artBytes)
                }
            } else if (ext.equals("m4a", ignoreCase = true) || ext.equals("mp4", ignoreCase = true)) {
                Mp4TagEditor.embedMetadata(
                    file = savedFile,
                    title = song.title,
                    artist = song.artist,
                    album = song.album,
                    artBytes = artBytes
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed embedding container metadata for ${savedFile.name}: ${e.message}")
        }
    }

    private fun embedId3Tag(file: File, song: Song, artBytes: ByteArray) {
        try {
            val audioBytes = file.readBytes()
            val offset = if (audioBytes.size >= 10 && audioBytes[0] == 'I'.code.toByte() && audioBytes[1] == 'D'.code.toByte() && audioBytes[2] == '3'.code.toByte()) {
                val s0 = audioBytes[6].toInt() and 0x7F
                val s1 = audioBytes[7].toInt() and 0x7F
                val s2 = audioBytes[8].toInt() and 0x7F
                val s3 = audioBytes[9].toInt() and 0x7F
                10 + (s0 shl 21) or (s1 shl 14) or (s2 shl 7) or s3
            } else 0

            val rawAudio = if (offset in 1 until audioBytes.size) {
                audioBytes.copyOfRange(offset, audioBytes.size)
            } else {
                audioBytes
            }

            val framesOut = java.io.ByteArrayOutputStream()
            writeTextFrame(framesOut, "TIT2", song.title)
            writeTextFrame(framesOut, "TPE1", song.artist)
            writeTextFrame(framesOut, "TALB", song.album)

            val apicPayload = java.io.ByteArrayOutputStream()
            apicPayload.write(0)
            apicPayload.write("image/jpeg\u0000".toByteArray(Charsets.ISO_8859_1))
            apicPayload.write(3)
            apicPayload.write(0)
            apicPayload.write(artBytes)
            val apicBytes = apicPayload.toByteArray()

            framesOut.write("APIC".toByteArray(Charsets.ISO_8859_1))
            writeBigEndianInt(framesOut, apicBytes.size)
            framesOut.write(0)
            framesOut.write(0)
            framesOut.write(apicBytes)

            val allFrames = framesOut.toByteArray()
            val synchsafeSize = encodeSynchsafe(allFrames.size)

            val header = ByteArray(10)
            header[0] = 'I'.code.toByte()
            header[1] = 'D'.code.toByte()
            header[2] = '3'.code.toByte()
            header[3] = 3
            header[4] = 0
            header[5] = 0
            header[6] = synchsafeSize[0]
            header[7] = synchsafeSize[1]
            header[8] = synchsafeSize[2]
            header[9] = synchsafeSize[3]

            FileOutputStream(file).use { out ->
                out.write(header)
                out.write(allFrames)
                out.write(rawAudio)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed embedding ID3 tag: ${e.message}")
        }
    }

    private fun writeTextFrame(out: java.io.ByteArrayOutputStream, frameId: String, text: String) {
        if (text.isBlank()) return
        val textBytes = text.toByteArray(Charsets.UTF_8)
        val payload = ByteArray(1 + textBytes.size)
        payload[0] = 3
        System.arraycopy(textBytes, 0, payload, 1, textBytes.size)

        out.write(frameId.toByteArray(Charsets.ISO_8859_1))
        writeBigEndianInt(out, payload.size)
        out.write(0)
        out.write(0)
        out.write(payload)
    }

    private fun writeBigEndianInt(out: java.io.ByteArrayOutputStream, value: Int) {
        out.write((value shr 24) and 0xFF)
        out.write((value shr 16) and 0xFF)
        out.write((value shr 8) and 0xFF)
        out.write(value and 0xFF)
    }

    private fun encodeSynchsafe(size: Int): ByteArray {
        val b = ByteArray(4)
        b[0] = ((size shr 21) and 0x7F).toByte()
        b[1] = ((size shr 14) and 0x7F).toByte()
        b[2] = ((size shr 7) and 0x7F).toByte()
        b[3] = (size and 0x7F).toByte()
        return b
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

    private fun persistDownloadedRecord(context: Context, song: Song, localPath: String) {
        val cleanId = song.id.removePrefix("online_").removePrefix("download_")
        idToLocalPath[song.id] = localPath
        idToLocalPath["online_$cleanId"] = localPath
        idToLocalPath["download_$cleanId"] = localPath

        val streamUrl = if (song.isStream()) song.path else null
        val metadata = DownloadedMetadata(
            title = song.title,
            artist = song.artist,
            album = song.album,
            artworkUrl = song.artworkUrl,
            duration = song.duration,
            streamUrl = streamUrl
        )
        pathToMetadata[localPath] = metadata
        if (!song.artworkUrl.isNullOrBlank()) {
            AlbumArtCache.registerStreamArtwork(localPath, song.artworkUrl)
        }

        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val json = JSONObject().apply {
                put("title", song.title)
                put("artist", song.artist)
                put("album", song.album)
                put("art", song.artworkUrl ?: "")
                put("dur", song.duration)
                if (streamUrl != null) put("stream_url", streamUrl)
            }
            prefs.edit()
                .putString("dl_${song.id}", localPath)
                .putString("dl_online_$cleanId", localPath)
                .putString("dl_download_$cleanId", localPath)
                .putString("meta_$localPath", json.toString())
                .apply()
        } catch (_: Exception) {}
    }

    fun getStreamUrlForPath(path: String): String? {
        return pathToMetadata[path]?.streamUrl
    }

    fun retroTagExistingDownloads(context: Context) {
        try {
            val downloadDir = getDownloadsDir(context)
            if (!downloadDir.exists()) return
            val audioFiles = downloadDir.listFiles { f ->
                f.isFile && (f.extension.equals("m4a", ignoreCase = true) || f.extension.equals("mp3", ignoreCase = true))
            } ?: return

            for (audioFile in audioFiles) {
                try {
                    val companion = File(audioFile.parentFile, "${audioFile.nameWithoutExtension}.jpg")
                    val artBytes = if (companion.exists() && companion.length() > 0) {
                        try {
                            val bmp = BitmapFactory.decodeFile(companion.absolutePath)
                            if (bmp != null) {
                                val jpegOut = java.io.ByteArrayOutputStream()
                                bmp.compress(Bitmap.CompressFormat.JPEG, 85, jpegOut)
                                val jpegBytes = jpegOut.toByteArray()
                                try { FileOutputStream(companion).use { it.write(jpegBytes) } } catch (_: Exception) {}
                                jpegBytes
                            } else {
                                companion.readBytes()
                            }
                        } catch (_: Exception) { null }
                    } else null

                    val meta = getMetadataForPath(context, audioFile.absolutePath)
                    val title = meta?.title?.takeIf { it.isNotBlank() } ?: audioFile.nameWithoutExtension
                    val artist = meta?.artist?.takeIf { it.isNotBlank() && it != "<unknown>" } ?: "Unknown Artist"
                    val album = meta?.album?.takeIf { it.isNotBlank() && it != "<unknown>" } ?: "Downloads"

                    if (audioFile.extension.equals("m4a", ignoreCase = true)) {
                        val tagged = Mp4TagEditor.embedMetadata(audioFile, title, artist, album, artBytes)
                        if (tagged) {
                            Log.d(TAG, "Retro-tagged M4A metadata and artwork for: ${audioFile.name}")
                            MediaScannerConnection.scanFile(context, arrayOf(audioFile.absolutePath), arrayOf("audio/mp4"), null)
                        }
                    } else if (audioFile.extension.equals("mp3", ignoreCase = true) && artBytes != null) {
                        val mockSong = Song(
                            id = audioFile.nameWithoutExtension,
                            title = title,
                            artist = artist,
                            album = album,
                            duration = meta?.duration ?: 0L,
                            path = audioFile.absolutePath
                        )
                        embedId3Tag(audioFile, mockSong, artBytes)
                        MediaScannerConnection.scanFile(context, arrayOf(audioFile.absolutePath), arrayOf("audio/mpeg"), null)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error retro-tagging ${audioFile.name}: ${e.message}")
                }
            }
        } catch (_: Exception) {}
    }

    fun getMetadataForPath(context: Context, path: String): DownloadedMetadata? {
        // 1. Check in-memory map
        pathToMetadata[path]?.let { return it }

        // 2. Check persistent SharedPreferences
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val savedJson = prefs.getString("meta_$path", null)
            if (!savedJson.isNullOrBlank()) {
                val json = JSONObject(savedJson)
                val meta = DownloadedMetadata(
                    title = json.optString("title"),
                    artist = json.optString("artist"),
                    album = json.optString("album"),
                    artworkUrl = json.optString("art").takeIf { it.isNotBlank() },
                    duration = json.optLong("dur", 0L),
                    streamUrl = json.optString("stream_url").takeIf { it.isNotBlank() }
                )
                pathToMetadata[path] = meta
                if (!meta.artworkUrl.isNullOrBlank()) {
                    AlbumArtCache.registerStreamArtwork(path, meta.artworkUrl)
                }
                return meta
            }
        } catch (_: Exception) {}

        // 3. Fallback for older downloaded files without JSON metadata
        val file = File(path)
        val nameWithoutExt = file.nameWithoutExtension
        if (path.contains("MusicPlayer", ignoreCase = true) || path.contains("Downloads", ignoreCase = true) || path.contains("BitChord", ignoreCase = true)) {
            if (nameWithoutExt.contains(" - ")) {
                val parts = nameWithoutExt.split(" - ", limit = 2)
                val parsedTitle = parts[0].trim()
                val parsedArtist = parts.getOrNull(1)?.trim().orEmpty()
                if (parsedTitle.isNotBlank()) {
                    val fallbackMeta = DownloadedMetadata(
                        title = parsedTitle,
                        artist = if (parsedArtist.isNotBlank() && parsedArtist != "<unknown>") parsedArtist else "Unknown Artist",
                        album = "Downloads",
                        artworkUrl = null,
                        duration = 0L
                    )
                    pathToMetadata[path] = fallbackMeta
                    return fallbackMeta
                }
            }

            val parsedLegacy = parseLegacyFilename(nameWithoutExt)
            if (parsedLegacy != null) {
                val fallbackMeta = DownloadedMetadata(
                    title = parsedLegacy.first,
                    artist = parsedLegacy.second,
                    album = "Downloads",
                    artworkUrl = null,
                    duration = 0L
                )
                pathToMetadata[path] = fallbackMeta
                return fallbackMeta
            }

            val cleaned = nameWithoutExt.replace(Regex("_[0-9a-fA-F]{6,8}$"), "")
            val underscoreParts = cleaned.split("__", limit = 2)
            if (underscoreParts.size == 2) {
                val artistPart = underscoreParts[0].replace("_", " ").trim()
                val titlePart = underscoreParts[1].replace("_", " ").trim()
                val fallbackMeta = DownloadedMetadata(
                    title = titlePart,
                    artist = artistPart,
                    album = "Downloads",
                    artworkUrl = null,
                    duration = 0L
                )
                pathToMetadata[path] = fallbackMeta
                return fallbackMeta
            } else if (cleaned.contains("_")) {
                val words = cleaned.replace("_", " ").trim()
                val fallbackMeta = DownloadedMetadata(
                    title = words,
                    artist = "Unknown Artist",
                    album = "Downloads",
                    artworkUrl = null,
                    duration = 0L
                )
                pathToMetadata[path] = fallbackMeta
                return fallbackMeta
            }
        }

        return null
    }

    private fun parseLegacyFilename(nameWithoutExt: String): Pair<String, String>? {
        val withoutHash = nameWithoutExt.replace(Regex("_[0-9a-fA-F]{6,8}$"), "")
        val knownPrefixes = listOf(
            "Anirudh_Ravichander_" to "Anirudh Ravichander",
            "A_R__Rahman_" to "A.R. Rahman",
            "The_Chainsmokers_" to "The Chainsmokers",
            "Na__Muthukumar_" to "Na. Muthukumar",
            "Hariharan___K__S__Chithra_" to "Hariharan, K.S. Chithra",
            "Bombay_Jayashree__Girishh_Gopa_" to "Bombay Jayashri",
            "Asal_Kolaar___Anand_Kashinath_" to "Asal Kolaar, Anand Kashinath",
            "Vivek_" to "Vivek",
            "Oasis_" to "Oasis",
            "Coldplay_" to "Coldplay",
            "Sai_Abhyankkar_" to "Sai Abhyankkar",
            "Sid_Sriram_" to "Sid Sriram",
            "Harris_Jayaraj_" to "Harris Jayaraj",
            "Yuvan_Shankar_Raja_" to "Yuvan Shankar Raja",
            "Ilayaraja_" to "Ilaiyaraaja",
            "Santhosh_Narayanan_" to "Santhosh Narayanan",
            "D__Imman_" to "D. Imman",
            "G_V__Prakash_Kumar_" to "G.V. Prakash Kumar",
            "Hiphop_Tamizha_" to "Hiphop Tamizha"
        )

        for ((prefix, artist) in knownPrefixes) {
            if (withoutHash.startsWith(prefix, ignoreCase = true)) {
                val rawTitle = withoutHash.substring(prefix.length)
                val title = rawTitle
                    .replace("__From__", " (From ")
                    .replace("__Original_Motion_Picture", " - Original")
                    .replace("__Reprise", " (Reprise)")
                    .replace("__Remix", " (Remix)")
                    .replace("__", " ")
                    .replace("_", " ")
                    .replace(Regex("""\s+"""), " ")
                    .trim()
                val formattedTitle = if (rawTitle.contains("__From__") && !title.endsWith(")")) "$title)" else title
                return Pair(formattedTitle, artist)
            }
        }

        return null
    }

    fun onSongDeleted(context: Context, song: Song) {
        val cleanId = song.id.removePrefix("download_").removePrefix("online_")
        val streamId = "online_$cleanId"
        val localId = "download_$cleanId"

        val localPath = idToLocalPath.remove(song.id)
            ?: idToLocalPath.remove(streamId)
            ?: idToLocalPath.remove(localId)
            ?: song.path

        pathToMetadata.remove(localPath)

        try {
            val file = File(localPath)
            if (file.exists()) {
                file.delete()
            }
            val parent = file.parentFile
            if (parent != null && parent.exists()) {
                val companion = File(parent, "${file.nameWithoutExtension}.jpg")
                if (companion.exists()) companion.delete()
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
                .remove("meta_$localPath")
                .apply()
        } catch (_: Exception) {}

        _downloadStates.update { current ->
            val updated = HashMap(current)
            updated.remove(song.id)
            updated.remove(streamId)
            updated.remove(localId)
            updated[song.id] = DownloadStatus.Idle
            updated[streamId] = DownloadStatus.Idle
            updated[localId] = DownloadStatus.Idle
            updated
        }
    }
}
