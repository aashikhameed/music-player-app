package com.aashik.music.wear.model

import androidx.compose.runtime.Immutable
import java.util.Locale

@Immutable
data class WearSong(
    val id: String,
    val title: String,
    val artist: String,
    val durationMs: Long,
    val path: String,
    val album: String = ""
) {
    fun formattedDuration(): String {
        if (durationMs <= 0) return "--:--"
        val totalSecs = durationMs / 1000
        val mins = totalSecs / 60
        val secs = totalSecs % 60
        return String.format(Locale.US, "%d:%02d", mins, secs)
    }
}
