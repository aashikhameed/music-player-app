package com.aashik.music.nativeaudio

import android.os.Handler
import android.os.Looper
import android.util.Log

/**
 * Kotlin JNI Bridge to C++ OpenSL ES Audio Engine.
 * Thread-safe interface for low-latency automotive playback on Blaupunkt Santa Rosa 985 (API 28).
 */
class NativeAudioBridge private constructor() {

    companion object {
        private const val TAG = "NativeAudioBridge"

        init {
            try {
                System.loadLibrary("audio-player")
                Log.i(TAG, "Successfully loaded native audio library libaudio-player.so")
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "Failed to load libaudio-player.so: ${e.message}")
            }
        }

        val instance: NativeAudioBridge by lazy {
            NativeAudioBridge().apply {
                init()
            }
        }
    }

    interface PlaybackListener {
        fun onPlaybackStateChanged(state: Int)
        fun onPositionChanged(positionMs: Long)
        fun onDurationUpdated(durationMs: Long)
        fun onTrackEnded()
        fun onError(errorCode: Int, message: String)
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private var listener: PlaybackListener? = null

    fun setListener(listener: PlaybackListener?) {
        this.listener = listener
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Native external declarations
    // ─────────────────────────────────────────────────────────────────────────

    external fun init(): Int
    external fun shutdown()
    external fun playTrack(filepath: String, positionMs: Long = 0L): Int
    external fun pause()
    external fun resume()
    external fun stop()
    external fun seek(positionMs: Long): Int
    external fun getCurrentPosition(): Long
    external fun getDuration(): Long
    external fun setVolume(volume: Float)
    external fun getPlaybackState(): Int
    external fun setShuffleMode(enabled: Boolean)
    external fun setRepeatMode(mode: Int)

    // ─────────────────────────────────────────────────────────────────────────
    // Callbacks from C++ audio thread (must be non-blocking and post to main thread)
    // ─────────────────────────────────────────────────────────────────────────

    @Suppress("unused")
    fun onPlaybackStateChanged(state: Int) {
        mainHandler.post {
            listener?.onPlaybackStateChanged(state)
        }
    }

    @Suppress("unused")
    fun onPositionChanged(positionMs: Long) {
        mainHandler.post {
            listener?.onPositionChanged(positionMs)
        }
    }

    @Suppress("unused")
    fun onDurationUpdated(durationMs: Long) {
        mainHandler.post {
            listener?.onDurationUpdated(durationMs)
        }
    }

    @Suppress("unused")
    fun onTrackEnded() {
        mainHandler.post {
            listener?.onTrackEnded()
        }
    }

    @Suppress("unused")
    fun onError(errorCode: Int, message: String) {
        Log.e(TAG, "Native audio error [$errorCode]: $message")
        mainHandler.post {
            listener?.onError(errorCode, message)
        }
    }
}
