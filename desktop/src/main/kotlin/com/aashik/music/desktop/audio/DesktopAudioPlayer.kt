package com.aashik.music.desktop.audio

import com.aashik.music.desktop.model.DesktopSong
import javafx.application.Platform
import javafx.scene.media.Media
import javafx.scene.media.MediaPlayer
import javafx.util.Duration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

object DesktopAudioPlayer {
    private var mediaPlayer: MediaPlayer? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _volume = MutableStateFlow(0.85f)
    val volume: StateFlow<Float> = _volume.asStateFlow()

    private var onTrackFinishedCallback: (() -> Unit)? = null

    private var javaFxStarted = false

    /** Started lazily on first playback so the UI (and off-screen snapshots) never need a display. */
    private fun ensureJavaFx() {
        if (javaFxStarted) return
        javaFxStarted = true
        try {
            Platform.startup {}
        } catch (_: IllegalStateException) {
            // JavaFX toolkit already initialized
        } catch (e: Throwable) {
            println("[DesktopAudio] JavaFX toolkit startup notice: ${e.message}")
        }
    }

    fun setOnTrackFinished(callback: () -> Unit) {
        onTrackFinishedCallback = callback
    }

    fun play(song: DesktopSong) {
        ensureJavaFx()
        try {
            mediaPlayer?.stop()
            mediaPlayer?.dispose()
            mediaPlayer = null
        } catch (_: Exception) {}

        _isPlaying.value = false
        _currentPositionMs.value = 0L
        _durationMs.value = song.duration

        try {
            val uriString = if (song.isStream()) {
                song.path
            } else {
                File(song.path).toURI().toString()
            }

            val media = Media(uriString)
            val player = MediaPlayer(media)
            mediaPlayer = player

            player.volume = _volume.value.toDouble()
            applyEq(player)

            player.setOnReady {
                val total = player.totalDuration
                if (total != null && !total.isUnknown) {
                    _durationMs.value = total.toMillis().toLong()
                }
            }

            player.currentTimeProperty().addListener { _, _, newTime ->
                if (newTime != null) {
                    _currentPositionMs.value = newTime.toMillis().toLong()
                }
            }

            player.setOnEndOfMedia {
                _isPlaying.value = false
                onTrackFinishedCallback?.invoke()
            }

            player.setOnError {
                println("[DesktopAudio] Playback error: ${player.error?.message}")
                _isPlaying.value = false
            }

            player.play()
            _isPlaying.value = true
        } catch (e: Exception) {
            println("[DesktopAudio] Failed to play song: ${e.message}")
            _isPlaying.value = false
        }
    }

    fun pause() {
        mediaPlayer?.pause()
        _isPlaying.value = false
    }

    fun resume() {
        mediaPlayer?.play()
        _isPlaying.value = true
    }

    fun togglePlayPause() {
        if (_isPlaying.value) pause() else resume()
    }

    fun seekTo(positionMs: Long) {
        try {
            mediaPlayer?.seek(Duration.millis(positionMs.toDouble()))
            _currentPositionMs.value = positionMs
        } catch (e: Exception) {
            println("[DesktopAudio] Seek failed: ${e.message}")
        }
    }

    // 10-band equalizer (JavaFX AudioEqualizer: 32 Hz … 16 kHz), applied to the current and every later track.
    private val eqGains = FloatArray(10)
    val eqBandGains: List<Float> get() = eqGains.toList()

    fun setEqBand(index: Int, db: Float) {
        if (index !in eqGains.indices) return
        eqGains[index] = db.coerceIn(-12f, 12f)
        mediaPlayer?.let { applyEq(it) }
    }

    fun resetEq(values: List<Float>) {
        for (i in eqGains.indices) eqGains[i] = values.getOrElse(i) { 0f }.coerceIn(-12f, 12f)
        mediaPlayer?.let { applyEq(it) }
    }

    private fun applyEq(player: MediaPlayer) {
        try {
            val eq = player.audioEqualizer
            eq.isEnabled = true
            eq.bands.forEachIndexed { i, band -> if (i < eqGains.size) band.gain = eqGains[i].toDouble() }
        } catch (_: Throwable) {}
    }

    fun setVolume(vol: Float) {
        val clamped = vol.coerceIn(0f, 1f)
        _volume.value = clamped
        mediaPlayer?.volume = clamped.toDouble()
    }
}

