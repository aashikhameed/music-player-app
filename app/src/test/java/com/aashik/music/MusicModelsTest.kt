package com.aashik.music

import com.aashik.music.model.MusicFolder
import com.aashik.music.model.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
}
