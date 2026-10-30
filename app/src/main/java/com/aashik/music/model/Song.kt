package com.aashik.music.model

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Immutable
@Entity(
    tableName = "songs",
    indices = [Index(value = ["title"])]
)
data class Song(
    @PrimaryKey(autoGenerate = false)
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val path: String,
    val artworkUrl: String? = null
) {
    @androidx.room.Ignore
    @Volatile
    private var cachedBadge: String? = null

    @androidx.room.Ignore
    @Volatile
    private var cachedExt: String? = null

    fun isStream(): Boolean {
        return path.startsWith("http://", ignoreCase = true) || path.startsWith("https://", ignoreCase = true)
    }

    fun getFileExtension(): String {
        cachedExt?.let { return it }
        if (isStream()) {
            val ext = if (duration <= 0) "LIVE" else "STREAM"
            cachedExt = ext
            return ext
        }
        val ext = path.substringAfterLast('.', "").uppercase().take(4)
        cachedExt = ext
        return ext
    }

    fun getBadgeLabel(): String {
        cachedBadge?.let { return it }
        if (isStream() && duration <= 0) {
            val label = "LIVE • STREAM"
            cachedBadge = label
            return label
        }
        val ext = getFileExtension()
        val totalSec = (duration / 1000).coerceAtLeast(0)
        val m = totalSec / 60
        val s = totalSec % 60
        val minStr = if (m < 10) "0$m" else m.toString()
        val secStr = if (s < 10) "0$s" else s.toString()
        val label = if (ext.isNotEmpty()) "$ext • $minStr:$secStr" else "$minStr:$secStr"
        cachedBadge = label
        return label
    }
}

@Immutable
data class NavigatingTrack(
    val song: Song? = null,
    val direction: Int = 1,
    val transitionId: Long = 0L
)

