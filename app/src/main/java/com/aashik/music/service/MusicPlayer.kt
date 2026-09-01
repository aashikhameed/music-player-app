package com.aashik.music.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import com.aashik.music.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MusicPlayer(private val context: Context) {

    // Low-RAM buffer control tailored for 2GB automotive devices
    private val loadControl = DefaultLoadControl.Builder()
        .setBufferDurationsMs(
            2500,  // Min buffer ms (2.5s)
            8000,  // Max buffer ms (8s - cuts ExoPlayer memory by ~60%)
            1000,  // Buffer for playback ms
            1500   // Buffer for playback after rebuffer ms
        )
        .setPrioritizeTimeOverSizeThresholds(true)
        .build()

    private var exoPlayer: ExoPlayer = ExoPlayer.Builder(context)
        .setLoadControl(loadControl)
        .build()

    private val audioManager: AudioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val focusRequest: AudioFocusRequest

    private val _positionFlow = MutableStateFlow(0L)
    val positionFlow: StateFlow<Long> = _positionFlow

    private val _durationFlow = MutableStateFlow(1L)
    val durationFlow: StateFlow<Long> = _durationFlow

    private val _isPlayingFlow = MutableStateFlow(false)
    val isPlayingFlow: StateFlow<Boolean> = _isPlayingFlow

    private val _currentSongFlow = MutableStateFlow<Song?>(null)
    val currentSongFlow: StateFlow<Song?> = _currentSongFlow

    private val _currentIndexFlow = MutableStateFlow(-1)
    val currentIndexFlow: StateFlow<Int> = _currentIndexFlow

    private var progressJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    var onCompletion: (() -> Unit)? = null

    private var volumeReceiver: BroadcastReceiver? = null

    init {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(attrs)
            .setOnAudioFocusChangeListener { focusChange ->
                when (focusChange) {
                    AudioManager.AUDIOFOCUS_LOSS -> pause()
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> pause()
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> exoPlayer.volume = 0.2f
                    AudioManager.AUDIOFOCUS_GAIN -> exoPlayer.volume = 1.0f
                }
            }
            .build()

        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlayingFlow.value = isPlaying
            }

            override fun onVolumeChanged(volume: Float) {
                if (volume <= 0f && exoPlayer.isPlaying) {
                    pause()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    onCompletion?.invoke()
                } else if (playbackState == Player.STATE_READY) {
                    _durationFlow.value = exoPlayer.duration.takeIf { it > 0 } ?: 1L
                }
            }
        })

        // Listen for hardware volume mute / volume change events from steering wheel or head unit
        try {
            var wasAutoPausedByMute = false
            volumeReceiver = object : BroadcastReceiver() {
                override fun onReceive(c: Context?, intent: Intent?) {
                    val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                    val isStreamMute = audioManager.isStreamMute(AudioManager.STREAM_MUSIC)
                    val isMuted = currentVolume == 0 || isStreamMute
                    
                    if (isMuted) {
                        if (exoPlayer.isPlaying) {
                            wasAutoPausedByMute = true
                            pause()
                        }
                    } else {
                        if (wasAutoPausedByMute && !exoPlayer.isPlaying) {
                            wasAutoPausedByMute = false
                            resume()
                        }
                    }
                }
            }
            val filter = IntentFilter("android.media.VOLUME_CHANGED_ACTION").apply {
                addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
                addAction("android.media.STREAM_MUTE_CHANGED_ACTION")
            }
            context.registerReceiver(volumeReceiver, filter)
        } catch (_: Exception) {}
    }

    fun getCurrentMediaId(): String? {
        return exoPlayer.currentMediaItem?.mediaId
    }

    private var playlist: List<Song> = emptyList()
    private var currentIndex: Int = -1

    fun setPlaylist(songs: List<Song>, startIndex: Int = 0) {
        playlist = songs
        currentIndex = startIndex
        _currentIndexFlow.value = currentIndex
        play(playlist[currentIndex])
    }

    fun play(song: Song) {
        val result = audioManager.requestAudioFocus(focusRequest)
        if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            return
        }

        _currentSongFlow.value = song
        val item = MediaItem.Builder()
            .setUri(Uri.parse(song.path))
            .setMediaId(song.path)
            .build()

        exoPlayer.setMediaItem(item)
        exoPlayer.prepare()
        exoPlayer.play()
        startTrackingProgress()
    }

    fun pause() {
        exoPlayer.pause()
        stopTrackingProgress()
        audioManager.abandonAudioFocusRequest(focusRequest)
    }

    fun stop() {
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
        stopTrackingProgress()
        _positionFlow.value = 0L
        _isPlayingFlow.value = false
        _currentSongFlow.value = null
        audioManager.abandonAudioFocusRequest(focusRequest)
    }

    suspend fun isPrepared(): Boolean = withContext(Dispatchers.Main) {
        exoPlayer.playbackState == Player.STATE_READY && !exoPlayer.isPlaying
    }

    fun resume() {
        val result = audioManager.requestAudioFocus(focusRequest)
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            exoPlayer.playWhenReady = true
            startTrackingProgress()
        }
    }

    fun seekTo(position: Long) {
        exoPlayer.seekTo(position)
        _positionFlow.value = position
    }

    private fun getCurrentPositionSafe(): Long {
        return exoPlayer.currentPosition
    }

    private fun startTrackingProgress() {
        stopTrackingProgress()
        progressJob = scope.launch(Dispatchers.Main) {
            while (true) {
                _positionFlow.value = getCurrentPositionSafe()
                delay(1000L)
            }
        }
    }

    private fun stopTrackingProgress() {
        progressJob?.cancel()
        progressJob = null
    }

    fun release() {
        stopTrackingProgress()
        try {
            volumeReceiver?.let { context.unregisterReceiver(it) }
            volumeReceiver = null
        } catch (_: Exception) {}
        audioManager.abandonAudioFocusRequest(focusRequest)
        exoPlayer.release()
    }
}
