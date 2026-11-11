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

    private val _isTrackBufferingFlow = MutableStateFlow(false)
    val isTrackBufferingFlow: StateFlow<Boolean> = _isTrackBufferingFlow

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    var onCompletion: ((isAutoCrossfade: Boolean) -> Unit)? = null

    private var volumeReceiver: BroadcastReceiver? = null
    private var playlist: List<Song> = emptyList()
    private var currentIndex: Int = -1

    @Volatile
    var userQueue: List<Song> = emptyList()

    @Volatile
    var activeRadioStationName: String? = null

    fun getPlaylist(): List<Song> = playlist
    fun getCurrentIndex(): Int = currentIndex

    // ── Crossfade State (default 3 seconds) ──
    private var crossfadeSeconds: Int = 3
    private var isTransitioning = false
    private var autoCrossfadeTriggeredPath: String? = null
    private var trackStartTimeMs: Long = 0L
    private var playJob: Job? = null
    private var streamMediaPlayer: MediaPlayer? = null
    private var streamEqualizer: Equalizer? = null
    private var streamTickerJob: Job? = null
    @Volatile
    private var streamElapsedMs: Long = 0L

    @Volatile
    private var isAudioSilencedForVideo: Boolean = false

    // Equalizer preset definitions matching C++ AudioEqualizer DSP
    private val PRESET_GAINS = arrayOf(
        floatArrayOf(0.0f, 0.0f, 0.0f, 0.0f, 0.0f),      // 0: Flat
        floatArrayOf(1.0f, -4.0f, 1.5f, 2.5f, 3.5f),     // 1: Acoustic
        floatArrayOf(6.0f, 3.0f, 0.0f, 0.0f, 0.0f),      // 2: Bass Booster
        floatArrayOf(-6.0f, -3.0f, 0.0f, 0.0f, 0.0f),    // 3: Bass Reducer
        floatArrayOf(3.0f, 0.0f, 0.0f, 2.0f, 3.5f),      // 4: Classical
        floatArrayOf(5.5f, 0.0f, 2.0f, 4.0f, 3.5f),      // 5: Dance
        floatArrayOf(4.5f, 1.0f, -1.0f, -2.0f, -4.0f),   // 6: Deep
        floatArrayOf(4.0f, 0.0f, 1.5f, 3.5f, 4.0f),      // 7: Electronic
        floatArrayOf(5.5f, 1.0f, 1.0f, 2.0f, 2.0f),      // 8: Hip-Hop
        floatArrayOf(2.5f, 1.0f, 2.0f, 2.5f, 2.5f),      // 9: Jazz
        floatArrayOf(2.5f, -1.5f, 2.0f, 3.5f, 2.0f),     // 10: Latin
        floatArrayOf(4.5f, 0.0f, 0.0f, 2.5f, 4.5f),      // 11: Loudness
        floatArrayOf(1.0f, 2.0f, 1.0f, 2.0f, -2.0f),     // 12: Lounge
        floatArrayOf(2.5f, 1.0f, 3.0f, 2.5f, 2.0f),      // 13: Piano
        floatArrayOf(1.5f, 3.5f, 1.0f, 3.0f, 2.5f),      // 14: Pop
        floatArrayOf(6.0f, -1.0f, 2.0f, 3.0f, 3.0f),     // 15: R&B
        floatArrayOf(3.5f, -2.0f, 1.0f, 3.0f, 4.0f),     // 16: Rock
        floatArrayOf(-5.0f, 1.0f, 3.0f, 3.0f, 1.0f),     // 17: Small Speakers
        floatArrayOf(-5.0f, -1.0f, 4.0f, 2.0f, -2.0f),    // 18: Spoken Word
        floatArrayOf(0.0f, 0.0f, 1.0f, 4.0f, 6.0f),      // 19: Treble Booster
        floatArrayOf(0.0f, 0.0f, -1.0f, -4.0f, -6.0f),   // 20: Treble Reducer
        floatArrayOf(-2.0f, -5.0f, 3.0f, 2.0f, 1.0f),    // 21: Vocal Booster
        floatArrayOf(2.0f, 0.0f, 1.5f, 1.0f, -1.0f)      // 22: Late Night
    )

    init {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(attrs)
            .setOnAudioFocusChangeListener { focusChange ->
                if (isAudioSilencedForVideo) {
                    // Video player in WebView owns the audio session; ignore focus changes
                    return@setOnAudioFocusChangeListener
                }
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
        // While a stream is current, MediaPlayer owns playback. The native engine's async
        // "stopped" callback (fired by nativeBridge.stop() when switching tracks) can arrive after
        // the new stream has already started and would wrongly flip the UI to paused.
        if (_currentSongFlow.value?.isStream() == true) return
        when (state) {
            3 -> _isPlayingFlow.value = true
            4, 5, 6 -> _isPlayingFlow.value = false
        }
    }

    override fun onPositionChanged(positionMs: Long) {
        if (isAudioSilencedForVideo) return
        _positionFlow.value = positionMs

        if (crossfadeSeconds > 0 && _isPlayingFlow.value && !isTransitioning) {
            val now = System.currentTimeMillis()
            val minPlayTime = ((crossfadeSeconds + 1) * 1000L).coerceAtLeast(3000L)
            // Ignore position reports during the initial warm-up period after a song change
            if (now - trackStartTimeMs < minPlayTime) {
                return
            }
            val dur = _durationFlow.value
            val crossfadeMs = crossfadeSeconds * 1000L
            // Only crossfade if the song is longer than crossfade duration + 2 seconds,
            // and the playback position is genuinely in the second half of the track.
            if (dur > (crossfadeMs + 2000L) && positionMs > (dur / 2)) {
                val remainingMs = dur - positionMs
                if (remainingMs in 0..crossfadeMs) {
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
                                delay(1200L)
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
        val now = System.currentTimeMillis()
        // If track started less than 3s ago, any onTrackEnded is a stale callback from the previous track!
        if (now - trackStartTimeMs < 3000L) {
            return
        }
        val currentPath = _currentSongFlow.value?.path
        if (currentPath != null && currentPath != autoCrossfadeTriggeredPath && !isTransitioning) {
            isTransitioning = true
            autoCrossfadeTriggeredPath = currentPath
            scope.launch(Dispatchers.Main) {
                try {
                    if (onCompletion != null) {
                        onCompletion?.invoke(false)
                    } else {
                        playNext(isAutoCrossfade = false)
                    }
                } finally {
                    delay(800L)
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
        if (userQueue.isNotEmpty()) {
            val nextSong = userQueue.first()
            userQueue = userQueue.drop(1)
            val idx = playlist.indexOfFirst { it.id == nextSong.id }
            if (idx != -1) {
                currentIndex = idx
                _currentIndexFlow.value = idx
            }
            play(nextSong, isAutoCrossfade)
            com.aashik.music.controller.MusicController.play(nextSong)
            return
        }
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

            val gains = if (currentPreset in PRESET_GAINS.indices) {
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
        if (isAudioSilencedForVideo) return
        streamTickerJob?.cancel()
        streamTickerJob = scope.launch(Dispatchers.Default) {
            while (streamMediaPlayer != null && _isPlayingFlow.value && !isAudioSilencedForVideo) {
                delay(250L)
                val curPos = try { streamMediaPlayer?.currentPosition?.toLong() } catch (_: Exception) { null }
                val pos = if (curPos != null && curPos > 0L) {
                    streamElapsedMs = curPos
                    curPos
                } else {
                    streamElapsedMs += 250L
                    streamElapsedMs
                }
                _positionFlow.value = pos

                // Automatic Crossfade Detection (Automotive parity for streaming tracks)
                if (crossfadeSeconds > 0 && _isPlayingFlow.value && !isTransitioning) {
                    val now = System.currentTimeMillis()
                    val minPlayTime = ((crossfadeSeconds + 1) * 1000L).coerceAtLeast(3000L)
                    if (now - trackStartTimeMs >= minPlayTime) {
                        val dur = _durationFlow.value
                        val crossfadeMs = crossfadeSeconds * 1000L
                        if (dur > (crossfadeMs + 2000L) && pos > (dur / 2)) {
                            val remainingMs = dur - pos
                            if (remainingMs in 0..crossfadeMs) {
                                val currentPath = _currentSongFlow.value?.path
                                if (currentPath != null && currentPath != autoCrossfadeTriggeredPath) {
                                    autoCrossfadeTriggeredPath = currentPath
                                    isTransitioning = true
                                    withContext(Dispatchers.Main) {
                                        try {
                                            if (onCompletion != null) {
                                                onCompletion?.invoke(true)
                                            } else {
                                                playNext(isAutoCrossfade = true)
                                            }
                                        } finally {
                                            delay(crossfadeMs + 800L)
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

    fun setAudioSilencedForVideo(silenced: Boolean) {
        val wasSilenced = isAudioSilencedForVideo
        isAudioSilencedForVideo = silenced
        if (silenced) {
            streamTickerJob?.cancel()
            streamTickerJob = null
            nativeBridge.pause()
            try {
                if (streamMediaPlayer?.isPlaying == true) {
                    streamMediaPlayer?.pause()
                }
                streamMediaPlayer?.setVolume(0.0f, 0.0f)
            } catch (_: Exception) {}
            try { audioManager.abandonAudioFocusRequest(focusRequest) } catch (_: Exception) {}
        } else if (wasSilenced) {
            val song = _currentSongFlow.value
            val pos = _positionFlow.value
            if (song != null) {
                if (song.isStream()) {
                    if (streamMediaPlayer != null) {
                        try {
                            audioManager.requestAudioFocus(focusRequest)
                            streamMediaPlayer?.setVolume(1.0f, 1.0f)
                            if (_isPlayingFlow.value) {
                                streamMediaPlayer?.start()
                                startStreamTicker()
                            }
                            if (pos > 0L) {
                                seekTo(pos)
                            }
                        } catch (e: Exception) {
                            play(song)
                            if (pos > 0L) seekTo(pos)
                        }
                    } else {
                        play(song)
                        if (pos > 0L) seekTo(pos)
                    }
                } else {
                    nativeBridge.setVolume(1.0f)
                    if (_isPlayingFlow.value) {
                        nativeBridge.resume()
                    }
                    if (pos > 0L) {
                        nativeBridge.seek(pos)
                    }
                }
            }
        }
    }

    fun isAudioSilencedForVideo(): Boolean = isAudioSilencedForVideo

    fun resetPlaybackForNewTrack(song: Song) {
        _currentSongFlow.value = song
        _positionFlow.value = 0L
        _durationFlow.value = if (song.duration > 0) song.duration else 0L
        if (isAudioSilencedForVideo) {
            streamTickerJob?.cancel()
            streamTickerJob = null
            _isPlayingFlow.value = true
        }
    }

    fun updateVideoPlaybackState(isPlaying: Boolean, positionMs: Long, durationMs: Long) {
        if (isAudioSilencedForVideo) {
            _isPlayingFlow.value = isPlaying
            if (positionMs >= 0L) {
                _positionFlow.value = positionMs
            }
            if (durationMs > 0L) {
                _durationFlow.value = durationMs
            }
        }
    }

    fun play(song: Song, isAutoCrossfade: Boolean = false) {
        val idx = playlist.indexOfFirst { it.id == song.id }
        if (idx != -1) {
            currentIndex = idx
            _currentIndexFlow.value = idx
        }
        _currentSongFlow.value = song
        _positionFlow.value = 0L
        _durationFlow.value = song.duration

        if (isAudioSilencedForVideo) {
            // Video mode handles playback via WebView; bypass native & streaming engines completely
            _isPlayingFlow.value = true
            return
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

        trackStartTimeMs = System.currentTimeMillis()
        if (!isAutoCrossfade) {
            autoCrossfadeTriggeredPath = null
        }
        isTransitioning = true
        scope.launch {
            delay(1000L)
            isTransitioning = false
        }
        _currentSongFlow.value = song
        _positionFlow.value = 0L
        _durationFlow.value = if (song.duration > 0) song.duration else 0L

        playJob?.cancel()

        if (song.isStream()) {
            _isTrackBufferingFlow.value = true
            nativeBridge.stop()

            val outgoingPlayer = streamMediaPlayer
            val outgoingEq = streamEqualizer
            streamMediaPlayer = null
            streamEqualizer = null
            stopStreamTicker()

            if (outgoingPlayer != null) {
                // Instantly unregister all listeners to prevent stale callbacks from triggering unwanted song jumps
                try {
                    outgoingPlayer.setOnCompletionListener(null)
                    outgoingPlayer.setOnErrorListener(null)
                    outgoingPlayer.setOnPreparedListener(null)
                } catch (_: Exception) {}

                val fadeDurationMs = if (isAutoCrossfade && crossfadeSeconds > 0) {
                    crossfadeSeconds * 1000L
                } else if (crossfadeSeconds > 0) {
                    500L // Quick smooth micro-crossfade on manual skip
                } else {
                    60L
                }

                scope.launch(Dispatchers.Default) {
                    try {
                        val steps = (fadeDurationMs / 40L).toInt().coerceAtLeast(1)
                        val stepDelay = fadeDurationMs / steps
                        for (i in 1..steps) {
                            val progress = i.toFloat() / steps.toFloat()
                            // Equal-power cosine decay
                            val vol = Math.cos(progress * Math.PI / 2.0).toFloat().coerceIn(0f, 1f)
                            try {
                                outgoingPlayer.setVolume(vol, vol)
                            } catch (_: Exception) {
                                break
                            }
                            delay(stepDelay)
                        }
                    } catch (_: Exception) {
                    } finally {
                        try {
                            outgoingEq?.release()
                            if (outgoingPlayer.isPlaying) outgoingPlayer.stop()
                            outgoingPlayer.reset()
                            outgoingPlayer.release()
                        } catch (_: Exception) {}
                    }
                }
            }

            val shouldFadeIn = (crossfadeSeconds > 0) && (isAutoCrossfade || outgoingPlayer != null)

            // Use the stream that was buffered ahead of time when this is the track we expected;
            // otherwise build and prepare one now.
            val holder = takePreloadedStream(song.path) ?: buildStreamHolder(song)
            // Must be set before activating: an already-prepared stream starts synchronously and
            // the position ticker stops itself if it finds no current player.
            streamMediaPlayer = holder.mp
            activateStream(holder, song, isAutoCrossfade, shouldFadeIn)
        } else {
            _isTrackBufferingFlow.value = false
            discardPreloadedStream()
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
                if (isAudioSilencedForVideo) return@launch
                val res = nativeBridge.playTrack(song.path, 0L, isAutoCrossfade)
                if (res == 0) {
                    if (isAudioSilencedForVideo) {
                        nativeBridge.pause()
                        return@launch
                    }
                    _isPlayingFlow.value = true
                    if (isAudioSilencedForVideo) {
                        nativeBridge.setVolume(0.0f)
                    }
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

    // ── Stream pre-buffering ────────────────────────────────────────────────
    // The next online track is prepared in a paused MediaPlayer while the current one plays, so
    // "Next" starts it instantly instead of waiting for the network.

    private class StreamHolder(val path: String, val mp: MediaPlayer) {
        @Volatile var prepared = false
        @Volatile var activated = false
        @Volatile var failed = false
        var isAutoCrossfade = false
        var shouldFadeIn = false
    }

    private var preloadedStream: StreamHolder? = null

    /** Starts buffering [song] (a direct-playable stream) without playing it. Call on the main thread. */
    fun preloadStream(song: Song) {
        if (!song.isStream() || !song.isDirectPlayable() || isAudioSilencedForVideo) return
        if (preloadedStream?.path == song.path && preloadedStream?.failed == false) return
        discardPreloadedStream()
        preloadedStream = buildStreamHolder(song)
    }

    private fun takePreloadedStream(path: String): StreamHolder? {
        val h = preloadedStream ?: return null
        preloadedStream = null
        if (h.path == path && !h.failed) return h
        releaseHolder(h)
        return null
    }

    private fun discardPreloadedStream() {
        preloadedStream?.let { releaseHolder(it) }
        preloadedStream = null
    }

    private fun releaseHolder(h: StreamHolder) {
        try {
            h.mp.setOnCompletionListener(null)
            h.mp.setOnErrorListener(null)
            h.mp.setOnPreparedListener(null)
            h.mp.reset()
            h.mp.release()
        } catch (_: Exception) {}
    }

    /** Creates a stream player and begins preparing it. Callbacks only touch shared state once activated. */
    private fun buildStreamHolder(song: Song): StreamHolder {
        val mp = MediaPlayer()
        val holder = StreamHolder(song.path, mp)
        try {
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            mp.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK)
            mp.setVolume(1.0f, 1.0f)
            mp.setDataSource(song.path)
            mp.setOnPreparedListener { player ->
                if (holder.activated) startStreamPlayback(player, song, holder) else holder.prepared = true
            }
            mp.setOnErrorListener { _, what, extra ->
                Log.e(TAG, "Streaming error: what=$what extra=$extra")
                if (holder.activated) {
                    _isTrackBufferingFlow.value = false
                    _isPlayingFlow.value = false
                    stopStreamTicker()
                } else {
                    holder.failed = true
                }
                true
            }
            mp.setOnCompletionListener {
                if (holder.activated) {
                    _isTrackBufferingFlow.value = false
                    _isPlayingFlow.value = false
                    stopStreamTicker()
                    onTrackEnded()
                }
            }
            mp.prepareAsync()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize stream player: ${e.message}")
            holder.failed = true
        }
        return holder
    }

    /** Makes [holder] the playing stream: starts immediately if already prepared, else on prepare. */
    private fun activateStream(holder: StreamHolder, song: Song, isAutoCrossfade: Boolean, shouldFadeIn: Boolean) {
        holder.isAutoCrossfade = isAutoCrossfade
        holder.shouldFadeIn = shouldFadeIn
        if (holder.failed) {
            _isTrackBufferingFlow.value = false
            _isPlayingFlow.value = false
            return
        }
        try { holder.mp.setVolume(if (shouldFadeIn) 0.0f else 1.0f, if (shouldFadeIn) 0.0f else 1.0f) } catch (_: Exception) {}
        holder.activated = true
        if (holder.prepared) startStreamPlayback(holder.mp, song, holder)
    }

    private fun startStreamPlayback(player: MediaPlayer, song: Song, holder: StreamHolder) {
        val isAutoCrossfade = holder.isAutoCrossfade
        val shouldFadeIn = holder.shouldFadeIn
        try {
            _isTrackBufferingFlow.value = false
            if (isAudioSilencedForVideo) {
                try { player.pause() } catch (_: Exception) {}
                try { player.setVolume(0.0f, 0.0f) } catch (_: Exception) {}
                return
            }
            player.start()
            _isPlayingFlow.value = true
            val dur = if (player.duration > 0) player.duration.toLong() else if (song.duration > 0) song.duration else 0L
            _durationFlow.value = dur
            startStreamTicker()
            initStreamEqualizer(player.audioSessionId)

            if (shouldFadeIn) {
                scope.launch(Dispatchers.Default) {
                    val fadeDurationMs = if (isAutoCrossfade) (crossfadeSeconds * 1000L).coerceAtLeast(500L) else 500L
                    val steps = (fadeDurationMs / 40L).toInt().coerceAtLeast(1)
                    val stepDelay = fadeDurationMs / steps
                    for (i in 1..steps) {
                        val progress = i.toFloat() / steps.toFloat()
                        // Equal-power sine curve
                        val vol = Math.sin(progress * Math.PI / 2.0).toFloat().coerceIn(0f, 1f)
                        try {
                            player.setVolume(vol, vol)
                        } catch (_: Exception) {
                            break
                        }
                        delay(stepDelay)
                    }
                    try {
                        val targetVol = if (isAudioSilencedForVideo) 0.0f else 1.0f
                        player.setVolume(targetVol, targetVol)
                    } catch (_: Exception) {}
                }
            } else if (isAudioSilencedForVideo) {
                try { player.setVolume(0.0f, 0.0f) } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start streaming playback: ${e.message}")
            _isPlayingFlow.value = false
        }
    }

    fun pause() {
        _isTrackBufferingFlow.value = false
        _isPlayingFlow.value = false
        if (isAudioSilencedForVideo) {
            return
        }
        if (_currentSongFlow.value?.isStream() == true && streamMediaPlayer != null) {
            try {
                streamMediaPlayer?.pause()
            } catch (_: Exception) {}
            stopStreamTicker()
        } else {
            nativeBridge.pause()
        }
        audioManager.abandonAudioFocusRequest(focusRequest)
    }

    fun stop() {
        _isTrackBufferingFlow.value = false
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
        if (isAudioSilencedForVideo) {
            _isPlayingFlow.value = true
            return
        }
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
        val clampedPos = position.coerceAtLeast(0L)
        _positionFlow.value = clampedPos
        if (isAudioSilencedForVideo) {
            return
        }
        if (_currentSongFlow.value?.isStream() == true) {
            streamElapsedMs = clampedPos
            if (_durationFlow.value > 0L) {
                try {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        streamMediaPlayer?.seekTo(clampedPos, android.media.MediaPlayer.SEEK_CLOSEST)
                    } else {
                        streamMediaPlayer?.seekTo(clampedPos.toInt())
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Stream seekTo failed: ${e.message}")
                }
            }
            return
        }
        val vol = if (isAudioSilencedForVideo) 0.0f else 1.0f
        nativeBridge.setVolume(vol)
        nativeBridge.seek(clampedPos)
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

    fun setRepeatMode(mode: Int) {
        nativeBridge.setRepeatMode(mode)
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
        discardPreloadedStream()
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
