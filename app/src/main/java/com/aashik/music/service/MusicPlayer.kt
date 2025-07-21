package com.aashik.music.service

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
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
    private var exoPlayer: ExoPlayer = ExoPlayer.Builder(context).build()

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


    init {
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlayingFlow.value = isPlaying
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    onCompletion?.invoke()
                } else if (playbackState == Player.STATE_READY) {
                    _durationFlow.value = exoPlayer.duration.takeIf { it > 0 } ?: 1L
                }
            }

//            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
//                val path = mediaItem?.mediaId
//                val song = songs.find { it.path == path }
//                if (song != null) {
//                    viewModel.setCurrentlyPlaying(song)
//                }
//            }
        })
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
        _currentSongFlow.value = song
        val item = MediaItem.Builder()
            .setUri(Uri.parse(song.path))
            .setMediaId(song.path) // Or use song.id if you have a unique ID
            .build()

        exoPlayer.setMediaItem(item)
        exoPlayer.prepare()
        exoPlayer.play()
        startTrackingProgress()
    }

    fun pause() {
        exoPlayer.pause()
        stopTrackingProgress()
    }


    suspend fun isPrepared(): Boolean = withContext(Dispatchers.Main) {
        exoPlayer.playbackState == Player.STATE_READY && !exoPlayer.isPlaying
    }

    fun resume() {
        exoPlayer.playWhenReady = true
        startTrackingProgress()
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
}
