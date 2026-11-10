package com.aashik.music.desktop.service

import com.aashik.music.desktop.model.DesktopSong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object DesktopLocalScanner {
    private val AUDIO_EXTENSIONS = setOf("mp3", "m4a", "flac", "wav", "aac", "ogg")

    suspend fun scanLocalMusic(): List<DesktopSong> = withContext(Dispatchers.IO) {
        val userHome = System.getProperty("user.home") ?: return@withContext emptyList()
        val musicDir = File(userHome, "Music")
        val downloadsDir = File(userHome, "Downloads")

        val foundSongs = mutableListOf<DesktopSong>()

        listOf(musicDir, downloadsDir).forEach { baseDir ->
            if (baseDir.exists() && baseDir.isDirectory) {
                try {
                    baseDir.walkTopDown()
                        .maxDepth(4)
                        .filter { it.isFile && it.extension.lowercase() in AUDIO_EXTENSIONS }
                        .forEach { file ->
                            val cleanName = file.nameWithoutExtension
                            val parts = if (cleanName.contains(" - ")) cleanName.split(" - ", limit = 2) else null
                            val title = parts?.getOrNull(0)?.trim() ?: cleanName
                            val artist = parts?.getOrNull(1)?.trim() ?: "Local Artist"

                            foundSongs.add(
                                DesktopSong(
                                    id = "local_${file.absolutePath.hashCode()}",
                                    title = title,
                                    artist = artist,
                                    album = file.parentFile?.name ?: "Local Music",
                                    duration = 0L,
                                    path = file.absolutePath
                                )
                            )
                        }
                } catch (_: Exception) {}
            }
        }

        foundSongs.sortedBy { it.title }
    }
}
