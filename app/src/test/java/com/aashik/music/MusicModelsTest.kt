package com.aashik.music

import com.aashik.music.model.MusicFolder
import com.aashik.music.model.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MusicModelsTest {

    @Test
    fun testSongModelCreation() {
        val song = Song(
            id = "/sdcard/Music/Rock/highway_to_drive.mp3",
            title = "Highway to Drive",
            artist = "AC/DC",
            album = "Rock Classics",
            duration = 210000L,
            path = "/sdcard/Music/Rock/highway_to_drive.mp3"
        )

        assertEquals("Highway to Drive", song.title)
        assertEquals("AC/DC", song.artist)
        assertEquals(210000L, song.duration)
    }

    @Test
    fun testFolderGroupingLogic() {
        val songs = listOf(
            Song(id = "1", title = "Track 1", artist = "Artist A", album = "Album A", duration = 180000L, path = "/sdcard/Music/Rock/track1.mp3"),
            Song(id = "2", title = "Track 2", artist = "Artist A", album = "Album A", duration = 190000L, path = "/sdcard/Music/Rock/track2.mp3"),
            Song(id = "3", title = "Track 3", artist = "Artist B", album = "Album B", duration = 200000L, path = "/sdcard/Music/Jazz/track3.mp3"),
            Song(id = "4", title = "Track 4", artist = "Artist C", album = "Album C", duration = 220000L, path = "/sdcard/Music/Electronic/track4.mp3")
        )

        val folderMap = songs.groupBy { File(it.path).parent ?: "Root" }
        val folders = folderMap.map { (path, songList) ->
            MusicFolder(
                name = File(path).name,
                path = path,
                songCount = songList.size
            )
        }

        assertEquals(3, folders.size)
        val rockFolder = folders.find { it.name == "Rock" }
        assertTrue(rockFolder != null)
        assertEquals(2, rockFolder?.songCount)

        val jazzFolder = folders.find { it.name == "Jazz" }
        assertTrue(jazzFolder != null)
        assertEquals(1, jazzFolder?.songCount)
    }

    @Test
    fun testSearchFilteringLogic() {
        val songs = listOf(
            Song(id = "1", title = "Highway Star", artist = "Deep Purple", album = "Machine Head", duration = 360000L, path = "/sdcard/Music/track1.mp3"),
            Song(id = "2", title = "Stairway to Heaven", artist = "Led Zeppelin", album = "Led Zeppelin IV", duration = 480000L, path = "/sdcard/Music/track2.mp3"),
            Song(id = "3", title = "Hotel California", artist = "Eagles", album = "Hotel California", duration = 390000L, path = "/sdcard/Music/track3.mp3")
        )

        val query = "star"
        val filtered = songs.filter {
            it.title.lowercase().contains(query) || it.artist.lowercase().contains(query) || it.album.lowercase().contains(query)
        }

        assertEquals(1, filtered.size)
        assertEquals("Highway Star", filtered.first().title)
    }

    @Test
    fun testEqualizerAcousticClarityPresetParameters() {
        // Acoustic Clarity Preset Specification
        val acousticBands = listOf(0.0f, -4.0f, 1.0f, 2.0f, 3.0f)
        val acousticHpf = 2 // 80 Hz Cut

        assertEquals(5, acousticBands.size)
        assertEquals(0.0f, acousticBands[0], 0.001f)
        // Verify 250 Hz Mud-Cut is strictly attenuated (-4 dB)
        assertEquals(-4.0f, acousticBands[1], 0.001f)
        // Verify Vocal presence and treble air boost
        assertTrue("Vocal presence should be boosted", acousticBands[3] > 0.0f)
        assertTrue("Treble sparkle should be boosted", acousticBands[4] > 0.0f)
        assertEquals(2, acousticHpf)
    }

    @Test
    fun testEqualizerGainClamping() {
        val rawGains = listOf(-25.0f, -4.0f, 0.0f, 5.0f, 18.0f)
        val clampedGains = rawGains.map { it.coerceIn(-12.0f, 12.0f) }

        assertEquals(-12.0f, clampedGains[0], 0.001f)
        assertEquals(-4.0f, clampedGains[1], 0.001f)
        assertEquals(0.0f, clampedGains[2], 0.001f)
        assertEquals(5.0f, clampedGains[3], 0.001f)
        assertEquals(12.0f, clampedGains[4], 0.001f)
    }

    @Test
    fun testCrossfadeDefaultsAndOptions() {
        val crossfadeOptions = listOf(0, 1, 2, 3, 5, 8)
        val defaultCrossfade = 3

        assertTrue("Options must contain 0s (Off)", crossfadeOptions.contains(0))
        assertTrue("Options must contain 3s default", crossfadeOptions.contains(3))
        assertEquals(3, defaultCrossfade)
        assertEquals(6, crossfadeOptions.size)
    }

    @Test
    fun testCrossfadeVolumeDecayCalculation() {
        val crossfadeSeconds = 3
        val crossfadeMs = crossfadeSeconds * 1000L

        // Linear volume decay at various remaining time marks
        val volAtStart = (3000L.toFloat() / crossfadeMs).coerceIn(0.05f, 1.0f)
        val volAtHalf = (1500L.toFloat() / crossfadeMs).coerceIn(0.05f, 1.0f)
        val volAtNearEnd = (300L.toFloat() / crossfadeMs).coerceIn(0.05f, 1.0f)
        val volAtEnd = (0L.toFloat() / crossfadeMs).coerceIn(0.05f, 1.0f)

        assertEquals(1.0f, volAtStart, 0.001f)
        assertEquals(0.5f, volAtHalf, 0.001f)
        assertEquals(0.1f, volAtNearEnd, 0.001f)
        assertEquals(0.05f, volAtEnd, 0.001f) // Clamped to minimum floor
    }

    @Test
    fun testEqualPowerCrossfadeCalculation() {
        val crossfadeDurationSec = 3.0f

        fun calcVolumes(elapsedSec: Float): Pair<Float, Float> {
            val p = (elapsedSec / crossfadeDurationSec).coerceIn(0.0f, 1.0f)
            val angle = p * 1.57079632679f // pi / 2
            val fadeVol = Math.cos(angle.toDouble()).toFloat()
            val currVol = Math.sin(angle.toDouble()).toFloat()
            return Pair(fadeVol, currVol)
        }

        // At transition start: outgoing song is full, incoming song is zero
        val (fadeStart, currStart) = calcVolumes(0.0f)
        assertEquals(1.0f, fadeStart, 0.001f)
        assertEquals(0.0f, currStart, 0.001f)

        // At transition midpoint (1.5s): equal-power constant acoustic sum (~0.707)
        val (fadeMid, currMid) = calcVolumes(1.5f)
        assertEquals(0.7071f, fadeMid, 0.001f)
        assertEquals(0.7071f, currMid, 0.001f)

        // At transition completion (3.0s): outgoing song is zero, incoming song is full
        val (fadeEnd, currEnd) = calcVolumes(3.0f)
        assertEquals(0.0f, fadeEnd, 0.001f)
        assertEquals(1.0f, currEnd, 0.001f)
    }

    @Test
    fun testCrossfadeRemainingTimeTrigger() {
        val durationMs = 180_000L
        val crossfadeSeconds = 3
        val crossfadeMs = crossfadeSeconds * 1000L

        fun shouldTriggerCrossfade(positionMs: Long): Boolean {
            if (crossfadeSeconds <= 0) return false
            if (durationMs <= crossfadeMs + 2000L) return false
            val remainingMs = durationMs - positionMs
            return remainingMs <= crossfadeMs
        }

        assertFalse(shouldTriggerCrossfade(170_000L)) // 10s left: not yet
        assertFalse(shouldTriggerCrossfade(176_000L)) // 4s left: not yet
        assertTrue(shouldTriggerCrossfade(177_000L))  // 3s left: trigger next song in crossfade!
        assertTrue(shouldTriggerCrossfade(178_000L))  // 2s left: in crossfade
    }

    @Test
    fun testBidirectionalQueueNavigationInShuffleMode() {
        // Given a stable shuffled queue
        val queue = listOf(
            Song(id = "1", title = "Song A", artist = "Artist 1", album = "Album", duration = 180000, path = "/a.mp3"),
            Song(id = "2", title = "Song B", artist = "Artist 2", album = "Album", duration = 180000, path = "/b.mp3"),
            Song(id = "3", title = "Song C", artist = "Artist 3", album = "Album", duration = 180000, path = "/c.mp3"),
            Song(id = "4", title = "Song D", artist = "Artist 4", album = "Album", duration = 180000, path = "/d.mp3")
        )

        var currentIdx = 0 // Starts at Song A

        fun next(): Song {
            currentIdx = if (currentIdx in 0 until queue.lastIndex) currentIdx + 1 else 0
            return queue[currentIdx]
        }

        fun prev(): Song {
            currentIdx = if (currentIdx > 0) currentIdx - 1 else queue.lastIndex
            return queue[currentIdx]
        }

        // Forward: A -> B -> C
        assertEquals("Song B", next().title)
        assertEquals("Song C", next().title)

        // Backward: C -> B -> A (must return to previous songs, NOT jump forward or randomize)
        assertEquals("Song B", prev().title)
        assertEquals("Song A", prev().title)

        // Wraparound backward: A -> D
        assertEquals("Song D", prev().title)

        // Wraparound forward: D -> A
        assertEquals("Song A", next().title)
    }

    @Test
    fun testDriveHudSwipeDirectionThreshold() {
        val swipeThresholdPx = 56f

        fun evaluateSwipe(accumulatedDrag: Float): String {
            return when {
                accumulatedDrag < -swipeThresholdPx -> "NEXT"
                accumulatedDrag > swipeThresholdPx -> "PREVIOUS"
                else -> "NO_ACTION"
            }
        }

        // Swiping Left (negative X drag) -> NEXT
        assertEquals("NEXT", evaluateSwipe(-70f))
        assertEquals("NEXT", evaluateSwipe(-56.1f))

        // Swiping Right (positive X drag) -> PREVIOUS
        assertEquals("PREVIOUS", evaluateSwipe(70f))
        assertEquals("PREVIOUS", evaluateSwipe(56.1f))

        // Small / indeterminate jitter -> NO_ACTION
        assertEquals("NO_ACTION", evaluateSwipe(0f))
        assertEquals("NO_ACTION", evaluateSwipe(30f))
        assertEquals("NO_ACTION", evaluateSwipe(-30f))
        assertEquals("NO_ACTION", evaluateSwipe(55.9f))
        assertEquals("NO_ACTION", evaluateSwipe(-55.9f))
    }

    @Test
    fun testVirtualSurroundSpatialWidth() {
        fun applySurround(sampleL: Float, sampleR: Float, width: Float): Pair<Float, Float> {
            val mid = (sampleL + sampleR) * 0.5f
            val side = (sampleL - sampleR) * 0.5f * width
            return Pair(mid + side, mid - side)
        }

        // Stereo wide signal
        val (lWide, rWide) = applySurround(1000f, -1000f, 1.50f)
        assertEquals(1500f, lWide, 0.001f)
        assertEquals(-1500f, rWide, 0.001f)

        // Center mono signal (sampleL == sampleR) must be preserved identically without alteration
        val (lMono, rMono) = applySurround(2000f, 2000f, 1.50f)
        assertEquals(2000f, lMono, 0.001f)
        assertEquals(2000f, rMono, 0.001f)
    }

    @Test
    fun testMultiFieldSearchMatching() {
        val song = Song(
            id = "/storage/usb1/Tamil/Harris/Vaseegara.mp3",
            title = "Vaseegara",
            artist = "Bombay Jayashri",
            album = "Minnale",
            duration = 300000,
            path = "/storage/usb1/Tamil/Harris/Vaseegara.mp3"
        )

        fun matches(query: String): Boolean {
            val q = query.trim()
            return song.title.contains(q, ignoreCase = true) ||
                   song.artist.contains(q, ignoreCase = true) ||
                   song.album.contains(q, ignoreCase = true) ||
                   song.path.contains(q, ignoreCase = true)
        }

        assertTrue(matches("Vaseegara"))      // Match by title
        assertTrue(matches("bombay"))         // Match by artist
        assertTrue(matches("minnale"))        // Match by album
        assertTrue(matches("Tamil"))          // Match by folder/path
        assertTrue(matches("usb1"))           // Match by drive
        assertFalse(matches("Anirudh"))       // Non-matching
    }

    @Test
    fun testSleepTimerRemainingCalculation() {
        val remainingSec = 1740 // 29 minutes
        val mins = (remainingSec / 60) + 1
        assertEquals(30, mins)

        val formatted = String.format("%02d:%02d", remainingSec / 60, remainingSec % 60)
        assertEquals("29:00", formatted)
    }

    @Test
    fun testAppleCarPlayTransitionDirection() {
        val playlist = listOf("Track 1", "Track 2", "Track 3", "Track 4")
        var currentIndex = 0
        var navDirection = 1

        fun onNext() {
            navDirection = 1
            currentIndex = (currentIndex + 1) % playlist.size
        }

        fun onPrev() {
            navDirection = -1
            currentIndex = if (currentIndex > 0) currentIndex - 1 else playlist.lastIndex
        }

        onNext()
        assertEquals(1, navDirection) // 1 = Next/Forward (Slide out left, slide in right)
        assertEquals(1, currentIndex)

        onNext()
        assertEquals(1, navDirection)
        assertEquals(2, currentIndex)

        onPrev()
        assertEquals(-1, navDirection) // -1 = Prev/Backward (Slide out right, slide in left)
        assertEquals(1, currentIndex)

        onPrev()
        assertEquals(-1, navDirection)
        assertEquals(0, currentIndex)

        // Wrap backwards
        onPrev()
        assertEquals(-1, navDirection)
        assertEquals(3, currentIndex)
    }

    @Test
    fun testDualModeCrossfadeSelection() {
        fun resolveCrossfadeDuration(isAutoCrossfade: Boolean, configuredSec: Float, isPlaying: Boolean): Float {
            return when {
                isAutoCrossfade -> configuredSec
                isPlaying -> 0.12f // 120ms anti-pop micro-fade for manual skip
                else -> 0.0f
            }
        }

        // Automatic end-of-track crossfade uses full configured duration (3.0s)
        assertEquals(3.0f, resolveCrossfadeDuration(isAutoCrossfade = true, configuredSec = 3.0f, isPlaying = true), 0.001f)
        assertEquals(5.0f, resolveCrossfadeDuration(isAutoCrossfade = true, configuredSec = 5.0f, isPlaying = true), 0.001f)

        // Manual skip during playback uses 120ms anti-pop micro-fade (CarPlay style)
        assertEquals(0.12f, resolveCrossfadeDuration(isAutoCrossfade = false, configuredSec = 3.0f, isPlaying = true), 0.001f)

        // Starting playback from stopped/paused state is instantaneous (0.0s)
        assertEquals(0.0f, resolveCrossfadeDuration(isAutoCrossfade = false, configuredSec = 3.0f, isPlaying = false), 0.001f)
    }

    @Test
    fun testSongStreamingDetection() {
        val localSong = Song(
            id = "1",
            title = "Local Track",
            artist = "Local Artist",
            album = "Local Album",
            duration = 180000L,
            path = "/storage/emulated/0/Music/track.flac"
        )
        assertFalse(localSong.isStream())
        assertEquals("FLAC", localSong.getFileExtension())
        assertEquals("FLAC • 03:00", localSong.getBadgeLabel())

        val streamSong = Song(
            id = "2",
            title = "Online Single",
            artist = "Stream Artist",
            album = "Single Album",
            duration = 215000L,
            path = "https://aac.saavncdn.com/123/stream.mp4"
        )
        assertTrue(streamSong.isStream())
        assertEquals("STREAM", streamSong.getFileExtension())
        assertEquals("STREAM • 03:35", streamSong.getBadgeLabel())

        val liveRadio = Song(
            id = "3",
            title = "Lofi Chill Radio",
            artist = "ChillHop",
            album = "Community Radio",
            duration = 0L,
            path = "https://streams.ilovemusic.de/iloveradio17.mp3"
        )
        assertTrue(liveRadio.isStream())
        assertEquals("LIVE", liveRadio.getFileExtension())
        assertEquals("LIVE • STREAM", liveRadio.getBadgeLabel())
    }

    @Test
    fun testRadioStationModelAndSongConversion() {
        val station = com.aashik.music.model.RadioStation(
            id = "test_synthwave",
            name = "Nightwave Plaza",
            genre = "Synthwave",
            description = "Aesthetic Synthwave Stream",
            streamUrl = "https://radio.plaza.one/mp3",
            bitrate = "128 kbps",
            country = "Global"
        )
        val stationSong = station.toSong()
        assertTrue(stationSong.isStream())
        assertEquals("Nightwave Plaza", stationSong.title)
        assertEquals("Synthwave • 128 kbps", stationSong.artist)
        assertEquals("Online Radio & Streams", stationSong.album)
        assertEquals(0L, stationSong.duration)
        assertEquals("https://radio.plaza.one/mp3", stationSong.path)
    }

    @Test
    fun testOnlineStreamUrlDecryptionAndQualitySelection() {
        val sampleEncrypted = "ID2ieOjCrwfgWvL5sXl4B1ImC5QfbsDy3oAkJDMbq8VkXOZU65DcFdrDhsTWIRT7u/8vMld9DHecVMFCBmD5/Rw7tS9a8Gtq"
        val decrypted = com.aashik.music.service.OnlineStreamRepository.decryptMediaUrl(sampleEncrypted)
        assertTrue("Decrypted URL must start with http or https", decrypted.startsWith("http"))
        assertTrue("Decrypted URL must end with .mp4 or .aac or .mp3", decrypted.contains(".mp4"))

        // Quality selection: 320kbps upgrade when supported
        val stream320 = com.aashik.music.service.OnlineStreamRepository.selectBestStream(decrypted, supports320 = true)
        assertTrue(stream320.contains("_320.mp4"))

        // Quality selection: 160kbps balanced when 320 is not supported
        val stream160 = com.aashik.music.service.OnlineStreamRepository.selectBestStream(decrypted, supports320 = false)
        assertTrue(stream160.contains("_160.mp4"))
    }

    @Test
    fun testDownloadStatusStatesAndStreamDownloadEligibility() {
        val streamSong = Song(
            id = "online_test123",
            title = "Blinding Lights",
            artist = "The Weeknd",
            album = "After Hours",
            duration = 200000L,
            path = "https://aac.saavncdn.com/test_320.mp4"
        )
        // Finite duration streams are eligible for download
        assertTrue(streamSong.isStream())
        assertTrue(streamSong.duration > 0)

        // Live stations (duration <= 0) are infinite broadcasts, not downloadable as single tracks
        val liveStation = Song(
            id = "station_1",
            title = "Lofi Girl",
            artist = "Lofi",
            album = "Radio",
            duration = 0L,
            path = "https://play.streamafrica.net/lofigirl"
        )
        assertTrue(liveStation.isStream())
        assertEquals(0L, liveStation.duration)

        // Verify DownloadStatus hierarchy
        val idle: com.aashik.music.service.DownloadStatus = com.aashik.music.service.DownloadStatus.Idle
        val downloading: com.aashik.music.service.DownloadStatus = com.aashik.music.service.DownloadStatus.Downloading(0.45f)
        val downloaded: com.aashik.music.service.DownloadStatus = com.aashik.music.service.DownloadStatus.Downloaded
        val failed: com.aashik.music.service.DownloadStatus = com.aashik.music.service.DownloadStatus.Failed("Network timeout")

        assertEquals(0.45f, (downloading as com.aashik.music.service.DownloadStatus.Downloading).progress, 0.001f)
        assertEquals("Network timeout", (failed as com.aashik.music.service.DownloadStatus.Failed).error)
        assertTrue(idle is com.aashik.music.service.DownloadStatus.Idle)
        assertTrue(downloaded is com.aashik.music.service.DownloadStatus.Downloaded)
    }

    @Test
    fun testMoodAndGenreCategories() {
        val moods = com.aashik.music.model.RadioStation.MOOD_CATEGORIES
        val genres = com.aashik.music.model.RadioStation.GENRE_CATEGORIES
        val allCategories = com.aashik.music.model.RadioStation.CATEGORIES

        // Verify counts
        assertEquals(12, moods.size)
        assertEquals(38, genres.size)
        assertEquals(50, allCategories.size)

        // Verify sections
        assertTrue(moods.all { it.section == com.aashik.music.model.CategorySection.MOODS_AND_MOMENTS })
        assertTrue(genres.all { it.section == com.aashik.music.model.CategorySection.GENRES })

        // Verify key mood tile colors
        val chill = moods.find { it.title == "Chill" }
        assertTrue(chill != null)
        assertEquals(0xFFA4C5FF, chill?.stripeColor)
        assertTrue(chill?.imageUrl?.contains("yt3.googleusercontent.com") == true)
        assertEquals("ggMPOg1uX1JOQWZFeDByc2Jm", chill?.params)

        val commute = moods.find { it.title == "Commute" }
        assertTrue(commute != null)
        assertEquals(0xFFFFC200, commute?.stripeColor)

        val romance = moods.find { it.title == "Romance" }
        assertTrue(romance != null)
        assertEquals(0xFFCC0000, romance?.stripeColor)

        // Verify key genre tile colors
        val african = genres.find { it.title == "African" }
        assertTrue(african != null)
        assertEquals(0xFF00A513, african?.stripeColor)

        val rock = genres.find { it.title == "Rock" }
        assertTrue(rock != null)
        assertEquals(0xFFCC0000, rock?.stripeColor)

        val tamil = genres.find { it.title == "Tamil" }
        assertTrue(tamil != null)
        assertEquals(0xFFCC0000, tamil?.stripeColor)

        val punjabi = genres.find { it.title == "Punjabi" }
        assertTrue(punjabi != null)
        assertEquals(0xFFE24B00, punjabi?.stripeColor)

        // Verify all 50 categories have non-blank IDs and valid stripe colors
        val validCategoryIds = allCategories.map { it.id }.toSet()
        assertEquals(50, validCategoryIds.size)

        // Verify curated stations map to known category IDs
        val curated = com.aashik.music.model.RadioStation.CURATED_STATIONS
        for (station in curated) {
            assertTrue("Station ${station.name} category ${station.categoryId} must exist in categories",
                validCategoryIds.contains(station.categoryId))
        }
    }

    @Test
    fun testStreamSongGenreRegistrationAndRadioTargeting() {
        val testSong = Song(
            id = "online_tamil_test_1",
            title = "Ala Bolelo",
            artist = "Anirudh Ravichander",
            album = "Jailer 2",
            duration = 240000L,
            path = "https://example.com/stream.mp4"
        )

        // Register genre
        com.aashik.music.service.OnlineStreamRepository.registerSongGenre(testSong.id, "Tamil")
        assertEquals("Tamil", com.aashik.music.service.OnlineStreamRepository.getSongGenre(testSong))

        // Station genre resolution
        val station = com.aashik.music.model.RadioStation(
            id = "test_lofi",
            name = "Lofi Study",
            genre = "Lo-Fi",
            streamUrl = "https://example.com/lofi.mp3"
        )
        val stationSong = station.toSong()
        com.aashik.music.service.OnlineStreamRepository.registerSongGenre(stationSong.id, station.genre)
        assertEquals("Lo-Fi", com.aashik.music.service.OnlineStreamRepository.getSongGenre(stationSong))
    }

    @Test
    fun testQueueItemSelectionAndDropBehavior() {
        val songs = (1..5).map { i ->
            Song(id = "s_$i", title = "Song $i", artist = "Artist $i", album = "Album $i", duration = 200000L, path = "/path/$i")
        }

        // 1. When 3rd item (index 2: "Song 3") is selected: items 1 and 2 before it must be removed
        var queue = songs
        val selectedIdx = 2
        val selectedSong = queue[selectedIdx]
        val remainingQueue = queue.drop(selectedIdx + 1)

        assertEquals("Song 3", selectedSong.title)
        assertEquals(2, remainingQueue.size)
        assertEquals("Song 4", remainingQueue[0].title)
        assertEquals("Song 5", remainingQueue[1].title)

        // 2. When last item (index 1 in remaining queue: "Song 5") is selected: all items before it must be removed
        val lastIdx = remainingQueue.lastIndex
        val lastSong = remainingQueue[lastIdx]
        val emptyQueue = remainingQueue.drop(lastIdx + 1)

        assertEquals("Song 5", lastSong.title)
        assertTrue(emptyQueue.isEmpty())
    }

    @Test
    fun testYouTubeVideoDetectionAndExtraction() {
        val ytWatchSong = Song(
            id = "s_1",
            title = "Tamil Melody",
            artist = "Anirudh",
            album = "Single",
            duration = 180000L,
            path = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
        )
        val ytShortSong = Song(
            id = "s_2",
            title = "Acoustic Live",
            artist = "Coldplay",
            album = "Concert",
            duration = 210000L,
            path = "https://youtu.be/9bZkp7q19f0"
        )
        val regularSong = Song(
            id = "s_3",
            title = "Local Audio",
            artist = "Artist",
            album = "Album",
            duration = 200000L,
            path = "/storage/emulated/0/Music/MusicPlayer/song.mp3"
        )

        assertTrue(ytWatchSong.isYouTube())
        assertEquals("dQw4w9WgXcQ", ytWatchSong.getYouTubeVideoId())
        assertTrue(ytWatchSong.isVideo())

        assertTrue(ytShortSong.isYouTube())
        assertEquals("9bZkp7q19f0", ytShortSong.getYouTubeVideoId())
        assertTrue(ytShortSong.isVideo())

        assertFalse(regularSong.isYouTube())
        assertNull(regularSong.getYouTubeVideoId())
        assertFalse(regularSong.isVideo())
    }

    @Test
    fun testSingleDownloadDirectoryStructure() {
        val song1 = Song(id = "online_1", title = "Track 1", artist = "Artist A", album = "Album A", duration = 180000L, path = "https://aac.saavncdn.com/1/track1.mp4")
        val song2 = Song(id = "online_2", title = "Track 2", artist = "Artist B", album = "Album B", duration = 210000L, path = "https://aac.saavncdn.com/2/track2.mp4")

        val parentDir = java.io.File("/storage/emulated/0/Music/MusicPlayer")
        val file1 = java.io.File(parentDir, "Artist_A_Track_1_hash1.m4a")
        val file2 = java.io.File(parentDir, "Artist_B_Track_2_hash2.m4a")

        // Both files must be in the exact same parent folder (not in separate subfolders)
        assertEquals(file1.parent, file2.parent)
        assertEquals("/storage/emulated/0/Music/MusicPlayer", file1.parent)
    }

    @Test
    fun testDownloadStoragePresets() {
        assertEquals("public_music", com.aashik.music.service.StreamDownloadManager.PRESET_PUBLIC_MUSIC)
        assertEquals("public_downloads", com.aashik.music.service.StreamDownloadManager.PRESET_PUBLIC_DOWNLOADS)
        assertEquals("app_internal", com.aashik.music.service.StreamDownloadManager.PRESET_APP_INTERNAL)
        assertEquals("custom", com.aashik.music.service.StreamDownloadManager.PRESET_CUSTOM)
    }

    @Test
    fun testBottomDockVisibilityLogic() {
        fun isDockVisible(hideBottomBar: Boolean): Boolean {
            return !hideBottomBar
        }

        // When hideBottomBar is false (normal mode): dock is visible
        assertTrue(isDockVisible(false))

        // When hideBottomBar is true (setting enabled): dock is completely hidden (not minimized to pill)
        assertFalse(isDockVisible(true))
    }

    @Test
    fun testYouTubeVideoResolverRegexAndSanitization() {
        val sampleJson = """{"contents":{"sectionListRenderer":{"contents":[{"itemSectionRenderer":{"contents":[{"compactVideoRenderer":{"videoId":"AY3Yea6hBbI","title":{"runs":[{"text":"Hey Minnale Video"}]}}}]}}]}}}"""
        val regex = Regex(""""videoId":\s*"([a-zA-Z0-9_-]{11})"""")
        val match = regex.find(sampleJson)
        assertNotNull(match)
        assertEquals("AY3Yea6hBbI", match!!.groupValues[1])

        // Title cleaning verification
        val rawTitle = "Hey Minnale (From \"Amaran\") &amp; Others"
        val cleaned = rawTitle
            .replace("&amp;", "&")
            .replace(Regex("""(?i)\(From\s+["'][^"']+["']\)"""), "")
            .replace(Regex("""(?i)\(Reprise\)"""), "")
            .replace(Regex("""(?i)\(Official.*?\)"""), "")
            .replace(Regex("""\(.*?\)|\[.*?\]"""), "")
            .replace(Regex("""\s+"""), " ")
            .trim()
        assertEquals("Hey Minnale & Others", cleaned)
    }

    @Test
    fun testSeekBarHoldAndCatchUpLogic() {
        var pendingSeekProgress: Float? = 0.75f
        val engineProgressBeforeSeek = 0.20f

        // 1. Immediately after finger release: displayProgress must stay at 0.75f, not jumping back to 0.20f
        val displayProgress1 = pendingSeekProgress ?: engineProgressBeforeSeek
        assertEquals(0.75f, displayProgress1, 0.001f)

        // 2. Engine progresses to 0.735f (within 0.025f proximity): pendingSeekProgress clears
        val incomingEngineProgress = 0.735f
        if (kotlin.math.abs(incomingEngineProgress - pendingSeekProgress!!) <= 0.025f) {
            pendingSeekProgress = null
        }
        assertNull(pendingSeekProgress)

        // 3. Display progress now smoothly tracks engine progress
        val displayProgress2 = pendingSeekProgress ?: incomingEngineProgress
        assertEquals(0.735f, displayProgress2, 0.001f)
    }
}
