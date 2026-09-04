package com.aashik.music.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import com.aashik.music.model.Song
import com.aashik.music.nativeaudio.NativeAudioBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Automotive Audio Playback Service Manager.
 * Backed by high-performance C++ OpenSL ES native audio engine (NativeAudioBridge).
 */
class MusicPlayer(private val context: Context) : NativeAudioBridge.PlaybackListener {

    private val nativeBridge = NativeAudioBridge.instance

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

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    var onCompletion: (() -> Unit)? = null

    private var volumeReceiver: BroadcastReceiver? = null
    private var playlist: List<Song> = emptyList()
    private var currentIndex: Int = -1

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
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> nativeBridge.setVolume(0.2f)
                    AudioManager.AUDIOFOCUS_GAIN -> nativeBridge.setVolume(1.0f)
                }
            }
            .build()

        // Register Native C++ Audio Callbacks
        nativeBridge.setListener(this)

        // Listen for hardware volume mute / volume change events from steering wheel or head unit
        try {
            var wasAutoPausedByMute = false
            volumeReceiver = object : BroadcastReceiver() {
                override fun onReceive(c: Context?, intent: Intent?) {
                    val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                    val isStreamMute = audioManager.isStreamMute(AudioManager.STREAM_MUSIC)
                    val isMuted = currentVolume == 0 || isStreamMute

                    if (isMuted) {
                        if (_isPlayingFlow.value) {
                            wasAutoPausedByMute = true
                            pause()
                        }
                    } else {
                        if (wasAutoPausedByMute && !_isPlayingFlow.value) {
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

    // ─────────────────────────────────────────────────────────────────────────
    // NativeAudioBridge.PlaybackListener Callbacks
    // ─────────────────────────────────────────────────────────────────────────

    override fun onPlaybackStateChanged(state: Int) {
        // C++ PlaybackState: 3 = PLAYING, 4 = PAUSED, 5 = STOPPED, 6 = COMPLETED
        when (state) {
            3 -> _isPlayingFlow.value = true
            4, 5, 6 -> _isPlayingFlow.value = false
        }
    }

    override fun onPositionChanged(positionMs: Long) {
        _positionFlow.value = positionMs
    }

    override fun onDurationUpdated(durationMs: Long) {
        _durationFlow.value = if (durationMs > 0) durationMs else 1L
    }

    private var isTransitioning = false

    override fun onTrackEnded() {
        _isPlayingFlow.value = false
        if (!isTransitioning) {
            isTransitioning = true
            scope.launch(Dispatchers.Main) {
                try {
                    onCompletion?.invoke()
                } finally {
                    delay(400)
                    isTransitioning = false
                }
            }
        }
    }

    override fun onError(errorCode: Int, message: String) {
        _isPlayingFlow.value = false
        isTransitioning = false
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Player Control Methods
    // ─────────────────────────────────────────────────────────────────────────

    fun getCurrentMediaId(): String? {
        return _currentSongFlow.value?.path
    }

    fun setPlaylist(songs: List<Song>, startIndex: Int = 0) {
        playlist = songs
        currentIndex = startIndex
        _currentIndexFlow.value = currentIndex
        if (startIndex in playlist.indices) {
            play(playlist[currentIndex])
        }
    }

    fun play(song: Song) {
        isTransitioning = false
        val result = audioManager.requestAudioFocus(focusRequest)
        if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            return
        }

        _currentSongFlow.value = song
        _positionFlow.value = 0L
        if (song.duration > 0) {
            _durationFlow.value = song.duration
        }

        val res = nativeBridge.playTrack(song.path, 0L)
        if (res == 0) {
            _isPlayingFlow.value = true
            val nativeDur = nativeBridge.getDuration()
            if (nativeDur > 0) {
                _durationFlow.value = nativeDur
            }
        }
    }

    fun pause() {
        nativeBridge.pause()
        _isPlayingFlow.value = false
        audioManager.abandonAudioFocusRequest(focusRequest)
    }

    fun stop() {
        nativeBridge.stop()
        _positionFlow.value = 0L
        _isPlayingFlow.value = false
        _currentSongFlow.value = null
        audioManager.abandonAudioFocusRequest(focusRequest)
    }

    suspend fun isPrepared(): Boolean = withContext(Dispatchers.Main) {
        val state = nativeBridge.getPlaybackState()
        state == 3 || state == 4 // PLAYING or PAUSED
    }

    fun resume() {
        val result = audioManager.requestAudioFocus(focusRequest)
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            nativeBridge.resume()
            _isPlayingFlow.value = true
        }
    }

    fun seekTo(position: Long) {
        nativeBridge.seek(position)
        _positionFlow.value = position
    }

    fun release() {
        nativeBridge.setListener(null)
        try {
            volumeReceiver?.let { context.unregisterReceiver(it) }
            volumeReceiver = null
        } catch (_: Exception) {}
        audioManager.abandonAudioFocusRequest(focusRequest)
        nativeBridge.shutdown()
    }
}
