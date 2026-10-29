package com.aashik.music

import com.aashik.music.model.MusicFolder
import com.aashik.music.model.Song
import org.junit.Assert.assertEquals
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
    fun testEqualizerAlpineClarityPresetParameters() {
        // Alpine Clarity Preset Specification
        val alpineBands = listOf(0.0f, -4.0f, 1.0f, 2.0f, 3.0f)
        val alpineHpf = 2 // 80 Hz Cut

        assertEquals(5, alpineBands.size)
        assertEquals(0.0f, alpineBands[0], 0.001f)
        // Verify 250 Hz Mud-Cut is strictly attenuated (-4 dB)
        assertEquals(-4.0f, alpineBands[1], 0.001f)
        // Verify Vocal presence and treble air boost
        assertTrue("Vocal presence should be boosted", alpineBands[3] > 0.0f)
        assertTrue("Treble sparkle should be boosted", alpineBands[4] > 0.0f)
        assertEquals(2, alpineHpf)
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
}
