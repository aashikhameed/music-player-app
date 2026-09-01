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
        const val ACTION_TEST_NOTIFICATION = "com.aashik.music.ACTION_TEST_NOTIFICATION"
    }

    @Suppress("DEPRECATION")
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_TEST_NOTIFICATION -> {
                val app = intent.getStringExtra("app") ?: "WhatsApp"
                val title = intent.getStringExtra("title") ?: "John Doe"
                val text = intent.getStringExtra("text") ?: "Hey! Let's hit the road and play some music 🚗🎵"
                com.aashik.music.notification.AppNotificationManager.sendTestNotification(app, title, text)
            }

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

                val deviceName = device?.name ?: "Connected Phone"
                Log.d(TAG, "Connected to $deviceName")
                com.aashik.music.notification.AppNotificationManager.setBluetoothConnected(true)
                com.aashik.music.bluetooth.BluetoothMediaManager.setConnectedPhone(deviceName, true)

                // Trigger playback on Bluetooth connection
                val serviceIntent = Intent(context, NotificationPlaybackService::class.java).apply {
                    action = ACTION_BLUETOOTH_AUTOPLAY
                }
                NotificationPlaybackService.startService(context, serviceIntent)
            }

            BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                val deviceName = device?.name ?: "Phone Disconnected"
                Log.d(TAG, "Disconnected from $deviceName")
                com.aashik.music.notification.AppNotificationManager.setBluetoothConnected(false)
                com.aashik.music.bluetooth.BluetoothMediaManager.setConnectedPhone(deviceName, false)

                // Pause playback when Bluetooth disconnects
                val serviceIntent = Intent(context, NotificationPlaybackService::class.java).apply {
                    action = ACTION_BLUETOOTH_PAUSE
                }
                NotificationPlaybackService.startService(context, serviceIntent)
            }
        }
    }
}
