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

class CarPlaybackService : MediaBrowserServiceCompat() {

    private lateinit var mediaSession: MediaSessionCompat
    private var viewModel: MusicViewModel? = null
    companion object {
        var instance: CarPlaybackService? = null
    }
    lateinit var songList: List<Song>

    override fun onCreate() {
        super.onCreate()
        songList = MusicDatabase.getDatabase(this@CarPlaybackService).songDao().getAllSongsBlocking()

        mediaSession = MediaSessionCompat(this, "CarPlaybackService").apply {
            setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                        MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
            )
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    MusicController.play()
                }

                override fun onPause() {
                    MusicController.pause()
                }

                override fun onSkipToNext() {
                    MusicController.next(songList) // Keep a local copy of songList
                }

                override fun onSkipToPrevious() {
                    MusicController.previous(songList)
                }

                override fun onStop() {
                    stopSelf()
                }
            })
            isActive = true
        }


        // Watch for playback state changes
        MusicController.onStateChanged = {
            val currentSong = viewModel?.currentSong?.value
            val isPlaying = viewModel?.isPlaying?.value ?: false
            updateNowPlayingMetadata(currentSong)
            updatePlaybackState(
                if (isPlaying)
                    PlaybackStateCompat.STATE_PLAYING
                else
                    PlaybackStateCompat.STATE_PAUSED
            )
        }
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
                .build()
        )
    }

    private fun updatePlaybackState(state: Int) {
        mediaSession.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setActions(
                    PlaybackStateCompat.ACTION_PLAY or
                            PlaybackStateCompat.ACTION_PAUSE or
                            PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                            PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                            PlaybackStateCompat.ACTION_STOP
                )
                .setState(state, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1f)
                .build()
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaSession.release()
    }
}
