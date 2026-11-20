package com.aashik.music.wear.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.wear.ongoing.OngoingActivity
import androidx.wear.ongoing.Status
import com.aashik.music.wear.R
import com.aashik.music.wear.player.WearMusicPlayer
import com.aashik.music.wear.presentation.WearMainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class WearPlaybackService : Service() {

    companion object {
        const val CHANNEL_ID = "wear_music_playback"
        const val NOTIFICATION_ID = 2001
        const val ACTION_PLAY = "com.aashik.music.wear.ACTION_PLAY"
        const val ACTION_PAUSE = "com.aashik.music.wear.ACTION_PAUSE"
        const val ACTION_NEXT = "com.aashik.music.wear.ACTION_NEXT"
        const val ACTION_PREVIOUS = "com.aashik.music.wear.ACTION_PREVIOUS"
    }

    private var mediaSession: MediaSessionCompat? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    override fun onCreate() {
        super.onCreate()
        WearMusicPlayer.initialize(this)
        createNotificationChannel()
        initMediaSession()
        observePlayerState()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> WearMusicPlayer.play()
            ACTION_PAUSE -> WearMusicPlayer.pause()
            ACTION_NEXT -> WearMusicPlayer.next()
            ACTION_PREVIOUS -> WearMusicPlayer.previous()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Wear Music Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background playback for Wear OS music player"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun initMediaSession() {
        mediaSession = MediaSessionCompat(this, "WearMusicSession").apply {
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() = WearMusicPlayer.play()
                override fun onPause() = WearMusicPlayer.pause()
                override fun onSkipToNext() = WearMusicPlayer.next()
                override fun onSkipToPrevious() = WearMusicPlayer.previous()
            })
            isActive = true
        }
    }

    private fun observePlayerState() {
        serviceScope.launch {
            WearMusicPlayer.state.collectLatest { state ->
                val notification = buildNotification(state.currentSong?.title, state.currentSong?.artist, state.isPlaying)
                startForeground(NOTIFICATION_ID, notification)

                val pbState = if (state.isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
                mediaSession?.setPlaybackState(
                    PlaybackStateCompat.Builder()
                        .setActions(
                            PlaybackStateCompat.ACTION_PLAY or
                            PlaybackStateCompat.ACTION_PAUSE or
                            PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                            PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                        )
                        .setState(pbState, state.positionMs, 1.0f)
                        .build()
                )
            }
        }
    }

    private fun buildNotification(title: String?, artist: String?, isPlaying: Boolean): Notification {
        val openAppIntent = Intent(this, WearMainActivity::class.java)
        val openAppPendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseAction = if (isPlaying) {
            val pauseIntent = Intent(this, WearPlaybackService::class.java).apply { action = ACTION_PAUSE }
            val pausePendingIntent = PendingIntent.getService(this, 1, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            NotificationCompat.Action.Builder(android.R.drawable.ic_media_pause, "Pause", pausePendingIntent).build()
        } else {
            val playIntent = Intent(this, WearPlaybackService::class.java).apply { action = ACTION_PLAY }
            val playPendingIntent = PendingIntent.getService(this, 2, playIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            NotificationCompat.Action.Builder(android.R.drawable.ic_media_play, "Play", playPendingIntent).build()
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title ?: "Music Player")
            .setContentText(artist ?: "Wear OS")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(openAppPendingIntent)
            .setOngoing(isPlaying)
            .addAction(playPauseAction)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        // Build OngoingActivity for Wear OS watch face indicator
        val ongoingActivityStatus = Status.Builder()
            .addTemplate(title ?: "Playing Music")
            .build()

        val ongoingActivity = OngoingActivity.Builder(this, NOTIFICATION_ID, builder)
            .setStaticIcon(R.drawable.ic_launcher_foreground)
            .setTouchIntent(openAppPendingIntent)
            .setStatus(ongoingActivityStatus)
            .build()
        ongoingActivity.apply(this)

        return builder.build()
    }

    override fun onDestroy() {
        mediaSession?.release()
        super.onDestroy()
    }
}
