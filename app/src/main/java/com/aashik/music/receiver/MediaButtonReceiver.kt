package com.aashik.music.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.KeyEvent

class MediaButtonReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_MEDIA_BUTTON) {
            val keyEvent = intent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT)
            if (keyEvent?.action == KeyEvent.ACTION_DOWN) {
                when (keyEvent.keyCode) {
                    KeyEvent.KEYCODE_MEDIA_PLAY -> {
                        Log.d("MediaButtonReceiver", "Play button pressed")
                        // Trigger your play method
                    }
                    KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                        Log.d("MediaButtonReceiver", "Pause button pressed")
                        // Trigger your pause method
                    }
                    KeyEvent.KEYCODE_MEDIA_NEXT -> {
                        Log.d("MediaButtonReceiver", "Next button pressed")
                        // Trigger your skipNext method
                    }
                    KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                        Log.d("MediaButtonReceiver", "Previous button pressed")
                        // Trigger your skipPrevious method
                    }
                }
            }
        }
    }
}
