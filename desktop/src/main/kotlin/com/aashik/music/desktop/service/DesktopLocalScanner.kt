package com.aashik.music.desktop.service

import com.aashik.music.desktop.model.DesktopSong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Properties

object DesktopLocalScanner {
    private val AUDIO_EXTENSIONS = setOf("mp3", "m4a", "flac", "wav", "aac", "ogg")
    private val PREFS_FILE = File(System.getProperty("user.home"), ".music_player_desktop.properties")

    fun getStoredStorageDir(): String {
        return try {
            if (PREFS_FILE.exists()) {
                val props = Properties()
                PREFS_FILE.inputStream().use { props.load(it) }
                val dir = props.getProperty("storage_dir")
                if (!dir.isNullOrBlank() && File(dir).exists()) {
                    return dir
                }
            }
            val userHome = System.getProperty("user.home") ?: "~"
            val defaultDir = File(userHome, "Music/MusicPlayer")
            if (!defaultDir.exists()) defaultDir.mkdirs()
            defaultDir.absolutePath
        } catch (_: Exception) {
            val userHome = System.getProperty("user.home") ?: "~"
            "$userHome/Music/MusicPlayer"
        }
    }

    fun saveStorageDir(newDir: String) {
        try {
            val props = Properties()
            if (PREFS_FILE.exists()) {
                PREFS_FILE.inputStream().use { props.load(it) }
            }
            props.setProperty("storage_dir", newDir)
            PREFS_FILE.outputStream().use { props.store(it, "Music Player Desktop Preferences") }
        } catch (e: Exception) {
            println("[DesktopLocalScanner] Failed to save storage dir: ${e.message}")
        }
    }

    suspend fun scanLocalMusic(customDir: String? = null): List<DesktopSong> = withContext(Dispatchers.IO) {
        val userHome = System.getProperty("user.home") ?: return@withContext emptyList()
        val preferredDir = customDir?.takeIf { it.isNotBlank() } ?: getStoredStorageDir()

        val directoriesToScan = mutableListOf<File>()
        val customFile = File(preferredDir)
        if (customFile.exists() && customFile.isDirectory) {
            directoriesToScan.add(customFile)
        }

        val musicDir = File(userHome, "Music")
        if (musicDir.exists() && musicDir.isDirectory && !directoriesToScan.contains(musicDir)) {
            directoriesToScan.add(musicDir)
        }

        val downloadsDir = File(userHome, "Downloads")
        if (downloadsDir.exists() && downloadsDir.isDirectory && !directoriesToScan.contains(downloadsDir)) {
            directoriesToScan.add(downloadsDir)
        }

        val foundSongs = mutableListOf<DesktopSong>()
        val seenPaths = HashSet<String>()

        directoriesToScan.forEach { baseDir ->
            try {
                baseDir.walkTopDown()
                    .maxDepth(4)
                    .filter { it.isFile && it.extension.lowercase() in AUDIO_EXTENSIONS }
                    .forEach { file ->
                        if (!seenPaths.contains(file.absolutePath)) {
                            seenPaths.add(file.absolutePath)
                            val cleanName = file.nameWithoutExtension
                            // "Artist - Title" (the downloader's naming); a .tags sidecar written at download time wins.
                            val tags = runCatching { File(file.parentFile, cleanName + ".tags").readLines() }.getOrNull()
                            val parts = if (cleanName.contains(" - ")) cleanName.split(" - ", limit = 2) else null
                            val title = tags?.getOrNull(0)?.takeIf { it.isNotBlank() } ?: parts?.getOrNull(1)?.trim() ?: cleanName
                            val artist = tags?.getOrNull(1)?.takeIf { it.isNotBlank() } ?: parts?.getOrNull(0)?.trim() ?: "Local Artist"
                            val album = tags?.getOrNull(2)?.takeIf { it.isNotBlank() }

                            // Companion artwork (.jpg or .png with same base name or folder.jpg) or fallback to embedded file artwork
                            val companionArt = listOf("jpg", "jpeg", "png", "webp").firstNotNullOfOrNull { e -> File(file.parentFile, "${file.nameWithoutExtension}.$e").takeIf { it.exists() } }
                                ?: File(file.parentFile, "cover.jpg").takeIf { it.exists() }
                                ?: File(file.parentFile, "folder.jpg").takeIf { it.exists() }

                            val artworkPath = companionArt?.toURI()?.toString() ?: file.absolutePath

                            foundSongs.add(
                                DesktopSong(
                                    id = "local_${file.absolutePath.hashCode()}",
                                    title = title,
                                    artist = artist,
                                    album = album ?: file.parentFile?.name ?: "Local Music",
                                    duration = 0L,
                                    path = file.absolutePath,
                                    artworkUrl = artworkPath
                                )
                            )
                        }
                    }
            } catch (_: Exception) {}
        }

        foundSongs.sortedBy { it.title }
    }
}
