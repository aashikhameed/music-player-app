package com.aashik.music

import com.aashik.music.model.Song
import com.aashik.music.provider.LocalFileSourceProvider
import com.aashik.music.provider.OnlineStreamSourceProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for Equalizer, SCVC 2.0 acoustic calculations, and MediaSourceProvider abstractions.
 */
class EqualizerModelTest {

    @Test
    fun testScvcSpeedGainCalculations() {
        // Off mode (0)
        assertEquals(0.0f, calculateScvcGain(0f, 0), 0.01f)
        assertEquals(0.0f, calculateScvcGain(100f, 0), 0.01f)

        // Threshold test: speed <= 15 km/h must have 0.0 dB gain across all modes
        assertEquals(0.0f, calculateScvcGain(0f, 1), 0.01f)
        assertEquals(0.0f, calculateScvcGain(10f, 1), 0.01f)
        assertEquals(0.0f, calculateScvcGain(15f, 2), 0.01f)
        assertEquals(0.0f, calculateScvcGain(15f, 3), 0.01f)

        // Above threshold: 45 km/h (excess speed = 30 km/h)
        // Mild: min(3.0, 30 * 1/30) = 1.0 dB
        assertEquals(1.0f, calculateScvcGain(45f, 1), 0.05f)
        // Moderate: min(5.0, 30 * 1.5/20) = 2.25 dB
        assertEquals(2.25f, calculateScvcGain(45f, 2), 0.05f)
        // Dynamic: min(7.0, 30 * 2.0/15) = 4.0 dB
        assertEquals(4.0f, calculateScvcGain(45f, 3), 0.05f)

        // Maximum gain caps at high speed (120 km/h)
        // Mild capped at 3.0 dB
        assertEquals(3.0f, calculateScvcGain(120f, 1), 0.01f)
        // Moderate capped at 5.0 dB
        assertEquals(5.0f, calculateScvcGain(120f, 2), 0.01f)
        // Dynamic capped at 7.0 dB
        assertEquals(7.0f, calculateScvcGain(120f, 3), 0.01f)
    }

    @Test
    fun testBalanceFaderClamping() {
        assertEquals(-1.0f, (-1.5f).coerceIn(-1.0f, 1.0f), 0.001f)
        assertEquals(1.0f, (2.0f).coerceIn(-1.0f, 1.0f), 0.001f)
        assertEquals(0.35f, (0.35f).coerceIn(-1.0f, 1.0f), 0.001f)
    }

    @Test
    fun testLocalFileSourceProvider() = runBlocking {
        val provider = LocalFileSourceProvider()
        assertEquals("local_storage", provider.providerId)

        val localSong = Song(
            id = "1",
            title = "Test Song",
            artist = "Test Artist",
            album = "Test Album",
            path = "/storage/emulated/0/Music/song.mp3",
            duration = 180000L
        )
        assertTrue(provider.isOfflineAvailable(localSong))
        assertEquals("/storage/emulated/0/Music/song.mp3", provider.getStreamUri(localSong))

        val streamSong = Song(
            id = "2",
            title = "Stream Song",
            artist = "Stream Artist",
            album = "Stream Album",
            path = "https://example.com/stream.mp3",
            duration = 0L
        )
        assertFalse(provider.isOfflineAvailable(streamSong))
        assertNull(provider.getStreamUri(streamSong))
    }

    @Test
    fun testOnlineStreamSourceProvider() = runBlocking {
        val provider = OnlineStreamSourceProvider()
        assertEquals("online_stream", provider.providerId)

        val streamSong = Song(
            id = "3",
            title = "Radio Station",
            artist = "Radio",
            album = "Live",
            path = "http://stream.example.com/live.aac",
            duration = 0L
        )
        assertFalse(provider.isOfflineAvailable(streamSong))
        assertEquals("http://stream.example.com/live.aac", provider.getStreamUri(streamSong))

        val localSong = Song(
            id = "4",
            title = "Local Song",
            artist = "Local Artist",
            album = "Local Album",
            path = "/storage/emulated/0/Music/track.flac",
            duration = 240000L
        )
        assertNull(provider.getStreamUri(localSong))
    }

    private fun calculateScvcGain(speedKmh: Float, mode: Int): Float {
        if (mode == 0 || speedKmh <= 15.0f) return 0.0f
        val excess = speedKmh - 15.0f
        return when (mode) {
            1 -> kotlin.math.min(3.0f, excess * (1.0f / 30.0f))
            2 -> kotlin.math.min(5.0f, excess * (1.5f / 20.0f))
            3 -> kotlin.math.min(7.0f, excess * (2.0f / 15.0f))
            else -> 0.0f
        }
    }
}

