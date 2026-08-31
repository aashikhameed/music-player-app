package com.aashik.music.receiver

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import com.aashik.music.service.NotificationPlaybackService

class BluetoothReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BluetoothReceiver"
        const val ACTION_BLUETOOTH_AUTOPLAY = "com.aashik.music.ACTION_BLUETOOTH_AUTOPLAY"
        const val ACTION_BLUETOOTH_PAUSE = "com.aashik.music.ACTION_BLUETOOTH_PAUSE"
    }

    @Suppress("DEPRECATION")
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            BluetoothDevice.ACTION_ACL_CONNECTED -> {
                val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    Log.w(TAG, "Missing BLUETOOTH_CONNECT permission")
                    return
                }

                Log.d(TAG, "Connected to ${device?.name}")

                // Trigger playback on Bluetooth connection
                val serviceIntent = Intent(context, NotificationPlaybackService::class.java).apply {
                    action = ACTION_BLUETOOTH_AUTOPLAY
                }
                NotificationPlaybackService.startService(context, serviceIntent)
            }

            BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                Log.d(TAG, "Disconnected from ${device?.name}")

                // Pause playback when Bluetooth disconnects
                val serviceIntent = Intent(context, NotificationPlaybackService::class.java).apply {
                    action = ACTION_BLUETOOTH_PAUSE
                }
                NotificationPlaybackService.startService(context, serviceIntent)
            }
        }
    }
}
