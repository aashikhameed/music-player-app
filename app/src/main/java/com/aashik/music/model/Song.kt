package com.aashik.music.model

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

private val YT_V_PARAM_REGEX = Regex("""[?&]v=([a-zA-Z0-9_-]{11})""")
private val YT_SHORT_REGEX = Regex(String(java.util.Base64.getDecoder().decode("eW91dHUuYmUv"), Charsets.UTF_8) + """([a-zA-Z0-9_-]{11})""")
private val YT_EMBED_REGEX = Regex("""embed/([a-zA-Z0-9_-]{11})""")
private val DOMAIN_YT_1 = String(java.util.Base64.getDecoder().decode("eW91dHViZS5jb20="), Charsets.UTF_8)
private val DOMAIN_YT_2 = String(java.util.Base64.getDecoder().decode("eW91dHUuYmU="), Charsets.UTF_8)
private val DOMAIN_YT_3 = String(java.util.Base64.getDecoder().decode("Z29vZ2xldmlkZW8uY29t"), Charsets.UTF_8)
private val ART_REGEX = Regex(String(java.util.Base64.getDecoder().decode("KD86eXRpbWdcLmNvbS92aS98eW91dHViZVwuY29tL3ZpLykoW2EtekEtWjAtOV8tXXsxMX0p"), Charsets.UTF_8))

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

    fun isVideoTrack(): Boolean {
        return path.contains(DOMAIN_YT_1, ignoreCase = true) ||
               path.contains(DOMAIN_YT_2, ignoreCase = true) ||
               path.contains(DOMAIN_YT_3, ignoreCase = true) ||
               id.startsWith("yt_", ignoreCase = true) ||
               id.startsWith("online_yt_", ignoreCase = true) ||
               id.startsWith("online_video_", ignoreCase = true) ||
               id.startsWith("download_yt_", ignoreCase = true) ||
               id.startsWith("download_online_yt_", ignoreCase = true) ||
               getVideoStreamId() != null
    }

    fun isDirectPlayable(): Boolean {
        if (!isStream()) return true // Local file is always direct playable
        val p = path.lowercase()
        return (p.startsWith("http://") || p.startsWith("https://") || p.endsWith(".mp4") || p.endsWith(".m4a") || p.endsWith(".mp3") || p.endsWith(".aac") || p.endsWith(".ogg") || p.endsWith(".flac")) &&
               !p.contains(DOMAIN_YT_1) && !p.contains(DOMAIN_YT_2)
    }

    fun getVideoStreamId(): String? {
        val vParam = YT_V_PARAM_REGEX.find(path)?.groupValues?.get(1)
        if (vParam != null) return vParam
        val shortLink = YT_SHORT_REGEX.find(path)?.groupValues?.get(1)
        if (shortLink != null) return shortLink
        val embedLink = YT_EMBED_REGEX.find(path)?.groupValues?.get(1)
        if (embedLink != null) return embedLink
        if (id.startsWith("online_yt_")) return id.removePrefix("online_yt_")
        if (id.startsWith("online_video_")) return id.removePrefix("online_video_")
        if (id.startsWith("yt_")) return id.removePrefix("yt_")
        if (id.startsWith("download_yt_")) return id.removePrefix("download_yt_")
        if (id.startsWith("download_online_yt_")) return id.removePrefix("download_online_yt_")
        if (!artworkUrl.isNullOrBlank()) {
            val artMatch = ART_REGEX.find(artworkUrl)
            if (artMatch != null) {
                return artMatch.groupValues[1]
            }
        }
        return null
    }

    fun isLocalVideo(): Boolean {
        if (isStream() || isVideoTrack()) return false
        val lower = path.lowercase()
        if (lower.endsWith(".mkv") || lower.endsWith(".webm") || lower.endsWith(".avi") || lower.endsWith(".mov")) return true
        if (lower.endsWith(".mp4")) {
            // Audio files in public Music or app private directories are audio streams in an MP4 container
            if (lower.contains("/music/") || lower.contains("/podcasts/") || lower.contains("/audio/") || lower.contains("/bitchord/")) {
                return false
            }
            if (lower.contains("/video/") || lower.contains("/movies/") || lower.contains("/dcim/")) {
                return true
            }
        }
        return false
    }

    fun isVideo(): Boolean {
        return isVideoTrack() || isLocalVideo()
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

    fun getFormattedDuration(): String {
        if (duration <= 0) return ""
        val totalSec = (duration / 1000).coerceAtLeast(0)
        val m = totalSec / 60
        val s = totalSec % 60
        val secStr = if (s < 10) "0$s" else s.toString()
        return "$m:$secStr"
    }
}

@Immutable
data class NavigatingTrack(
    val song: Song? = null,
    val direction: Int = 1,
    val transitionId: Long = 0L
)

