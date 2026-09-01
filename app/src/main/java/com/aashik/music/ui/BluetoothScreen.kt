package com.aashik.music.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BluetoothAudio
import androidx.compose.material.icons.rounded.BluetoothConnected
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.NotificationAdd
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aashik.music.bluetooth.BluetoothMediaManager
import com.aashik.music.model.AppNotification
import com.aashik.music.model.getNotificationCategoryIcon
import com.aashik.music.notification.AppNotificationManager
import com.aashik.music.theme.AppGradients
import com.aashik.music.viewmodel.MusicViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Dedicated Bluetooth Audio & Phone Sync Hub.
 *
 * Distinct from the local music library:
 * - Phone Audio Panel: Displays active audio stream & track metadata from connected mobile phone (Spotify, Apple Music, YouTube Music).
 * - Phone Alerts Panel: Real-time Phone Alerts & Notification mirroring.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BluetoothScreen(
    @Suppress("UNUSED_PARAMETER") viewModel: MusicViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val phoneMedia by BluetoothMediaManager.phoneMedia.collectAsState()
    val notifications by AppNotificationManager.notificationHistory.collectAsState()
    val isBtConnected by AppNotificationManager.isBluetoothConnected.collectAsState()
    val isListenerActive by AppNotificationManager.isListenerActive.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var compactTab by remember { mutableIntStateOf(0) } // For split-screen / narrow widths

    LaunchedEffect(Unit) {
        AppNotificationManager.checkNotificationAccess(context)
        BluetoothMediaManager.init(context)
    }

    val cardShape = RoundedCornerShape(16.dp)
    val cardGrad = AppGradients.capsule(isActive = false)
    val borderBrush = AppGradients.border(isActive = false)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .padding(10.dp)
    ) {
        val isNarrow = maxWidth < 620.dp

        if (isNarrow) {
            // =========================================================================
            // COMPACT / SPLIT SCREEN (35% Side Panel or Mobile Portrait)
            // =========================================================================
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(cardShape)
                    .background(brush = cardGrad, shape = cardShape)
                    .border(border = BorderStroke(1.dp, borderBrush), shape = cardShape)
                    .padding(10.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Segmented Tabs for Compact Mode
                    TabRow(
                        selectedTabIndex = compactTab,
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Tab(
                            selected = compactTab == 0,
                            onClick = { compactTab = 0 },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Rounded.BluetoothAudio, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Text("Audio", fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                                }
                            }
                        )
                        Tab(
                            selected = compactTab == 1,
                            onClick = { compactTab = 1 },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Rounded.Notifications, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Text("Alerts (${notifications.size})", fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                                }
                            }
                        )
                        Tab(
                            selected = compactTab == 2,
                            onClick = { compactTab = 2 },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Rounded.Info, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Text("Info", fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                                }
                            }
                        )
                    }

                    when (compactTab) {
                        0 -> {
                            PhoneMediaStreamPlayer(
                                phoneMedia = phoneMedia,
                                isBtConnected = isBtConnected,
                                onOpenSettings = {
                                    try {
                                        context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                                    } catch (_: Exception) {
                                        context.startActivity(Intent(Settings.ACTION_SETTINGS))
                                    }
                                },
                                onTestPhoneSync = {
                                    BluetoothMediaManager.updatePhoneMediaManually(
                                        title = "Starboy (Feat. Daft Punk)",
                                        artist = "The Weeknd",
                                        album = "Starboy",
                                        appName = "Spotify",
                                        isPlaying = true,
                                        deviceName = "Aashik's iPhone"
                                    )
                                    AppNotificationManager.sendTestNotification(
                                        appName = "Spotify",
                                        sender = "Now Playing on iPhone",
                                        message = "The Weeknd — Starboy (Feat. Daft Punk) 🎵"
                                    )
                                }
                            )
                        }
                        1 -> {
                            AlertsFeedContent(
                                notifications = notifications,
                                onSendTest = {
                                    AppNotificationManager.sendTestNotification(
                                        appName = "WhatsApp",
                                        sender = "Alex Johnson",
                                        message = "On my way! Turn up the playlist 🎵🚗"
                                    )
                                },
                                onClearAll = { AppNotificationManager.clearHistory() }
                            )
                        }
                        else -> {
                            PhoneInfoContent(
                                isListenerActive = isListenerActive,
                                onGrantPermission = {
                                    try {
                                        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                                    } catch (_: Exception) {
                                        context.startActivity(Intent(Settings.ACTION_SETTINGS))
                                    }
                                }
                            )
                        }
                    }
                }
            }
        } else {
            // =========================================================================
            // FULL SCREEN (2-Panel Landscape Infotainment Layout)
            // =========================================================================
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Left Panel: Bluetooth Audio & Phone Streaming
                Box(
                    modifier = Modifier
                        .weight(1.1f)
                        .fillMaxHeight()
                        .clip(cardShape)
                        .background(brush = cardGrad, shape = cardShape)
                        .border(border = BorderStroke(1.dp, borderBrush), shape = cardShape)
                        .padding(14.dp)
                ) {
                    PhoneMediaStreamPlayer(
                        phoneMedia = phoneMedia,
                        isBtConnected = isBtConnected,
                        onOpenSettings = {
                            try {
                                context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                            } catch (_: Exception) {
                                context.startActivity(Intent(Settings.ACTION_SETTINGS))
                            }
                        },
                        onTestPhoneSync = {
                            BluetoothMediaManager.updatePhoneMediaManually(
                                title = "Starboy (Feat. Daft Punk)",
                                artist = "The Weeknd",
                                album = "Starboy",
                                appName = "Spotify",
                                isPlaying = true,
                                deviceName = "Aashik's iPhone"
                            )
                            AppNotificationManager.sendTestNotification(
                                appName = "Spotify",
                                sender = "Now Playing on iPhone",
                                message = "The Weeknd — Starboy (Feat. Daft Punk) 🎵"
                            )
                        }
                    )
                }

                // Right Panel: Alerts & Phone Notifications
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(cardShape)
                        .background(brush = cardGrad, shape = cardShape)
                        .border(border = BorderStroke(1.dp, borderBrush), shape = cardShape)
                        .padding(12.dp)
                ) {
                    PhoneAlertsPanel(
                        notifications = notifications,
                        isListenerActive = isListenerActive,
                        selectedTab = selectedTab,
                        onTabSelected = { selectedTab = it },
                        onGrantPermission = {
                            try {
                                context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                            } catch (_: Exception) {
                                context.startActivity(Intent(Settings.ACTION_SETTINGS))
                            }
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhoneMediaStreamPlayer(
    phoneMedia: com.aashik.music.model.BluetoothPhoneMedia,
    isBtConnected: Boolean,
    onOpenSettings: () -> Unit,
    onTestPhoneSync: () -> Unit
) {
    val primaryButtonGrad = AppGradients.primaryButton()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Header: Phone Connection Status Pill
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(if (phoneMedia.isConnected || isBtConnected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (phoneMedia.isConnected || isBtConnected) Icons.Rounded.BluetoothConnected else Icons.Rounded.Bluetooth,
                        contentDescription = null,
                        tint = if (phoneMedia.isConnected || isBtConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Text(
                        text = phoneMedia.connectedDeviceName,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Text(
                        text = if (phoneMedia.isPlaying) "STREAMING AUDIO" else if (phoneMedia.isConnected || isBtConnected) "CONNECTED VIA BLUETOOTH" else "READY TO PAIR",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        ),
                        color = if (phoneMedia.isPlaying) MaterialTheme.colorScheme.secondary else if (phoneMedia.isConnected || isBtConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Source app badge (e.g. Spotify / Apple Music / Bluetooth)
            val appBadgeShape = RoundedCornerShape(8.dp)
            Box(
                modifier = Modifier
                    .clip(appBadgeShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = phoneMedia.appName,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Hero Bluetooth Now Playing Track Card (From Mobile Phone)
        val heroCardShape = RoundedCornerShape(14.dp)
        val heroGrad = AppGradients.card(isActive = phoneMedia.isPlaying)
        val heroBorder = AppGradients.border(isActive = phoneMedia.isPlaying)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(heroCardShape)
                .background(brush = heroGrad, shape = heroCardShape)
                .border(border = BorderStroke(1.dp, heroBorder), shape = heroCardShape)
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Album Art or App Icon
                    val art = phoneMedia.albumArt
                    if (art != null) {
                        Image(
                            bitmap = art.asImageBitmap(),
                            contentDescription = "Album Art",
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surface),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.BluetoothAudio,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }

                    // Song Title & Artist from Phone
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = phoneMedia.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (phoneMedia.isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = if (phoneMedia.isPlaying) Modifier.basicMarquee() else Modifier
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = phoneMedia.artist,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (phoneMedia.album.isNotBlank()) {
                            Text(
                                text = phoneMedia.album,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Squiggly Wave Seekbar / Audio Visualizer for Bluetooth Audio
                CustomHorizontalSeekBar(
                    progress = if (phoneMedia.durationMs > 0) (phoneMedia.positionMs.toFloat() / phoneMedia.durationMs).coerceIn(0f, 1f) else 0.35f,
                    onProgressChanged = {},
                    isPlaying = phoneMedia.isPlaying,
                    currentPositionMs = phoneMedia.positionMs,
                    durationMs = if (phoneMedia.durationMs > 0) phoneMedia.durationMs else 214000L,
                    showTimeLabels = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Bluetooth AVRCP Playback Transport Controls - Large Driver Friendly
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Previous Track
                    IconButton(
                        onClick = { BluetoothMediaManager.skipToPrevious() },
                        modifier = Modifier.size(46.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SkipPrevious,
                            contentDescription = "Previous Track",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(18.dp))

                    // Hero Play/Pause Circle Button
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(brush = primaryButtonGrad, shape = CircleShape)
                            .clickable { BluetoothMediaManager.togglePlayPause() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (phoneMedia.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (phoneMedia.isPlaying) "Pause" else "Play",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(18.dp))

                    // Next Track
                    IconButton(
                        onClick = { BluetoothMediaManager.skipToNext() },
                        modifier = Modifier.size(46.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SkipNext,
                            contentDescription = "Next Track",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }

        // Quick Action & Bluetooth Tools
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onOpenSettings,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("BT Settings", fontSize = 12.sp)
            }

            FilledTonalButton(
                onClick = onTestPhoneSync,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Rounded.PhoneAndroid,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Test Phone Sync", fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun PhoneAlertsPanel(
    notifications: List<AppNotification>,
    isListenerActive: Boolean,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onGrantPermission: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Header Tabs: Alerts / Phone Info
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { onTabSelected(0) },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Rounded.Notifications, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text("Alerts (${notifications.size})", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { onTabSelected(1) },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Rounded.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text("Phone Info", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                }
            )
        }

        if (selectedTab == 0) {
            AlertsFeedContent(
                notifications = notifications,
                onSendTest = {
                    AppNotificationManager.sendTestNotification(
                        appName = "WhatsApp",
                        sender = "Alex Johnson",
                        message = "On my way! Turn up the playlist 🎵🚗"
                    )
                },
                onClearAll = { AppNotificationManager.clearHistory() }
            )
        } else {
            PhoneInfoContent(
                isListenerActive = isListenerActive,
                onGrantPermission = onGrantPermission
            )
        }
    }
}

@Composable
private fun AlertsFeedContent(
    notifications: List<AppNotification>,
    onSendTest: () -> Unit,
    onClearAll: () -> Unit
) {
    if (notifications.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Notifications,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(44.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "No Phone Alerts Yet",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Incoming notifications from WhatsApp, SMS, Calls & Apps on your mobile phone will appear here in real-time.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(14.dp))
                FilledTonalButton(
                    onClick = onSendTest,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.NotificationsActive,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Send Test Alert", fontSize = 12.sp)
                }
            }
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalButton(
                onClick = onSendTest,
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.height(30.dp)
            ) {
                Text("+ Test Alert", fontSize = 11.sp)
            }

            Text(
                text = "Clear All",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { onClearAll() }
                    .padding(4.dp)
            )
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 8.dp)
        ) {
            items(items = notifications, key = { it.id }) { item ->
                NotificationHistoryItem(item)
            }
        }
    }
}

@Composable
private fun PhoneInfoContent(
    isListenerActive: Boolean,
    onGrantPermission: () -> Unit
) {
    val cardGrad = AppGradients.capsule(isActive = false)
    val borderBrush = AppGradients.border(isActive = false)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Notification Listener Access Status Card
        val notifCardShape = RoundedCornerShape(12.dp)
        val notifCardGrad = AppGradients.capsule(isActive = isListenerActive)
        val notifCardBorder = AppGradients.border(isActive = isListenerActive)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(notifCardShape)
                .background(brush = notifCardGrad, shape = notifCardShape)
                .border(border = BorderStroke(1.dp, notifCardBorder), shape = notifCardShape)
                .clickable {
                    if (!isListenerActive) {
                        onGrantPermission()
                    }
                }
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = if (isListenerActive) Icons.Rounded.NotificationsActive else Icons.Rounded.NotificationAdd,
                    contentDescription = null,
                    tint = if (isListenerActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(28.dp)
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isListenerActive) "Notification Mirroring Active" else "Enable Notification Listener",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isListenerActive)
                            "Capturing phone alerts in real-time"
                        else
                            "Tap to grant Android notification listener permission",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Bluetooth Audio Protocol Details Card
        val infoCardShape = RoundedCornerShape(12.dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(infoCardShape)
                .background(brush = cardGrad, shape = infoCardShape)
                .border(border = BorderStroke(1.dp, borderBrush), shape = infoCardShape)
                .padding(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Supported Bluetooth Protocols",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "• A2DP Audio Sink: High-fidelity stereo streaming\n• AVRCP 1.6: Remote track title, artist & transport commands\n• Phone Notification Mirroring: Real-time alerts HUD",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun NotificationHistoryItem(notification: AppNotification) {
    val itemShape = RoundedCornerShape(12.dp)
    val itemGrad = AppGradients.capsule(isActive = false)
    val itemBorder = AppGradients.border(isActive = false)
    val iconInfo = getNotificationCategoryIcon(notification.packageName, notification.category)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(itemShape)
            .background(brush = itemGrad, shape = itemShape)
            .border(border = BorderStroke(1.dp, itemBorder), shape = itemShape)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(iconInfo.badgeColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconInfo.icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = notification.appName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = iconInfo.badgeColor
                    )
                    Text(
                        text = formatTimeAgo(notification.timestamp),
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = notification.title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (notification.text.isNotBlank()) {
                    Text(
                        text = notification.text,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

private fun formatTimeAgo(timeMs: Long): String {
    val formatter = SimpleDateFormat("h:mm a", Locale.getDefault())
    return formatter.format(Date(timeMs))
}
