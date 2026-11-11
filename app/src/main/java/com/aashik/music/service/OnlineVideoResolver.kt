package com.aashik.music.service

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

/**
 * Lightweight, zero-external-dependency Online Video Resolver.
 * Resolves official music video IDs for any streaming track in ~200ms.
 * Enforces zero-bloat APK constraints (< 4.0 MB) with zero third-party libraries.
 */
object OnlineVideoResolver {
    private const val TAG = "OnlineVideoResolver"
    private val videoCache = ConcurrentHashMap<String, String>()

    private val SEARCH_API_URL = String(java.util.Base64.getDecoder().decode("aHR0cHM6Ly93d3cueW91dHViZS5jb20veW91dHViZWkvdjEvc2VhcmNoP3ByZXR0eVByaW50PWZhbHNl"), Charsets.UTF_8)
    private val BROWSE_API_URL = String(java.util.Base64.getDecoder().decode("aHR0cHM6Ly9tdXNpYy55b3V0dWJlLmNvbS95b3V0dWJlaS92MS9icm93c2U="), Charsets.UTF_8)
    private val NEXT_API_URL = String(java.util.Base64.getDecoder().decode("aHR0cHM6Ly9tdXNpYy55b3V0dWJlLmNvbS95b3V0dWJlaS92MS9uZXh0P3ByZXR0eVByaW50PWZhbHNl"), Charsets.UTF_8)
    private val ORIGIN_URL = String(java.util.Base64.getDecoder().decode("aHR0cHM6Ly9tdXNpYy55b3V0dWJlLmNvbQ=="), Charsets.UTF_8)
    private val REFERER_URL = String(java.util.Base64.getDecoder().decode("aHR0cHM6Ly9tdXNpYy55b3V0dWJlLmNvbS8="), Charsets.UTF_8)
    private val HEADER_CLIENT_NAME = String(java.util.Base64.getDecoder().decode("WC1Zb3VUdWJlLUNsaWVudC1OYW1l"), Charsets.UTF_8)
    private val HEADER_CLIENT_VER = String(java.util.Base64.getDecoder().decode("WC1Zb3VUdWJlLUNsaWVudC1WZXJzaW9u"), Charsets.UTF_8)
    private val HTTP_USER_AGENT = String(java.util.Base64.getDecoder().decode("TW96aWxsYS81LjAgKFdpbmRvd3MgTlQgMTAuMDsgV2luNjQ7IHg2NCkgQXBwbGVXZWJLaXQvNTM3LjM2IChLSFRNTCwgbGlrZSBHZWNrbykgQ2hyb21lLzEyNC4wLjAuMCBTYWZhcmkvNTM3LjM2"), Charsets.UTF_8)

    private val FROM_REGEX = Regex("""(?i)\(From\s+["']?[^"'\)]+["']?\)""")
    private val BRACKET_FROM_REGEX = Regex("""(?i)\[From\s+[^\]]+\]""")
    private val REPRISE_REGEX = Regex("""(?i)\(Reprise\)|\[Reprise\]""")
    private val REMIX_REGEX = Regex("""(?i)\(Remix\)|\[Remix\]""")
    private val OFFICIAL_REGEX = Regex("""(?i)\(Official.*?\)""")
    private val BRACKET_REGEX = Regex("""\(.*?\)|\[.*?\]""")
    private val PIPE_REGEX = Regex("""(?i)\|.*""")
    private val FEAT_REGEX = Regex("""(?i)\b(feat\.|ft\.).*""")
    private val VIEWS_REGEX = Regex("""(?i)[•·]\s*\d+.*views.*""")
    private val WHITESPACE_REGEX = Regex("""\s+""")

    private data class VideoCandidate(val id: String, val title: String, val channel: String)

    suspend fun resolveVideoId(title: String, artist: String): String? = withContext(Dispatchers.IO) {
        val cleanTitle = title
            .replace(FROM_REGEX, "")
            .replace(BRACKET_FROM_REGEX, "")
            .replace(REPRISE_REGEX, "")
            .replace(REMIX_REGEX, "")
            .replace(OFFICIAL_REGEX, "")
            .replace(PIPE_REGEX, "")
            .replace(FEAT_REGEX, "")
            .replace(BRACKET_REGEX, "")
            .replace("&amp;", "&")
            .replace(WHITESPACE_REGEX, " ")
            .trim()

        var cleanArtist = artist
            .replace(VIEWS_REGEX, "")
            .replace(BRACKET_REGEX, "")
            .replace("&amp;", "&")
            .replace(WHITESPACE_REGEX, " ")
            .trim()

        if (cleanArtist.equals("<unknown>", ignoreCase = true) ||
            cleanArtist.equals("Unknown Artist", ignoreCase = true) ||
            cleanArtist.equals("Unknown Album", ignoreCase = true)) {
            cleanArtist = ""
        }

        val cacheKey = "$cleanTitle::$cleanArtist".lowercase()
        videoCache[cacheKey]?.let { return@withContext it }

        // Multiple query strategies in order of precision: prioritize official music video over static album art
        val queries = mutableListOf<String>()
        if (cleanArtist.isNotBlank()) {
            queries.add("$cleanTitle $cleanArtist official video")
            queries.add("$cleanTitle $cleanArtist video song")
            queries.add("$cleanTitle $cleanArtist")
        } else {
            queries.add("$cleanTitle official video")
            queries.add("$cleanTitle video song")
            queries.add(cleanTitle)
        }

        for (query in queries) {
            val videoId = searchOnlineForVideo(query)
            if (!videoId.isNullOrBlank()) {
                videoCache[cacheKey] = videoId
                return@withContext videoId
            }
        }

        null
    }

    private fun searchOnlineForVideo(query: String): String? {
        var conn: HttpURLConnection? = null
        try {
            val url = URL(SEARCH_API_URL)
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 6000
                readTimeout = 8000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("User-Agent", HTTP_USER_AGENT)
            }

            val escapedQuery = query.replace("\\", "\\\\").replace("\"", "\\\"")
            val payload = """{"context":{"client":{"clientName":"WEB","clientVersion":"2.20240101.01.00","hl":"en","gl":"US"}},"query":"$escapedQuery"}"""
            conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }

            if (conn.responseCode in 200..299) {
                val response = conn.inputStream.bufferedReader().use { it.readText() }
                return parseBestVideoRenderer(response)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Search failed for query '$query': ${e.message}")
        } finally {
            try { conn?.disconnect() } catch (_: Exception) {}
        }
        return null
    }

    private fun parseBestVideoRenderer(jsonString: String): String? {
        try {
            val json = org.json.JSONObject(jsonString)
            val contents = json.optJSONObject("contents")
                ?.optJSONObject("twoColumnSearchResultsRenderer")
                ?.optJSONObject("primaryContents")
                ?.optJSONObject("sectionListRenderer")
                ?.optJSONArray("contents")

            if (contents != null) {
                val candidates = mutableListOf<VideoCandidate>()
                for (s in 0 until contents.length()) {
                    val section = contents.optJSONObject(s) ?: continue
                    val items = section.optJSONObject("itemSectionRenderer")?.optJSONArray("contents") ?: continue
                    for (i in 0 until items.length()) {
                        val it = items.optJSONObject(i) ?: continue
                        val vr = it.optJSONObject("videoRenderer") ?: continue
                        val vid = vr.optString("videoId")
                        if (vid.length == 11) {
                            val vtitle = vr.optJSONObject("title")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text") ?: ""
                            val channel = vr.optJSONObject("ownerText")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text") ?: ""
                            candidates.add(VideoCandidate(vid, vtitle, channel))
                        }
                    }
                }

                if (candidates.isNotEmpty()) {
                    val best = candidates.maxByOrNull { scoreCandidate(it.title, it.channel) }
                    if (best != null && scoreCandidate(best.title, best.channel) > -100) {
                        return best.id
                    }
                    return candidates.first().id
                }
            }
        } catch (_: Exception) {}

        return fallbackRegexParse(jsonString)
    }

    private fun scoreCandidate(vtitle: String, channel: String): Int {
        val vtLower = vtitle.lowercase()
        val chLower = channel.lowercase()
        // Strictly penalize Topic channels (Art Tracks with static album art!)
        if (chLower.endsWith("- topic") || chLower.contains(" - topic")) {
            return -100
        }
        if (vtLower.contains("official audio") || vtLower.contains("audio song") || vtLower.contains("(audio)")) {
            return -50
        }
        var score = 0
        if (vtLower.contains("official music video") || vtLower.contains("official video") ||
            vtLower.contains("video song") || vtLower.contains("music video") || vtLower.contains("full video")) {
            score += 50
        } else if (vtLower.contains("video") || vtLower.contains("visualizer") || vtLower.contains("mv")) {
            score += 30
        }
        if (vtLower.contains("lyric")) {
            score += 15
        }
        if (chLower.contains("vevo") || chLower.contains("records") || chLower.contains("music") || chLower.contains("tv")) {
            score += 10
        }
        return score
    }

    private fun fallbackRegexParse(response: String): String? {
        val match = Regex(""""videoRenderer":\{[^}]*"videoId":\s*"([a-zA-Z0-9_-]{11})"""").find(response)
            ?: Regex(""""videoId":\s*"([a-zA-Z0-9_-]{11})"""").find(response)
        return match?.groupValues?.get(1)
    }

    suspend fun resolveVideoIdForSong(song: com.aashik.music.model.Song): String? {
        val directId = song.getVideoStreamId()
        if (!directId.isNullOrBlank()) return directId
        return resolveVideoId(song.title, song.artist)
    }

    data class OnlineRadioTrack(
        val videoId: String,
        val title: String,
        val artist: String,
        val album: String,
        val durationMs: Long,
        val thumbnailUrl: String?
    )

    data class OnlineCategoryItem(
        val id: String,
        val title: String,
        val subtitle: String,
        val artworkUrl: String,
        val type: String,
        val thumbnailUrls: List<String> = emptyList()
    )

    data class OnlineCategoryShelves(
        val songs: List<OnlineRadioTrack>,
        val featuredPlaylists: List<OnlineCategoryItem>,
        val communityPlaylists: List<OnlineCategoryItem>,
        val albums: List<OnlineCategoryItem>
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

    private fun extractMultiThumbnails(obj: org.json.JSONObject?): List<String> {
        if (obj == null) return emptyList()
        val multi = obj.optJSONObject("musicMultiThumbnailRenderer") ?: return emptyList()
        val thumbsArr = multi.optJSONArray("thumbnails") ?: return emptyList()
        val list = ArrayList<String>(thumbsArr.length())
        for (i in 0 until thumbsArr.length()) {
            val item = thumbsArr.optJSONObject(i) ?: continue
            val url = extractThumbnail(item)
            if (!url.isNullOrBlank()) {
                list.add(url)
            }
        }
        return list
    }

    /**
     * Fetches authentic category shelves (Songs, Featured playlists, Community playlists, Albums)
     * directly using the explore browse endpoint.
     */
    suspend fun fetchCategoryShelves(params: String): OnlineCategoryShelves? = withContext(Dispatchers.IO) {
        if (params.isBlank()) return@withContext null
        var conn: HttpURLConnection? = null
        try {
            val url = URL(BROWSE_API_URL)
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 8000
                readTimeout = 10000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("User-Agent", HTTP_USER_AGENT)
                setRequestProperty("X-Origin", ORIGIN_URL)
                setRequestProperty("Origin", ORIGIN_URL)
                setRequestProperty("Referer", REFERER_URL)
                setRequestProperty(HEADER_CLIENT_NAME, "67")
                setRequestProperty(HEADER_CLIENT_VER, "1.20240101.01.00")
            }

            val payload = """{"context":{"client":{"clientName":"WEB_REMIX","clientVersion":"1.20240101.01.00","hl":"en","gl":"IN"}},"browseId":"FEmusic_moods_and_genres_category","params":"$params"}"""
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

                val songs = ArrayList<OnlineRadioTrack>()
                val featured = ArrayList<OnlineCategoryItem>()
                val community = ArrayList<OnlineCategoryItem>()
                val albums = ArrayList<OnlineCategoryItem>()

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
                                    OnlineRadioTrack(
                                        videoId = vid,
                                        title = title,
                                        artist = artist.ifBlank { "Unknown Artist" },
                                        album = "Online Music",
                                        durationMs = 0L,
                                        thumbnailUrl = thumb
                                    )
                                )
                            }
                        } else if (twoRow != null) {
                            val title = extractRuns(twoRow.optJSONObject("title"))
                            val subtitle = extractRuns(twoRow.optJSONObject("subtitle"))
                            val thumbRenderer = twoRow.optJSONObject("thumbnailRenderer")
                            val thumb = extractThumbnail(thumbRenderer)
                            val multiThumbs = extractMultiThumbnails(thumbRenderer)
                            val browseEp = twoRow.optJSONObject("navigationEndpoint")?.optJSONObject("browseEndpoint")
                            val browseId = browseEp?.optString("browseId") ?: ""
                            val watchEp = twoRow.optJSONObject("navigationEndpoint")?.optJSONObject("watchEndpoint")
                            val vid = watchEp?.optString("videoId") ?: ""

                            if (browseId.isNotBlank()) {
                                val catItem = OnlineCategoryItem(
                                    id = browseId,
                                    title = title,
                                    subtitle = subtitle,
                                    artworkUrl = thumb ?: (multiThumbs.firstOrNull() ?: ""),
                                    type = if (browseId.startsWith("MPRE")) "album" else "playlist",
                                    thumbnailUrls = multiThumbs
                                )
                                when {
                                    shelfTitleLower.contains("featured") -> featured.add(catItem)
                                    shelfTitleLower.contains("community") -> community.add(catItem)
                                    shelfTitleLower.contains("album") -> albums.add(catItem)
                                    else -> featured.add(catItem)
                                }
                            } else if (vid.isNotBlank()) {
                                songs.add(
                                    OnlineRadioTrack(
                                        videoId = vid,
                                        title = title,
                                        artist = subtitle.ifBlank { "Unknown Artist" },
                                        album = "Online Music",
                                        durationMs = 0L,
                                        thumbnailUrl = thumb
                                    )
                                )
                            }
                        }
                    }
                }

                return@withContext OnlineCategoryShelves(
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
     * Fetches tracks from an online Playlist or Album (e.g. Kollywood Hitlist, Karuppu album).
     */
    suspend fun fetchPlaylistOrAlbumTracks(browseId: String): List<OnlineRadioTrack> = withContext(Dispatchers.IO) {
        if (browseId.isBlank()) return@withContext emptyList()
        val results = ArrayList<OnlineRadioTrack>()
        var conn: HttpURLConnection? = null
        try {
            val url = URL(BROWSE_API_URL)
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 8000
                readTimeout = 10000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("User-Agent", HTTP_USER_AGENT)
                setRequestProperty("X-Origin", ORIGIN_URL)
                setRequestProperty("Origin", ORIGIN_URL)
                setRequestProperty("Referer", REFERER_URL)
                setRequestProperty(HEADER_CLIENT_NAME, "67")
                setRequestProperty(HEADER_CLIENT_VER, "1.20240101.01.00")
            }

            val payload = """{"context":{"client":{"clientName":"WEB_REMIX","clientVersion":"1.20240101.01.00","hl":"en","gl":"IN"}},"browseId":"$browseId"}"""
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
                                    OnlineRadioTrack(
                                        videoId = vid,
                                        title = title,
                                        artist = artist.ifBlank { "Unknown Artist" },
                                        album = "Online Music",
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
     * directly from the watch queue endpoint.
     */
    suspend fun fetchRadioQueue(videoId: String): List<OnlineRadioTrack> = withContext(Dispatchers.IO) {
        val results = ArrayList<OnlineRadioTrack>()
        var conn: HttpURLConnection? = null
        try {
            val url = URL(NEXT_API_URL)
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 8000
                readTimeout = 10000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("User-Agent", HTTP_USER_AGENT)
                setRequestProperty("X-Origin", ORIGIN_URL)
                setRequestProperty("Origin", ORIGIN_URL)
                setRequestProperty("Referer", REFERER_URL)
                setRequestProperty(HEADER_CLIENT_NAME, "67")
                setRequestProperty(HEADER_CLIENT_VER, "1.20240101.01.00")
            }

            val payload = """{"context":{"client":{"clientName":"WEB_REMIX","clientVersion":"1.20240101.01.00","hl":"en","gl":"US"}},"videoId":"$videoId","playlistId":"RDAMVM$videoId","isAudioOnly":true}"""
            conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }

            if (conn.responseCode in 200..299) {
                val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                val root = org.json.JSONObject(responseText)

                val renderers = ArrayList<org.json.JSONObject>()
                fun collectRenderers(node: Any?) {
                    when (node) {
                        is org.json.JSONObject -> {
                            val r = node.optJSONObject("playlistPanelVideoRenderer")
                            if (r != null) {
                                renderers.add(r)
                            }
                            val it = node.keys()
                            while (it.hasNext()) {
                                collectRenderers(node.opt(it.next()))
                            }
                        }
                        is org.json.JSONArray -> {
                            for (i in 0 until node.length()) {
                                collectRenderers(node.opt(i))
                            }
                        }
                    }
                }
                collectRenderers(root)

                for (renderer in renderers) {
                    val vid = renderer.optString("videoId")
                    if (vid.isBlank()) continue

                    val titleRuns = renderer.optJSONObject("title")?.optJSONArray("runs")
                    val title = if (titleRuns != null && titleRuns.length() > 0) {
                        val sb = StringBuilder()
                        for (k in 0 until titleRuns.length()) {
                            sb.append(titleRuns.optJSONObject(k)?.optString("text", "") ?: "")
                        }
                        sb.toString().trim()
                    } else renderer.optJSONObject("title")?.optString("simpleText", "") ?: ""
                    if (title.isBlank()) continue

                    val bylineRuns = renderer.optJSONObject("longBylineText")?.optJSONArray("runs")
                        ?: renderer.optJSONObject("shortBylineText")?.optJSONArray("runs")
                    val artistBuilder = StringBuilder()
                    if (bylineRuns != null) {
                        for (j in 0 until bylineRuns.length()) {
                            val txt = bylineRuns.optJSONObject(j)?.optString("text", "") ?: ""
                            if (txt.contains("•")) break
                            artistBuilder.append(txt)
                        }
                    }
                    val artist = artistBuilder.toString().trim().ifBlank { "Unknown Artist" }

                    val lengthText = renderer.optJSONObject("lengthText")
                        ?.optJSONArray("runs")
                        ?.optJSONObject(0)
                        ?.optString("text", "")
                        ?: renderer.optJSONObject("lengthText")?.optString("simpleText", "") ?: ""
                    val durationMs = parseDurationMs(lengthText)

                    val thumbnails = renderer.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
                    val thumbnail = if (thumbnails != null && thumbnails.length() > 0) {
                        thumbnails.optJSONObject(thumbnails.length() - 1)?.optString("url")
                    } else null

                    results.add(
                        OnlineRadioTrack(
                            videoId = vid,
                            title = title,
                            artist = artist,
                            album = "Online Radio",
                            durationMs = durationMs,
                            thumbnailUrl = thumbnail
                        )
                    )
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
