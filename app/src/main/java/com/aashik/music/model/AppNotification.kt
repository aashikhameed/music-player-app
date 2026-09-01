package com.aashik.music.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Message
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

@Immutable
data class AppNotification(
    val id: String,
    val packageName: String,
    val appName: String,
    val title: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isFromBluetooth: Boolean = false,
    val category: String? = null
)

data class NotificationIconInfo(
    val icon: ImageVector,
    val badgeColor: Color
)

fun getNotificationCategoryIcon(packageName: String, category: String?): NotificationIconInfo {
    val pkg = packageName.lowercase()
    return when {
        pkg.contains("whatsapp") -> NotificationIconInfo(Icons.AutoMirrored.Rounded.Message, Color(0xFF25D366))
        pkg.contains("telegram") -> NotificationIconInfo(Icons.AutoMirrored.Rounded.Message, Color(0xFF2AABEE))
        pkg.contains("message") || pkg.contains("sms") || category == "msg" -> NotificationIconInfo(Icons.AutoMirrored.Rounded.Message, Color(0xFF00C853))
        pkg.contains("dialer") || pkg.contains("call") || pkg.contains("phone") || category == "call" -> NotificationIconInfo(Icons.Rounded.Call, Color(0xFF2196F3))
        pkg.contains("mail") || pkg.contains("gmail") || category == "email" -> NotificationIconInfo(Icons.Rounded.Email, Color(0xFFEA4335))
        pkg.contains("maps") || pkg.contains("navigation") || category == "navigation" -> NotificationIconInfo(Icons.Rounded.Navigation, Color(0xFFFF9800))
        else -> NotificationIconInfo(Icons.Rounded.Notifications, Color(0xFF00E5FF))
    }
}
