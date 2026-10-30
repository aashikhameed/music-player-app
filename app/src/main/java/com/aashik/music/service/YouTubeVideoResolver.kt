package com.aashik.music.service

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

/**
 * Lightweight, zero-external-dependency YouTube Video Resolver.
 * Uses YouTube Music's InnerTube API (WEB_REMIX client, exactly like BitChord)
 * to resolve official music video IDs for any streaming track in ~200ms.
 * Enforces zero-bloat APK constraints (< 4.0 MB) with zero third-party libraries.
 */
object YouTubeVideoResolver {
    private const val TAG = "YouTubeVideoResolver"
    private val videoCache = ConcurrentHashMap<String, String>()

    suspend fun resolveVideoId(title: String, artist: String): String? = withContext(Dispatchers.IO) {
        val cleanTitle = title
            .replace("&amp;", "&")
            .replace(Regex("""(?i)\(From\s+["'][^"']+["']\)"""), "")
            .replace(Regex("""(?i)\(Reprise\)"""), "")
            .replace(Regex("""(?i)\(Official.*?\)"""), "")
            .replace(Regex("""\(.*?\)|\[.*?\]"""), "")
            .replace(Regex("""\s+"""), " ")
            .trim()
        val cleanArtist = artist
            .replace("&amp;", "&")
            .replace(Regex("""\(.*?\)|\[.*?\]"""), "")
            .replace(Regex("""\s+"""), " ")
            .trim()
        val cacheKey = "$cleanTitle::$cleanArtist".lowercase()
        videoCache[cacheKey]?.let { return@withContext it }

        val query = "$cleanTitle $cleanArtist official music video"
        var conn: HttpURLConnection? = null
        try {
            val url = URL("https://music.youtube.com/youtubei/v1/search")
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 8000
                readTimeout = 10000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
            }

            val escapedQuery = query.replace("\\", "\\\\").replace("\"", "\\\"")
            val payload = """{"context":{"client":{"clientName":"WEB_REMIX","clientVersion":"1.20260707.12.00","hl":"en","gl":"US"}},"query":"$escapedQuery"}"""
            conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }

            if (conn.responseCode in 200..299) {
                val response = conn.inputStream.bufferedReader().use { it.readText() }
                val match = Regex(""""videoId":\s*"([a-zA-Z0-9_-]{11})"""").find(response)
                val videoId = match?.groupValues?.get(1)
                if (!videoId.isNullOrBlank()) {
                    videoCache[cacheKey] = videoId
                    return@withContext videoId
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Video resolution failed for $cleanTitle: ${e.message}")
        } finally {
            try { conn?.disconnect() } catch (_: Exception) {}
        }
        null
    }

    suspend fun resolveVideoIdForSong(song: com.aashik.music.model.Song): String? {
        val directId = song.getYouTubeVideoId()
        if (!directId.isNullOrBlank()) return directId
        return resolveVideoId(song.title, song.artist)
    }

    data class YouTubeRadioTrack(
        val videoId: String,
        val title: String,
        val artist: String,
        val album: String,
        val durationMs: Long,
        val thumbnailUrl: String?
    )

    data class YouTubeCategoryItem(
        val id: String,
        val title: String,
        val subtitle: String,
        val artworkUrl: String,
        val type: String
    )

    data class YouTubeCategoryShelves(
        val songs: List<YouTubeRadioTrack>,
        val featuredPlaylists: List<YouTubeCategoryItem>,
        val communityPlaylists: List<YouTubeCategoryItem>,
        val albums: List<YouTubeCategoryItem>
    )

    private fun extractRuns(obj: org.json.JSONObject?): String {
        if (obj == null) return ""
        val runs = obj.optJSONArray("runs") ?: return obj.optString("text", "")
        val sb = StringBuilder()
        for (i in 0 until runs.length()) {
            sb.append(runs.optJSONObject(i)?.optString("text", "") ?: "")
        }
        return sb.toString().trim()
    }

    private fun extractThumbnail(obj: org.json.JSONObject?): String? {
        if (obj == null) return null
        val thumbs = obj.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
            ?: obj.optJSONObject("musicThumbnailRenderer")?.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
            ?: obj.optJSONArray("thumbnails")
        if (thumbs != null && thumbs.length() > 0) {
            return thumbs.optJSONObject(thumbs.length() - 1)?.optString("url")
        }
        return null
    }

    /**
     * Fetches authentic YouTube Music category shelves (Songs, Featured playlists, Community playlists, Albums)
     * directly matching BitChord's Innertube explore browse endpoint.
     */
    suspend fun fetchCategoryShelves(params: String): YouTubeCategoryShelves? = withContext(Dispatchers.IO) {
        if (params.isBlank()) return@withContext null
        var conn: HttpURLConnection? = null
        try {
            val url = URL("https://music.youtube.com/youtubei/v1/browse")
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 8000
                readTimeout = 10000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                setRequestProperty("X-YouTube-Client-Name", "67")
                setRequestProperty("X-YouTube-Client-Version", "1.20260707.12.00")
            }

            val payload = """{"context":{"client":{"clientName":"WEB_REMIX","clientVersion":"1.20260707.12.00","hl":"en","gl":"IN"}},"browseId":"FEmusic_moods_and_genres_category","params":"$params"}"""
            conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }

            if (conn.responseCode in 200..299) {
                val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                val root = org.json.JSONObject(responseText)
                val sections = root
                    .optJSONObject("contents")
                    ?.optJSONObject("singleColumnBrowseResultsRenderer")
                    ?.optJSONArray("tabs")
                    ?.optJSONObject(0)
                    ?.optJSONObject("tabRenderer")
                    ?.optJSONObject("content")
                    ?.optJSONObject("sectionListRenderer")
                    ?.optJSONArray("contents") ?: return@withContext null

                val songs = ArrayList<YouTubeRadioTrack>()
                val featured = ArrayList<YouTubeCategoryItem>()
                val community = ArrayList<YouTubeCategoryItem>()
                val albums = ArrayList<YouTubeCategoryItem>()

                for (s in 0 until sections.length()) {
                    val sec = sections.optJSONObject(s) ?: continue
                    val shelf = sec.optJSONObject("musicCarouselShelfRenderer")
                        ?: sec.optJSONObject("musicShelfRenderer") ?: continue

                    val header = shelf.optJSONObject("header")?.optJSONObject("musicCarouselShelfBasicHeaderRenderer")
                    val shelfTitle = extractRuns(header?.optJSONObject("title")).ifBlank {
                        extractRuns(shelf.optJSONObject("title"))
                    }
                    val shelfTitleLower = shelfTitle.lowercase()
                    val contents = shelf.optJSONArray("contents") ?: continue

                    for (i in 0 until contents.length()) {
                        val item = contents.optJSONObject(i) ?: continue
                        val respItem = item.optJSONObject("musicResponsiveListItemRenderer")
                        val twoRow = item.optJSONObject("musicTwoRowItemRenderer")

                        if (respItem != null) {
                            val flexCols = respItem.optJSONArray("flexColumns")
                            val title = if (flexCols != null && flexCols.length() > 0) {
                                extractRuns(flexCols.optJSONObject(0)?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")?.optJSONObject("text"))
                            } else ""
                            val artist = if (flexCols != null && flexCols.length() > 1) {
                                extractRuns(flexCols.optJSONObject(1)?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")?.optJSONObject("text"))
                            } else ""
                            val thumb = extractThumbnail(respItem.optJSONObject("thumbnail"))
                            val overlay = respItem.optJSONObject("overlay")?.optJSONObject("musicItemThumbnailOverlayRenderer")
                            val playEp = overlay?.optJSONObject("content")?.optJSONObject("musicPlayButtonRenderer")?.optJSONObject("playNavigationEndpoint")?.optJSONObject("watchEndpoint")
                            val vid = playEp?.optString("videoId")?.ifBlank { null }
                                ?: respItem.optJSONObject("navigationEndpoint")?.optJSONObject("watchEndpoint")?.optString("videoId")?.ifBlank { null }

                            if (!vid.isNullOrBlank() && title.isNotBlank()) {
                                songs.add(
                                    YouTubeRadioTrack(
                                        videoId = vid,
                                        title = title,
                                        artist = artist.ifBlank { "Unknown Artist" },
                                        album = "YouTube Music",
                                        durationMs = 0L,
                                        thumbnailUrl = thumb
                                    )
                                )
                            }
                        } else if (twoRow != null) {
                            val title = extractRuns(twoRow.optJSONObject("title"))
                            val subtitle = extractRuns(twoRow.optJSONObject("subtitle"))
                            val thumb = extractThumbnail(twoRow.optJSONObject("thumbnailRenderer"))
                            val browseEp = twoRow.optJSONObject("navigationEndpoint")?.optJSONObject("browseEndpoint")
                            val browseId = browseEp?.optString("browseId") ?: ""
                            val watchEp = twoRow.optJSONObject("navigationEndpoint")?.optJSONObject("watchEndpoint")
                            val vid = watchEp?.optString("videoId") ?: ""

                            if (browseId.isNotBlank()) {
                                val catItem = YouTubeCategoryItem(
                                    id = browseId,
                                    title = title,
                                    subtitle = subtitle,
                                    artworkUrl = thumb ?: "",
                                    type = if (browseId.startsWith("MPRE")) "album" else "playlist"
                                )
                                when {
                                    shelfTitleLower.contains("featured") -> featured.add(catItem)
                                    shelfTitleLower.contains("community") -> community.add(catItem)
                                    shelfTitleLower.contains("album") -> albums.add(catItem)
                                    else -> featured.add(catItem)
                                }
                            } else if (vid.isNotBlank()) {
                                songs.add(
                                    YouTubeRadioTrack(
                                        videoId = vid,
                                        title = title,
                                        artist = subtitle.ifBlank { "Unknown Artist" },
                                        album = "YouTube Music",
                                        durationMs = 0L,
                                        thumbnailUrl = thumb
                                    )
                                )
                            }
                        }
                    }
                }

                return@withContext YouTubeCategoryShelves(
                    songs = songs,
                    featuredPlaylists = featured,
                    communityPlaylists = community,
                    albums = albums
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "fetchCategoryShelves failed for $params: ${e.message}")
        } finally {
            try { conn?.disconnect() } catch (_: Exception) {}
        }
        null
    }

    /**
     * Fetches tracks from a YouTube Music Playlist or Album (e.g. Kollywood Hitlist, Karuppu album).
     */
    suspend fun fetchPlaylistOrAlbumTracks(browseId: String): List<YouTubeRadioTrack> = withContext(Dispatchers.IO) {
        if (browseId.isBlank()) return@withContext emptyList()
        val results = ArrayList<YouTubeRadioTrack>()
        var conn: HttpURLConnection? = null
        try {
            val url = URL("https://music.youtube.com/youtubei/v1/browse")
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 8000
                readTimeout = 10000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                setRequestProperty("X-YouTube-Client-Name", "67")
                setRequestProperty("X-YouTube-Client-Version", "1.20260707.12.00")
            }

            val payload = """{"context":{"client":{"clientName":"WEB_REMIX","clientVersion":"1.20260707.12.00","hl":"en","gl":"IN"}},"browseId":"$browseId"}"""
            conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }

            if (conn.responseCode in 200..299) {
                val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                val root = org.json.JSONObject(responseText)
                val secList = root
                    .optJSONObject("contents")
                    ?.optJSONObject("twoColumnBrowseResultsRenderer")
                    ?.optJSONObject("secondaryContents")
                    ?.optJSONObject("sectionListRenderer")
                    ?.optJSONArray("contents")

                if (secList != null) {
                    for (s in 0 until secList.length()) {
                        val sec = secList.optJSONObject(s) ?: continue
                        val shelf = sec.optJSONObject("musicPlaylistShelfRenderer")
                            ?: sec.optJSONObject("musicShelfRenderer") ?: continue
                        val contents = shelf.optJSONArray("contents") ?: continue

                        for (i in 0 until contents.length()) {
                            val it = contents.optJSONObject(i) ?: continue
                            val respItem = it.optJSONObject("musicResponsiveListItemRenderer") ?: continue
                            val flexCols = respItem.optJSONArray("flexColumns")
                            val title = if (flexCols != null && flexCols.length() > 0) {
                                extractRuns(flexCols.optJSONObject(0)?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")?.optJSONObject("text"))
                            } else ""
                            val artist = if (flexCols != null && flexCols.length() > 1) {
                                extractRuns(flexCols.optJSONObject(1)?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")?.optJSONObject("text"))
                            } else ""
                            val overlay = respItem.optJSONObject("overlay")?.optJSONObject("musicItemThumbnailOverlayRenderer")
                            val playEp = overlay?.optJSONObject("content")?.optJSONObject("musicPlayButtonRenderer")?.optJSONObject("playNavigationEndpoint")?.optJSONObject("watchEndpoint")
                            val vid = playEp?.optString("videoId")?.ifBlank { null }
                                ?: respItem.optJSONObject("navigationEndpoint")?.optJSONObject("watchEndpoint")?.optString("videoId")?.ifBlank { null }
                            val thumb = extractThumbnail(respItem.optJSONObject("thumbnail"))

                            if (!vid.isNullOrBlank() && title.isNotBlank()) {
                                results.add(
                                    YouTubeRadioTrack(
                                        videoId = vid,
                                        title = title,
                                        artist = artist.ifBlank { "Unknown Artist" },
                                        album = "YouTube Music",
                                        durationMs = 0L,
                                        thumbnailUrl = thumb
                                    )
                                )
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "fetchPlaylistOrAlbumTracks failed for $browseId: ${e.message}")
        } finally {
            try { conn?.disconnect() } catch (_: Exception) {}
        }
        results
    }

    /**
     * Fetches up to 50 algorithmic radio recommendation tracks for a seed video
     * directly from YouTube Music's RDAMVM watch queue endpoint (exactly as BitChord does).
     */
    suspend fun fetchRadioQueue(videoId: String): List<YouTubeRadioTrack> = withContext(Dispatchers.IO) {
        val results = ArrayList<YouTubeRadioTrack>()
        var conn: HttpURLConnection? = null
        try {
            val url = URL("https://music.youtube.com/youtubei/v1/next")
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 8000
                readTimeout = 10000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
            }

            val payload = """{"context":{"client":{"clientName":"WEB_REMIX","clientVersion":"1.20260707.12.00","hl":"en","gl":"US"}},"videoId":"$videoId","playlistId":"RDAMVM$videoId","isAudioOnly":true}"""
            conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }

            if (conn.responseCode in 200..299) {
                val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                val root = org.json.JSONObject(responseText)
                val contents = root
                    .optJSONObject("contents")
                    ?.optJSONObject("singleColumnMusicWatchNextResultsRenderer")
                    ?.optJSONObject("tabbedRenderer")
                    ?.optJSONObject("watchNextTabbedResultsRenderer")
                    ?.optJSONArray("tabs")
                    ?.optJSONObject(0)
                    ?.optJSONObject("tabRenderer")
                    ?.optJSONObject("content")
                    ?.optJSONObject("musicQueueRenderer")
                    ?.optJSONObject("content")
                    ?.optJSONObject("playlistPanelRenderer")
                    ?.optJSONArray("contents")

                if (contents != null) {
                    for (i in 0 until contents.length()) {
                        val wrapper = contents.optJSONObject(i) ?: continue
                        val renderer = wrapper.optJSONObject("playlistPanelVideoRenderer")
                            ?: wrapper.optJSONObject("playlistPanelVideoWrapperRenderer")
                                ?.optJSONObject("primaryRenderer")
                                ?.optJSONObject("playlistPanelVideoRenderer")
                            ?: continue

                        val vid = renderer.optString("videoId")
                        if (vid.isBlank()) continue

                        val title = renderer.optJSONObject("title")
                            ?.optJSONArray("runs")
                            ?.optJSONObject(0)
                            ?.optString("text", "") ?: ""
                        if (title.isBlank()) continue

                        val bylineRuns = renderer.optJSONObject("shortBylineText")?.optJSONArray("runs")
                        val artistBuilder = StringBuilder()
                        if (bylineRuns != null) {
                            for (j in 0 until bylineRuns.length()) {
                                artistBuilder.append(bylineRuns.optJSONObject(j)?.optString("text", "") ?: "")
                            }
                        }
                        val artist = artistBuilder.toString().trim()

                        val lengthText = renderer.optJSONObject("lengthText")
                            ?.optJSONArray("runs")
                            ?.optJSONObject(0)
                            ?.optString("text", "") ?: ""
                        val durationMs = parseDurationMs(lengthText)

                        val thumbnails = renderer.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
                        val thumbnail = if (thumbnails != null && thumbnails.length() > 0) {
                            thumbnails.optJSONObject(thumbnails.length() - 1)?.optString("url")
                        } else null

                        results.add(
                            YouTubeRadioTrack(
                                videoId = vid,
                                title = title,
                                artist = artist.ifBlank { "Unknown Artist" },
                                album = "YouTube Music Radio",
                                durationMs = durationMs,
                                thumbnailUrl = thumbnail
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "fetchRadioQueue failed for $videoId: ${e.message}")
        } finally {
            try { conn?.disconnect() } catch (_: Exception) {}
        }
        results
    }

    private fun parseDurationMs(durationStr: String): Long {
        val parts = durationStr.split(':')
        return when (parts.size) {
            2 -> {
                val min = parts[0].trim().toLongOrNull() ?: 0L
                val sec = parts[1].trim().toLongOrNull() ?: 0L
                (min * 60 + sec) * 1000L
            }
            3 -> {
                val hr = parts[0].trim().toLongOrNull() ?: 0L
                val min = parts[1].trim().toLongOrNull() ?: 0L
                val sec = parts[2].trim().toLongOrNull() ?: 0L
                (hr * 3600 + min * 60 + sec) * 1000L
            }
            else -> 0L
        }
    }
}
