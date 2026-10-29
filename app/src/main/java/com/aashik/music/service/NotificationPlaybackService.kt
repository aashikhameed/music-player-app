package com.aashik.music.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import androidx.media.session.MediaButtonReceiver
import com.aashik.music.R
import com.aashik.music.cache.AlbumArtCache
import com.aashik.music.controller.MusicController
import com.aashik.music.model.Song
import com.aashik.music.utils.loadAlbumArt
import com.aashik.music.viewmodel.MusicViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

/**
 * Automotive Media Playback Foreground Service.
 * Provides MediaSessionCompat integration for system media controls,
 * lock screen media seekbar, notifications with album art, and background playback persistence.
 */
class NotificationPlaybackService : Service() {

    companion object {
        private const val CHANNEL_ID = "music_playback_notification"
        private const val NOTIFICATION_ID = 1
        var instance: NotificationPlaybackService? = null

        fun startService(context: Context, customIntent: Intent? = null) {
            val intent = customIntent ?: Intent(context, NotificationPlaybackService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    private val binder = LocalBinder()
    private lateinit var mediaSession: MediaSessionCompat
    private val musicPlayer by lazy { MusicPlayer.getInstance(this) }
    private var viewModel: MusicViewModel? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var cachedLargeIcon: Bitmap? = null
    private var cachedLargeIconPath: String? = null

    private val controllerListener: () -> Unit = {
        val currentSong = musicPlayer.currentSongFlow.value ?: MusicController.currentSong
        val isPlaying = musicPlayer.isPlayingFlow.value
        updateMetadata(currentSong)
        updatePlaybackState(
            if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
        )
    }

    inner class LocalBinder : Binder() {
        fun getService(): NotificationPlaybackService = this@NotificationPlaybackService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        setupMediaSession()
        instance = this

        MusicController.addListener(controllerListener)
        observePlayerState()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        MediaButtonReceiver.handleIntent(mediaSession, intent)
        val currentSong = musicPlayer.currentSongFlow.value ?: MusicController.currentSong
        val isPlaying = musicPlayer.isPlayingFlow.value
        buildAndShowNotification(currentSong, isPlaying, cachedLargeIcon)
        return START_STICKY
    }

    fun setViewModel(vm: MusicViewModel) {
        this.viewModel = vm
    }

    private fun observePlayerState() {
        // 1. Track changes -> update metadata with artwork & duration
        serviceScope.launch {
            musicPlayer.currentSongFlow.collectLatest { song ->
                if (song != null) {
                    updateMetadata(song)
                }
            }
        }

        // 2. Play/Pause state changes -> update playback state & notification action buttons
        serviceScope.launch {
            musicPlayer.isPlayingFlow.collectLatest { isPlaying ->
                val state = if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
                updatePlaybackState(state)
                val currentSong = musicPlayer.currentSongFlow.value ?: MusicController.currentSong
                buildAndShowNotification(currentSong, isPlaying, cachedLargeIcon)
            }
        }

        // 3. Duration updates -> ensure MediaMetadata duration matches exact hardware duration
        serviceScope.launch {
            musicPlayer.durationFlow.collectLatest { durationMs ->
                val song = musicPlayer.currentSongFlow.value
                if (song != null && durationMs > 0) {
                    updateMetadata(song, durationMs)
                }
            }
        }

        // 4. Seek position drift sync -> synchronize MediaSession position periodically without excessive IPC
        serviceScope.launch {
            var lastSyncedPos = -10000L
            musicPlayer.positionFlow.collectLatest { pos ->
                if (abs(pos - lastSyncedPos) >= 3000L) {
                    lastSyncedPos = pos
                    val state = if (musicPlayer.isPlayingFlow.value) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
                    updatePlaybackState(state, pos)
                }
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
                    if (viewModel != null) {
                        viewModel?.resumeMusic()
                    } else {
                        musicPlayer.resume()
                        MusicController.play(musicPlayer.currentSongFlow.value)
                    }
                }

                override fun onPause() {
                    if (viewModel != null) {
                        viewModel?.pause()
                    } else {
                        musicPlayer.pause()
                        MusicController.pause()
                    }
                }

                override fun onSkipToNext() {
                    if (viewModel != null) {
                        viewModel?.playNextSong()
                    } else {
                        musicPlayer.playNext()
                    }
                }

                override fun onSkipToPrevious() {
                    if (viewModel != null) {
                        viewModel?.playPreviousSong()
                    } else {
                        musicPlayer.playPrevious()
                    }
                }

                override fun onSeekTo(pos: Long) {
                    musicPlayer.seekTo(pos)
                    val state = if (musicPlayer.isPlayingFlow.value) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
                    updatePlaybackState(state, pos)
                }

                override fun onStop() {
                    musicPlayer.stop()
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            })
            isActive = true
        }

        updatePlaybackState(PlaybackStateCompat.STATE_PAUSED)
    }

    /**
     * Updates MediaSessionCompat playback state with real-time seek position, speed, and seek action.
     */
    private fun updatePlaybackState(state: Int, positionMs: Long = -1L) {
        val isPlaying = state == PlaybackStateCompat.STATE_PLAYING
        val currentPos = if (positionMs >= 0) positionMs else musicPlayer.positionFlow.value
        val playbackSpeed = if (isPlaying) 1.0f else 0.0f

        val actions = PlaybackStateCompat.ACTION_PLAY or
                PlaybackStateCompat.ACTION_PAUSE or
                PlaybackStateCompat.ACTION_PLAY_PAUSE or
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                PlaybackStateCompat.ACTION_SEEK_TO or
                PlaybackStateCompat.ACTION_STOP

        val playbackState = PlaybackStateCompat.Builder()
            .setActions(actions)
            .setState(state, currentPos.coerceAtLeast(0L), playbackSpeed, SystemClock.elapsedRealtime())
            .build()

        mediaSession.setPlaybackState(playbackState)
    }

    /**
     * Updates MediaSessionCompat metadata with song title, artist, album, duration, and artwork.
     */
    private fun updateMetadata(song: Song?, durationMs: Long = -1L) {
        if (song == null) return
        val dur = if (durationMs > 0) durationMs else (if (song.duration > 0) song.duration else musicPlayer.durationFlow.value)

        serviceScope.launch(Dispatchers.IO) {
            val artBitmap = getAlbumArtBitmap(song.path)
            withContext(Dispatchers.Main) {
                val metadata = MediaMetadataCompat.Builder()
                    .putString(MediaMetadataCompat.METADATA_KEY_TITLE, song.title.ifEmpty { "Unknown Title" })
                    .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, song.artist.ifEmpty { "Unknown Artist" })
                    .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, song.album.ifEmpty { "Unknown Album" })
                    .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, song.title.ifEmpty { "Unknown Title" })
                    .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, song.artist.ifEmpty { "Unknown Artist" })
                    .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION, song.album)
                    .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, if (dur > 0) dur else -1L)
                    .apply {
                        if (artBitmap != null) {
                            putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, artBitmap)
                            putBitmap(MediaMetadataCompat.METADATA_KEY_ART, artBitmap)
                            putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, artBitmap)
                        }
                    }
                    .build()

                mediaSession.setMetadata(metadata)
                val isPlaying = musicPlayer.isPlayingFlow.value
                val state = if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
                updatePlaybackState(state)
                buildAndShowNotification(song, isPlaying, artBitmap)
            }
        }
    }

    private fun getAlbumArtBitmap(path: String): Bitmap? {
        if (cachedLargeIconPath == path && cachedLargeIcon != null) {
            return cachedLargeIcon
        }
        val bitmap = AlbumArtCache.get(path) ?: loadAlbumArt(path)
        if (bitmap != null) {
            cachedLargeIcon = bitmap
            cachedLargeIconPath = path
        }
        return bitmap
    }

    private fun buildAndShowNotification(song: Song?, isPlaying: Boolean, artBitmap: Bitmap?) {
        val notification = buildNotification(song, isPlaying, artBitmap)
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(song: Song?, isPlaying: Boolean, artBitmap: Bitmap?): Notification {
        val title = song?.title?.ifEmpty { "Unknown Title" } ?: "No media playing"
        val artist = song?.artist?.ifEmpty { "Unknown Artist" } ?: "Select a track"

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(artist)
            .setSubText(song?.album?.takeIf { it.isNotEmpty() })
            .setSmallIcon(R.drawable.ic_music_note)
            .setContentIntent(openAppIntent())
            .setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        if (artBitmap != null) {
            builder.setLargeIcon(artBitmap)
        }

        // Previous Action
        builder.addAction(
            NotificationCompat.Action(
                R.drawable.ic_skip_previous, "Previous",
                MediaButtonReceiver.buildMediaButtonPendingIntent(
                    this,
                    PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                )
            )
        )

        // Play / Pause Action
        builder.addAction(
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

        // Next Action
        builder.addAction(
            NotificationCompat.Action(
                R.drawable.ic_skip_next, "Next",
                MediaButtonReceiver.buildMediaButtonPendingIntent(
                    this,
                    PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                )
            )
        )

        builder.setStyle(
            MediaStyle()
                .setMediaSession(mediaSession.sessionToken)
                .setShowActionsInCompactView(0, 1, 2)
        )

        return builder.build()
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
            ).apply {
                description = "Music playback controls and seek status"
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        MusicController.removeListener(controllerListener)
        serviceScope.cancel()
        instance = null
        viewModel = null
        mediaSession.release()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // If music is playing when user swipes away app, keep playing in foreground!
        if (!musicPlayer.isPlayingFlow.value) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }
}
