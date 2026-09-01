package com.aashik.music.notification

import android.content.Context
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import com.aashik.music.model.AppNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

object AppNotificationManager {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var autoDismissJob: Job? = null

    private val _activeNotification = MutableStateFlow<AppNotification?>(null)
    val activeNotification: StateFlow<AppNotification?> = _activeNotification.asStateFlow()

    private val _notificationHistory = MutableStateFlow<List<AppNotification>>(emptyList())
    val notificationHistory: StateFlow<List<AppNotification>> = _notificationHistory.asStateFlow()

    private val _isBluetoothConnected = MutableStateFlow(false)
    val isBluetoothConnected: StateFlow<Boolean> = _isBluetoothConnected.asStateFlow()

    private val _isListenerActive = MutableStateFlow(false)
    val isListenerActive: StateFlow<Boolean> = _isListenerActive.asStateFlow()

    fun setBluetoothConnected(connected: Boolean) {
        _isBluetoothConnected.value = connected
    }

    fun setListenerActive(active: Boolean) {
        _isListenerActive.value = active
    }

    fun checkNotificationAccess(context: Context): Boolean {
        val enabledPackages = NotificationManagerCompat.getEnabledListenerPackages(context)
        val hasAccess = enabledPackages.contains(context.packageName)
        _isListenerActive.value = hasAccess
        return hasAccess
    }

    fun postNotification(notification: AppNotification) {
        // Add to history (max 30 items)
        val currentList = _notificationHistory.value.toMutableList()
        currentList.removeAll { it.id == notification.id }
        currentList.add(0, notification)
        if (currentList.size > 30) {
            _notificationHistory.value = currentList.subList(0, 30)
        } else {
            _notificationHistory.value = currentList
        }

        // Show as active heads-up notification
        _activeNotification.value = notification

        // Auto-dismiss after 6.5 seconds
        autoDismissJob?.cancel()
        autoDismissJob = scope.launch {
            delay(6500L)
            if (_activeNotification.value?.id == notification.id) {
                _activeNotification.value = null
            }
        }
    }

    fun dismissActive() {
        autoDismissJob?.cancel()
        _activeNotification.value = null
    }

    fun clearHistory() {
        _notificationHistory.value = emptyList()
        dismissActive()
    }

    fun sendTestNotification(appName: String = "WhatsApp", sender: String = "John Doe", message: String = "Hey! Let's hit the road and play some music 🚗🎵") {
        val testNotification = AppNotification(
            id = UUID.randomUUID().toString(),
            packageName = "com.whatsapp",
            appName = appName,
            title = sender,
            text = message,
            timestamp = System.currentTimeMillis(),
            isFromBluetooth = _isBluetoothConnected.value,
            category = "msg"
        )
        postNotification(testNotification)
    }
}
