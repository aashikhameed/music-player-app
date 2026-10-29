package com.aashik.music.service

import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.media.MediaBrowserServiceCompat
import com.aashik.music.controller.MusicController
import com.aashik.music.data.MusicDatabase
import com.aashik.music.model.Song
import com.aashik.music.viewmodel.MusicViewModel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class CarPlaybackService : MediaBrowserServiceCompat() {

    private lateinit var mediaSession: MediaSessionCompat
    private var viewModel: MusicViewModel? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    var songList: List<Song> = emptyList()

    private val controllerListener: () -> Unit = {
        val currentSong = viewModel?.currentSong?.value ?: MusicController.currentSong
        val isPlaying = viewModel?.isPlaying?.value ?: MusicController.isPlaying
        updateNowPlayingMetadata(currentSong)
        updatePlaybackState(
            if (isPlaying)
                PlaybackStateCompat.STATE_PLAYING
            else
                PlaybackStateCompat.STATE_PAUSED
        )
    }

    companion object {
        var instance: CarPlaybackService? = null
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        serviceScope.launch(Dispatchers.IO) {
            songList = MusicDatabase.getDatabase(this@CarPlaybackService).songDao().getAllSongs()
        }

        mediaSession = MediaSessionCompat(this, "CarPlaybackService").apply {
            setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                        MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
            )
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    val player = MusicPlayer.getInstance(this@CarPlaybackService)
                    if (viewModel != null) {
                        viewModel?.resumeMusic()
                    } else {
                        player.resume()
                        MusicController.play(player.currentSongFlow.value)
                    }
                }

                override fun onPause() {
                    val player = MusicPlayer.getInstance(this@CarPlaybackService)
                    if (viewModel != null) {
                        viewModel?.pause()
                    } else {
                        player.pause()
                        MusicController.pause()
                    }
                }

                override fun onSkipToNext() {
                    if (viewModel != null) {
                        viewModel?.playNextSong()
                    } else {
                        MusicPlayer.getInstance(this@CarPlaybackService).playNext()
                    }
                }

                override fun onSkipToPrevious() {
                    if (viewModel != null) {
                        viewModel?.playPreviousSong()
                    } else {
                        MusicPlayer.getInstance(this@CarPlaybackService).playPrevious()
                    }
                }

                override fun onSeekTo(pos: Long) {
                    MusicPlayer.getInstance(this@CarPlaybackService).seekTo(pos)
                }

                override fun onStop() {
                    val player = MusicPlayer.getInstance(this@CarPlaybackService)
                    player.stop()
                    stopSelf()
                }
            })
            isActive = true
        }

        MusicController.addListener(controllerListener)
    }


    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot? {
        return BrowserRoot("root", null)
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        result.sendResult(mutableListOf())
    }

    fun setViewModel(vm: MusicViewModel) {
        this.viewModel = vm
        updateNowPlayingMetadata(vm.currentSong.value)
        updatePlaybackState(
            if (vm.isPlaying.value)
                PlaybackStateCompat.STATE_PLAYING
            else
                PlaybackStateCompat.STATE_PAUSED
        )
    }

    private fun updateNowPlayingMetadata(song: Song?) {
        song ?: return
        mediaSession.setMetadata(
            MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, song.title)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, song.artist)
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, if (song.duration > 0) song.duration else -1L)
                .build()
        )
    }

    private fun updatePlaybackState(state: Int) {
        val player = MusicPlayer.getInstance(this)
        val pos = player.positionFlow.value
        val speed = if (state == PlaybackStateCompat.STATE_PLAYING) 1f else 0f
        mediaSession.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setActions(
                    PlaybackStateCompat.ACTION_PLAY or
                            PlaybackStateCompat.ACTION_PAUSE or
                            PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                            PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                            PlaybackStateCompat.ACTION_SEEK_TO or
                            PlaybackStateCompat.ACTION_STOP
                )
                .setState(state, pos.coerceAtLeast(0L), speed, android.os.SystemClock.elapsedRealtime())
                .build()
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        MusicController.removeListener(controllerListener)
        serviceScope.cancel()
        instance = null
        viewModel = null
        mediaSession.release()
    }
}
