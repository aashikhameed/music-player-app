package com.aashik.music.bluetooth

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.aashik.music.model.BluetoothPhoneMedia
import com.aashik.music.service.AppNotificationListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Manages Bluetooth Phone Audio Streaming (A2DP / AVRCP / External Media Sessions).
 * Captures real-time metadata (Song title, Artist, Album, Playback State) from connected mobile phones.
 */
object BluetoothMediaManager {
    private const val TAG = "BluetoothMediaManager"
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _phoneMedia = MutableStateFlow(
        BluetoothPhoneMedia(
            title = "No Bluetooth Track",
            artist = "Connect phone to stream audio",
            album = "",
            appName = "Bluetooth Audio",
            isPlaying = false,
            connectedDeviceName = "Phone Disconnected",
            isConnected = false
        )
    )
    val phoneMedia: StateFlow<BluetoothPhoneMedia> = _phoneMedia.asStateFlow()

    private var activeController: MediaController? = null
    private var mediaSessionManager: MediaSessionManager? = null
    private var isListeningToSessions = false

    private val controllerCallback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) {
            super.onMetadataChanged(metadata)
            updateMetadata(metadata)
        }

        override fun onPlaybackStateChanged(state: PlaybackState?) {
            super.onPlaybackStateChanged(state)
            updatePlaybackState(state)
        }

        override fun onSessionDestroyed() {
            super.onSessionDestroyed()
            Log.d(TAG, "Media session destroyed")
            activeController = null
        }
    }

    private val sessionsChangedListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        Log.d(TAG, "Active sessions changed: count = ${controllers?.size}")
        findAndAttachExternalController(controllers)
    }

    fun init(context: Context) {
        try {
            mediaSessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
            val componentName = ComponentName(context, AppNotificationListenerService::class.java)

            if (!isListeningToSessions) {
                try {
                    mediaSessionManager?.addOnActiveSessionsChangedListener(
                        sessionsChangedListener,
                        componentName,
                        mainHandler
                    )
                    isListeningToSessions = true
                    val initialControllers = mediaSessionManager?.getActiveSessions(componentName)
                    findAndAttachExternalController(initialControllers)
                } catch (e: SecurityException) {
                    Log.w(TAG, "Notification listener permission not yet granted: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing MediaSessionManager", e)
        }
    }

    private fun findAndAttachExternalController(controllers: List<MediaController>?) {
        if (controllers.isNullOrEmpty()) {
            return
        }

        // Pick the first non-internal active media session (e.g. Spotify, YouTube Music, Bluetooth A2DP)
        val external = controllers.firstOrNull { it.packageName != "com.aashik.music" } ?: controllers.firstOrNull()
        if (external != null && external != activeController) {
            activeController?.unregisterCallback(controllerCallback)
            activeController = external
            activeController?.registerCallback(controllerCallback, mainHandler)

            updateMetadata(activeController?.metadata)
            updatePlaybackState(activeController?.playbackState)
            Log.d(TAG, "Attached to external media session from: ${external.packageName}")
        }
    }

    private fun updateMetadata(metadata: MediaMetadata?) {
        if (metadata == null) return

        val title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: metadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
            ?: "Unknown Title"
        val artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: metadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: "Unknown Artist"
        val album = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM) ?: ""
        val duration = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION).coerceAtLeast(0L)
        val artBitmap: Bitmap? = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_ART)

        val appPackage = activeController?.packageName ?: ""
        val appName = when {
            appPackage.contains("spotify") -> "Spotify"
            appPackage.contains("youtube") -> "YouTube Music"
            appPackage.contains("apple") -> "Apple Music"
            appPackage.contains("amazon") -> "Amazon Music"
            appPackage.contains("bluetooth") -> "Bluetooth A2DP"
            else -> "Bluetooth Phone"
        }

        _phoneMedia.value = _phoneMedia.value.copy(
            title = title,
            artist = artist,
            album = album,
            appName = appName,
            durationMs = duration,
            albumArt = artBitmap,
            isConnected = true,
            connectedDeviceName = if (_phoneMedia.value.connectedDeviceName == "Phone Disconnected") "Connected Phone" else _phoneMedia.value.connectedDeviceName
        )
    }

    private fun updatePlaybackState(state: PlaybackState?) {
        if (state == null) return
        val isPlaying = state.state == PlaybackState.STATE_PLAYING
        val position = state.position

        _phoneMedia.value = _phoneMedia.value.copy(
            isPlaying = isPlaying,
            positionMs = position,
            isConnected = true
        )
    }

    fun setConnectedPhone(deviceName: String, isConnected: Boolean) {
        _phoneMedia.value = _phoneMedia.value.copy(
            connectedDeviceName = deviceName,
            isConnected = isConnected
        )
    }

    fun updatePhoneMediaManually(
        title: String,
        artist: String,
        album: String = "",
        appName: String = "Spotify",
        isPlaying: Boolean = true,
        deviceName: String = "iPhone 15 Pro",
        durationMs: Long = 214000L
    ) {
        _phoneMedia.value = BluetoothPhoneMedia(
            title = title,
            artist = artist,
            album = album,
            appName = appName,
            isPlaying = isPlaying,
            durationMs = durationMs,
            positionMs = 38000L,
            albumArt = null,
            connectedDeviceName = deviceName,
            isConnected = true
        )
    }

    // AVRCP Remote Controls sent to phone media player
    fun togglePlayPause() {
        activeController?.let { controller ->
            val isPlaying = controller.playbackState?.state == PlaybackState.STATE_PLAYING
            if (isPlaying) {
                controller.transportControls.pause()
            } else {
                controller.transportControls.play()
            }
        } ?: run {
            // Local state toggle if no external session attached
            _phoneMedia.value = _phoneMedia.value.copy(isPlaying = !_phoneMedia.value.isPlaying)
        }
    }

    fun skipToNext() {
        activeController?.transportControls?.skipToNext() ?: run {
            // Demo track advance
            _phoneMedia.value = _phoneMedia.value.copy(
                title = "Blinding Lights",
                artist = "The Weeknd",
                album = "After Hours",
                appName = "Spotify",
                isPlaying = true
            )
        }
    }

    fun skipToPrevious() {
        activeController?.transportControls?.skipToPrevious() ?: run {
            _phoneMedia.value = _phoneMedia.value.copy(
                title = "Starboy",
                artist = "The Weeknd ft. Daft Punk",
                album = "Starboy",
                appName = "Spotify",
                isPlaying = true
            )
        }
    }
}
