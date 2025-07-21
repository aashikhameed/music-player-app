package com.aashik.music.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import androidx.media.session.MediaButtonReceiver
import com.aashik.music.R
import com.aashik.music.controller.MusicController
import com.aashik.music.model.Song
import com.aashik.music.viewmodel.MusicViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class NotificationPlaybackService : Service() {

    companion object {
        private const val CHANNEL_ID = "music_playback_notification"
        private const val NOTIFICATION_ID = 1
        var instance: NotificationPlaybackService? = null

        fun startService(context: Context) {
            val intent = Intent(context, NotificationPlaybackService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    private val binder = LocalBinder()
    private lateinit var mediaSession: MediaSessionCompat
    private var viewModel: MusicViewModel? = null

    inner class LocalBinder : Binder() {
        fun getService(): NotificationPlaybackService = this@NotificationPlaybackService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        setupMediaSession()
        instance = this

        MusicController.onStateChanged = {
            updateMetadata(MusicController.currentSong)
            updateNotification()
            updatePlaybackState(
                if (MusicController.isPlaying)
                    PlaybackStateCompat.STATE_PLAYING
                else
                    PlaybackStateCompat.STATE_PAUSED
            )
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        MediaButtonReceiver.handleIntent(mediaSession, intent)
        return START_NOT_STICKY
    }

    fun setViewModel(vm: MusicViewModel) {
        this.viewModel = vm

        CoroutineScope(Dispatchers.Main).launch {
            vm.currentSong.collectLatest { song ->
                song?.let {
                    updateMetadata(it)
                    updateNotification()
                }
            }
        }

        CoroutineScope(Dispatchers.Main).launch {
            vm.isPlaying.collectLatest {
                updateNotification()
                updatePlaybackState(if (it) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED)
            }
        }
    }

    private fun setupMediaSession() {
        mediaSession = MediaSessionCompat(this, "NotificationPlaybackService").apply {
            setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                        MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
            )
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    viewModel?.resumeMusic()
                }

                override fun onPause() {
                    viewModel?.pause()
                }

                override fun onSkipToNext() {
                    viewModel?.playNextSong()
                }

                override fun onSkipToPrevious() {
                    viewModel?.playPreviousSong()
                }

                override fun onStop() {
                    stopSelf()
                }
            })
            isActive = true
        }

        updatePlaybackState(PlaybackStateCompat.STATE_PAUSED)
    }


    private fun updatePlaybackState(state: Int) {
        mediaSession.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setActions(
                    PlaybackStateCompat.ACTION_PLAY or
                            PlaybackStateCompat.ACTION_PAUSE or
                            PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                            PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                            PlaybackStateCompat.ACTION_PLAY_PAUSE
                )
                .setState(state, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1f)
                .build()
        )
    }

    private fun updateMetadata(song: Song?) {
        if (song == null) return
        mediaSession.setMetadata(
            MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, song.title)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, song.artist)
                .build()
        )
    }

    private fun updateNotification() {
        val isPlaying = viewModel?.isPlaying?.value ?: false
        val notification = buildNotification(isPlaying)
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(isPlaying: Boolean): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(viewModel?.currentSong?.value?.title ?: "Unknown")
            .setContentText(viewModel?.currentSong?.value?.artist ?: "")
            .setSmallIcon(R.drawable.ic_music_note)
            .setContentIntent(openAppIntent())
            .addAction(
                NotificationCompat.Action(
                    R.drawable.ic_skip_previous, "Previous",
                    MediaButtonReceiver.buildMediaButtonPendingIntent(
                        this,
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                    )
                )
            )
            .addAction(
                if (isPlaying)
                    NotificationCompat.Action(
                        R.drawable.ic_pause, "Pause",
                        MediaButtonReceiver.buildMediaButtonPendingIntent(
                            this,
                            PlaybackStateCompat.ACTION_PAUSE
                        )
                    )
                else
                    NotificationCompat.Action(
                        R.drawable.ic_play, "Play",
                        MediaButtonReceiver.buildMediaButtonPendingIntent(
                            this,
                            PlaybackStateCompat.ACTION_PLAY
                        )
                    )
            )
            .addAction(
                NotificationCompat.Action(
                    R.drawable.ic_skip_next, "Next",
                    MediaButtonReceiver.buildMediaButtonPendingIntent(
                        this,
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                    )
                )
            )
            .setStyle(
                MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
//                    .setMediaButtonReceiver(
//                        MediaButtonReceiver.buildMediaButtonPendingIntent(
//                            this,
//                            PlaybackStateCompat.ACTION_PLAY_PAUSE
//                        )
//                    )
            )
            .setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    private fun openAppIntent(): PendingIntent {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        return PendingIntent.getActivity(
            this, 0, launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Playback Notification",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaSession.release()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        viewModel?.pause()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()            // stops the service
        super.onTaskRemoved(rootIntent)
    }
}
