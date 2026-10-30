package com.aashik.music.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.audiofx.Equalizer
import android.os.PowerManager
import android.util.Log
import com.aashik.music.model.Song
import com.aashik.music.nativeaudio.NativeAudioBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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

    companion object {
        private const val TAG = "MusicPlayer"

        @Volatile
        private var instance: MusicPlayer? = null

        fun getInstance(context: Context): MusicPlayer {
            return instance ?: synchronized(this) {
                instance ?: MusicPlayer(context.applicationContext).also { instance = it }
            }
        }
    }

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
    var onCompletion: ((isAutoCrossfade: Boolean) -> Unit)? = null

    private var volumeReceiver: BroadcastReceiver? = null
    private var playlist: List<Song> = emptyList()
    private var currentIndex: Int = -1

    // ── Crossfade State (default 3 seconds) ──
    private var crossfadeSeconds: Int = 3
    private var isTransitioning = false
    private var autoCrossfadeTriggeredPath: String? = null
    private var playJob: Job? = null
    private var streamMediaPlayer: MediaPlayer? = null
    private var streamEqualizer: Equalizer? = null
    private var streamTickerJob: Job? = null

    // Equalizer preset definitions matching C++ AudioEqualizer DSP
    private val PRESET_GAINS = arrayOf(
        floatArrayOf(0.0f, 0.0f, 0.0f, 0.0f, 0.0f),      // 0: Flat
        floatArrayOf(0.0f, -4.0f, 1.0f, 2.0f, 3.0f),     // 1: Acoustic Clarity
        floatArrayOf(-2.0f, -5.0f, 2.0f, 3.0f, 1.0f),    // 2: Vocal Separation
        floatArrayOf(3.0f, -3.0f, 0.0f, 1.0f, 2.0f),     // 3: Bass Punch
        floatArrayOf(2.0f, -2.0f, 0.0f, 2.0f, 3.0f)      // 4: Rock Dynamic
    )

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
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                        nativeBridge.setVolume(0.2f)
                        streamMediaPlayer?.setVolume(0.2f, 0.2f)
                    }
                    AudioManager.AUDIOFOCUS_GAIN -> {
                        nativeBridge.setVolume(1.0f)
                        streamMediaPlayer?.setVolume(1.0f, 1.0f)
                    }
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

        if (crossfadeSeconds > 0 && _isPlayingFlow.value && !isTransitioning) {
            val dur = _durationFlow.value
            val crossfadeMs = crossfadeSeconds * 1000L
            // Only crossfade if the song is longer than crossfade duration + 2 seconds
            if (dur > (crossfadeMs + 2000L)) {
                val remainingMs = dur - positionMs
                if (remainingMs <= crossfadeMs) {
                    val currentPath = _currentSongFlow.value?.path
                    if (currentPath != null && currentPath != autoCrossfadeTriggeredPath) {
                        autoCrossfadeTriggeredPath = currentPath
                        isTransitioning = true
                        scope.launch(Dispatchers.Main) {
                            try {
                                if (onCompletion != null) {
                                    onCompletion?.invoke(true)
                                } else {
                                    playNext(isAutoCrossfade = true)
                                }
                            } finally {
                                delay(1000L)
                                isTransitioning = false
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDurationUpdated(durationMs: Long) {
        _durationFlow.value = if (durationMs > 0) durationMs else 1L
    }

    override fun onTrackEnded() {
        val currentPath = _currentSongFlow.value?.path
        if (currentPath != autoCrossfadeTriggeredPath && !isTransitioning) {
            isTransitioning = true
            scope.launch(Dispatchers.Main) {
                try {
                    if (onCompletion != null) {
                        onCompletion?.invoke(false)
                    } else {
                        playNext(isAutoCrossfade = false)
                    }
                } finally {
                    delay(400L)
                    isTransitioning = false
                }
            }
        }
    }

    override fun onError(errorCode: Int, message: String) {
        _isPlayingFlow.value = false
        isTransitioning = false
        autoCrossfadeTriggeredPath = null
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Player Control Methods
    // ─────────────────────────────────────────────────────────────────────────

    fun setCrossfadeSeconds(seconds: Int) {
        crossfadeSeconds = seconds
        nativeBridge.setCrossfadeDuration(seconds.toFloat())
    }

    fun getCrossfadeSeconds(): Int = crossfadeSeconds

    fun getCurrentMediaId(): String? {
        return _currentSongFlow.value?.path
    }

    fun setPlaylist(songs: List<Song>, startIndex: Int = 0) {
        playlist = songs
        currentIndex = startIndex
        _currentIndexFlow.value = currentIndex
    }

    fun playNext(isAutoCrossfade: Boolean = false) {
        if (playlist.isEmpty()) return
        val curId = _currentSongFlow.value?.id
        val idx = if (curId != null) playlist.indexOfFirst { it.id == curId } else currentIndex
        val nextIdx = if (idx in 0 until playlist.lastIndex) idx + 1 else 0
        currentIndex = nextIdx
        _currentIndexFlow.value = nextIdx
        val nextSong = playlist[nextIdx]
        play(nextSong, isAutoCrossfade)
        com.aashik.music.controller.MusicController.play(nextSong)
    }

    fun playPrevious() {
        if (playlist.isEmpty()) return
        val curId = _currentSongFlow.value?.id
        val idx = if (curId != null) playlist.indexOfFirst { it.id == curId } else currentIndex
        val prevIdx = if (idx > 0) idx - 1 else playlist.lastIndex
        currentIndex = prevIdx
        _currentIndexFlow.value = prevIdx
        val prevSong = playlist[prevIdx]
        play(prevSong, isAutoCrossfade = false)
        com.aashik.music.controller.MusicController.play(prevSong)
    }

    private fun initStreamEqualizer(audioSessionId: Int) {
        try {
            streamEqualizer?.release()
        } catch (_: Exception) {}
        streamEqualizer = null

        try {
            if (audioSessionId != AudioManager.ERROR) {
                val eq = Equalizer(0, audioSessionId)
                streamEqualizer = eq
                syncStreamEqualizer()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to initialize stream Equalizer: ${e.message}")
        }
    }

    fun syncStreamEqualizer() {
        val eq = streamEqualizer ?: return
        try {
            val enabled = nativeBridge.isEqualizerEnabled()
            eq.enabled = enabled
            if (!enabled) return

            val range = eq.bandLevelRange
            val minLevel = range[0]
            val maxLevel = range[1]
            val numBands = eq.numberOfBands.toInt()
            val currentPreset = nativeBridge.getEqualizerPreset()
            Log.d(TAG, "syncStreamEqualizer: enabled=$enabled, preset=$currentPreset, bands=$numBands")

            val gains = if (currentPreset in 0..4) {
                PRESET_GAINS[currentPreset]
            } else {
                FloatArray(5) { band -> nativeBridge.getEqualizerBandGain(band) }
            }

            for (band in 0 until minOf(numBands, 5)) {
                val gainDb = gains[band]
                val mB = (gainDb * 100f).toInt().toShort().coerceIn(minLevel, maxLevel)
                eq.setBandLevel(band.toShort(), mB)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error applying stream equalizer: ${e.message}")
        }
    }

    private fun startStreamTicker() {
        streamTickerJob?.cancel()
        streamTickerJob = scope.launch(Dispatchers.Default) {
            var elapsedMs = 0L
            while (streamMediaPlayer != null && _isPlayingFlow.value) {
                delay(250L)
                val curPos = try { streamMediaPlayer?.currentPosition?.toLong() } catch (_: Exception) { null }
                val pos = if (curPos != null && curPos > 0L) {
                    curPos
                } else {
                    elapsedMs += 250L
                    elapsedMs
                }
                _positionFlow.value = pos

                // Auto-crossfade transition before stream finishes
                if (crossfadeSeconds > 0 && _isPlayingFlow.value && !isTransitioning) {
                    val dur = _durationFlow.value
                    val crossfadeMs = crossfadeSeconds * 1000L
                    if (dur > (crossfadeMs + 2000L)) {
                        val remainingMs = dur - pos
                        if (remainingMs <= crossfadeMs) {
                            val currentPath = _currentSongFlow.value?.path
                            if (currentPath != null && currentPath != autoCrossfadeTriggeredPath) {
                                autoCrossfadeTriggeredPath = currentPath
                                isTransitioning = true
                                scope.launch(Dispatchers.Main) {
                                    try {
                                        if (onCompletion != null) {
                                            onCompletion?.invoke(true)
                                        } else {
                                            playNext(isAutoCrossfade = true)
                                        }
                                    } finally {
                                        delay(1000L)
                                        isTransitioning = false
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun stopStreamTicker() {
        streamTickerJob?.cancel()
        streamTickerJob = null
    }

    private fun stopStreamPlayback() {
        stopStreamTicker()
        try {
            streamEqualizer?.release()
        } catch (_: Exception) {}
        streamEqualizer = null
        streamMediaPlayer?.let { mp ->
            try {
                if (mp.isPlaying) mp.stop()
                mp.reset()
                mp.release()
            } catch (_: Exception) {}
        }
        streamMediaPlayer = null
    }

    fun play(song: Song, isAutoCrossfade: Boolean = false) {
        val idx = playlist.indexOfFirst { it.id == song.id }
        if (idx != -1) {
            currentIndex = idx
            _currentIndexFlow.value = idx
        }
        try {
            val result = audioManager.requestAudioFocus(focusRequest)
            if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED &&
                result != AudioManager.AUDIOFOCUS_REQUEST_DELAYED) {
                Log.w(TAG, "Audio focus not granted ($result), proceeding with playback anyway on automotive headunit")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Audio focus request failed: ${e.message}")
        }

        autoCrossfadeTriggeredPath = null
        isTransitioning = false
        _currentSongFlow.value = song
        _positionFlow.value = 0L
        _durationFlow.value = if (song.duration > 0) song.duration else 0L

        playJob?.cancel()

        if (song.isStream()) {
            nativeBridge.stop()

            val outgoingPlayer = streamMediaPlayer
            val outgoingEq = streamEqualizer
            streamMediaPlayer = null
            streamEqualizer = null
            stopStreamTicker()

            if (outgoingPlayer != null) {
                if (isAutoCrossfade && crossfadeSeconds > 0) {
                    scope.launch(Dispatchers.Default) {
                        val fadeDurationMs = (crossfadeSeconds * 1000L).coerceAtLeast(500L)
                        val steps = 20
                        val stepDelay = fadeDurationMs / steps
                        for (i in (steps - 1) downTo 0) {
                            val vol = i.toFloat() / steps.toFloat()
                            try {
                                outgoingPlayer.setVolume(vol, vol)
                            } catch (_: Exception) {
                                break
                            }
                            delay(stepDelay)
                        }
                        try {
                            outgoingEq?.release()
                            if (outgoingPlayer.isPlaying) outgoingPlayer.stop()
                            outgoingPlayer.reset()
                            outgoingPlayer.release()
                        } catch (_: Exception) {}
                    }
                } else {
                    scope.launch(Dispatchers.Default) {
                        try {
                            outgoingPlayer.setVolume(0.3f, 0.3f)
                            delay(30L)
                            outgoingPlayer.setVolume(0.0f, 0.0f)
                            delay(20L)
                            outgoingEq?.release()
                            if (outgoingPlayer.isPlaying) outgoingPlayer.stop()
                            outgoingPlayer.reset()
                            outgoingPlayer.release()
                        } catch (_: Exception) {}
                    }
                }
            }

            val mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK)
                if (isAutoCrossfade && crossfadeSeconds > 0) {
                    setVolume(0.0f, 0.0f)
                } else {
                    setVolume(1.0f, 1.0f)
                }
                try {
                    setDataSource(song.path)
                    setOnPreparedListener { player ->
                        try {
                            player.start()
                            _isPlayingFlow.value = true
                            val dur = if (player.duration > 0) player.duration.toLong() else if (song.duration > 0) song.duration else 0L
                            _durationFlow.value = dur
                            startStreamTicker()
                            initStreamEqualizer(player.audioSessionId)

                            if (isAutoCrossfade && crossfadeSeconds > 0) {
                                scope.launch(Dispatchers.Default) {
                                    val fadeDurationMs = (crossfadeSeconds * 1000L).coerceAtLeast(500L)
                                    val steps = 20
                                    val stepDelay = fadeDurationMs / steps
                                    for (i in 1..steps) {
                                        val vol = i.toFloat() / steps.toFloat()
                                        try {
                                            player.setVolume(vol, vol)
                                        } catch (_: Exception) {
                                            break
                                        }
                                        delay(stepDelay)
                                    }
                                    try {
                                        player.setVolume(1.0f, 1.0f)
                                    } catch (_: Exception) {}
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to start streaming playback: ${e.message}")
                            _isPlayingFlow.value = false
                        }
                    }
                    setOnErrorListener { _, what, extra ->
                        Log.e(TAG, "Streaming error: what=$what extra=$extra")
                        _isPlayingFlow.value = false
                        stopStreamTicker()
                        true
                    }
                    setOnCompletionListener {
                        _isPlayingFlow.value = false
                        stopStreamTicker()
                        onCompletion?.invoke(false)
                        onTrackEnded()
                    }
                    prepareAsync()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to initialize stream player: ${e.message}")
                    _isPlayingFlow.value = false
                }
            }
            streamMediaPlayer = mp
        } else {
            val outgoingPlayer = streamMediaPlayer
            val outgoingEq = streamEqualizer
            streamMediaPlayer = null
            streamEqualizer = null
            stopStreamTicker()
            if (outgoingPlayer != null) {
                scope.launch(Dispatchers.Default) {
                    try {
                        outgoingEq?.release()
                        if (outgoingPlayer.isPlaying) outgoingPlayer.stop()
                        outgoingPlayer.reset()
                        outgoingPlayer.release()
                    } catch (_: Exception) {}
                }
            }

            playJob = scope.launch(Dispatchers.IO) {
                val res = nativeBridge.playTrack(song.path, 0L, isAutoCrossfade)
                if (res == 0) {
                    _isPlayingFlow.value = true
                    val nativeDur = nativeBridge.getDuration()
                    if (nativeDur > 0) {
                        _durationFlow.value = nativeDur
                    }
                } else {
                    Log.e(TAG, "nativeBridge.playTrack returned error code $res for track: ${song.path}")
                }
            }
        }
    }

    fun pause() {
        if (_currentSongFlow.value?.isStream() == true && streamMediaPlayer != null) {
            try {
                streamMediaPlayer?.pause()
            } catch (_: Exception) {}
            _isPlayingFlow.value = false
            stopStreamTicker()
        } else {
            nativeBridge.pause()
            _isPlayingFlow.value = false
        }
        audioManager.abandonAudioFocusRequest(focusRequest)
    }

    fun stop() {
        playJob?.cancel()
        autoCrossfadeTriggeredPath = null
        isTransitioning = false
        stopStreamPlayback()
        nativeBridge.setVolume(1.0f)
        nativeBridge.stop()
        _positionFlow.value = 0L
        _isPlayingFlow.value = false
        _currentSongFlow.value = null
        audioManager.abandonAudioFocusRequest(focusRequest)
    }

    suspend fun isPrepared(): Boolean = withContext(Dispatchers.Main) {
        if (_currentSongFlow.value?.isStream() == true) {
            return@withContext streamMediaPlayer != null
        }
        val state = nativeBridge.getPlaybackState()
        state == 3 || state == 4 // PLAYING or PAUSED
    }

    fun resume() {
        try {
            audioManager.requestAudioFocus(focusRequest)
        } catch (e: Exception) {
            Log.w(TAG, "Audio focus request on resume failed: ${e.message}")
        }
        if (_currentSongFlow.value?.isStream() == true && streamMediaPlayer != null) {
            try {
                streamMediaPlayer?.start()
                _isPlayingFlow.value = true
                startStreamTicker()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to resume streaming: ${e.message}")
            }
        } else {
            nativeBridge.setVolume(1.0f)
            nativeBridge.resume()
            _isPlayingFlow.value = true
        }
    }

    fun togglePlayPause() {
        if (_isPlayingFlow.value) {
            pause()
        } else {
            resume()
        }
    }

    fun seekRelative(deltaMs: Long) {
        if (_currentSongFlow.value?.isStream() == true) {
            val dur = _durationFlow.value
            if (dur > 0L) {
                val curPos = _positionFlow.value
                val target = (curPos + deltaMs).coerceIn(0L, dur)
                seekTo(target)
            }
            return
        }
        val curPos = _positionFlow.value
        val dur = _durationFlow.value.coerceAtLeast(1L)
        val target = (curPos + deltaMs).coerceIn(0L, dur)
        seekTo(target)
    }

    fun seekTo(position: Long) {
        if (_currentSongFlow.value?.isStream() == true) {
            if (_durationFlow.value > 0L) {
                try {
                    streamMediaPlayer?.seekTo(position.toInt())
                    _positionFlow.value = position
                } catch (_: Exception) {}
            }
            return
        }
        nativeBridge.setVolume(1.0f)
        nativeBridge.seek(position)
        _positionFlow.value = position
    }

    fun setEqualizerEnabled(enabled: Boolean) {
        nativeBridge.setEqualizerEnabled(enabled)
        try {
            streamEqualizer?.enabled = enabled
            if (enabled) {
                syncStreamEqualizer()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error toggling stream equalizer: ${e.message}")
        }
    }

    fun isEqualizerEnabled(): Boolean = nativeBridge.isEqualizerEnabled()

    fun setEqualizerPreset(preset: Int) {
        nativeBridge.setEqualizerPreset(preset)
        syncStreamEqualizer()
    }

    fun getEqualizerPreset(): Int = nativeBridge.getEqualizerPreset()

    fun setEqualizerBandGain(band: Int, gainDb: Float) {
        nativeBridge.setEqualizerBandGain(band, gainDb)
        syncStreamEqualizer()
    }

    fun getEqualizerBandGain(band: Int): Float = nativeBridge.getEqualizerBandGain(band)

    fun setEqualizerHpfMode(mode: Int) {
        nativeBridge.setEqualizerHpfMode(mode)
    }

    fun getEqualizerHpfMode(): Int = nativeBridge.getEqualizerHpfMode()

    fun setTimeAlignment(mode: Int) {
        nativeBridge.setTimeAlignment(mode)
    }

    fun getTimeAlignment(): Int = nativeBridge.getTimeAlignment()

    fun setBassNormalizer(mode: Int) {
        nativeBridge.setBassNormalizer(mode)
    }

    fun getBassNormalizer(): Int = nativeBridge.getBassNormalizer()

    fun setVirtualSurroundMode(mode: Int) {
        nativeBridge.setVirtualSurroundMode(mode)
    }

    fun getVirtualSurroundMode(): Int = nativeBridge.getVirtualSurroundMode()

    fun setSpeedCompensation(speedKmh: Float, mode: Int) {
        nativeBridge.setSpeedCompensation(speedKmh, mode)
    }

    fun getSpeedCompensationDb(): Float = nativeBridge.getSpeedCompensationDb()

    fun setBalance(balance: Float) {
        nativeBridge.setBalance(balance)
    }

    fun getBalance(): Float = nativeBridge.getBalance()

    fun setFader(fader: Float) {
        nativeBridge.setFader(fader)
    }

    fun getFader(): Float = nativeBridge.getFader()

    fun release() {
        scope.coroutineContext[kotlinx.coroutines.Job]?.cancel()
        nativeBridge.setListener(null)
        try {
            streamEqualizer?.release()
        } catch (_: Exception) {}
        streamEqualizer = null
        try {
            volumeReceiver?.let { context.unregisterReceiver(it) }
            volumeReceiver = null
        } catch (_: Exception) {}
        audioManager.abandonAudioFocusRequest(focusRequest)
        nativeBridge.shutdown()
    }
}
