package com.aashik.music.model

import android.graphics.Bitmap

/**
 * Data model for audio tracks streaming from a connected Bluetooth phone.
 */
data class BluetoothPhoneMedia(
    val title: String = "No Bluetooth Track",
    val artist: String = "Connect phone to stream audio",
    val album: String = "",
    val appName: String = "Bluetooth Audio",
    val isPlaying: Boolean = false,
    val durationMs: Long = 0L,
    val positionMs: Long = 0L,
    val albumArt: Bitmap? = null,
    val connectedDeviceName: String = "Phone Disconnected",
    val isConnected: Boolean = false
)
