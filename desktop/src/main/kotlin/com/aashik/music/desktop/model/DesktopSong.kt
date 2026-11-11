package com.aashik.music.desktop.model

data class DesktopSong(
    val id: String,
    val title: String,
    val artist: String,
    val album: String = "Unknown Album",
    val duration: Long = 0L,
    val path: String,
    val artworkUrl: String? = null
) {
    fun isStream(): Boolean =
        path.startsWith("http://", ignoreCase = true) || path.startsWith("https://", ignoreCase = true)

    fun isDirectPlayable(): Boolean {
        if (!isStream()) return true
        val lower = path.lowercase()
        return lower.contains(".mp3") ||
                lower.contains(".m4a") ||
                lower.contains(".aac") ||
                lower.contains(".flac") ||
                lower.contains(".ogg") ||
                lower.contains(".mp4") ||
                lower.startsWith("http")
    }
}

