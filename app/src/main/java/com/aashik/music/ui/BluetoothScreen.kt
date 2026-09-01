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
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BluetoothScreen(
    @Suppress("UNUSED_PARAMETER") viewModel: MusicViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val phoneMedia by BluetoothMediaManager.phoneMedia.collectAsState()
    val notifications by AppNotificationManager.notificationHistory.collectAsState()
    val isListenerActive by AppNotificationManager.isListenerActive.collectAsState()

    LaunchedEffect(Unit) {
        AppNotificationManager.checkNotificationAccess(context)
        BluetoothMediaManager.init(context)
    }

    val cardShape = RoundedCornerShape(16.dp)
    val cardGrad = AppGradients.card(isActive = false)
    val borderBrush = AppGradients.border(isActive = false)
    val primaryButtonGrad = AppGradients.primaryButton()

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        val isNarrow = maxWidth < 600.dp

        if (isNarrow) {
            // ── Narrow: Stacked rows ─────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                BtMediaCard(
                    phoneMedia = phoneMedia,
                    cardGrad = cardGrad,
                    borderBrush = borderBrush,
                    primaryButtonGrad = primaryButtonGrad,
                    cardShape = cardShape,
                    modifier = Modifier.fillMaxWidth()
                )
                BtAlertsCard(
                    notifications = notifications,
                    isListenerActive = isListenerActive,
                    cardGrad = cardGrad,
                    borderBrush = borderBrush,
                    cardShape = cardShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                )
            }
        } else {
            // ── Wide: Side-by-side columns ───────────────────────────────────
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BtMediaCard(
                    phoneMedia = phoneMedia,
                    cardGrad = cardGrad,
                    borderBrush = borderBrush,
                    primaryButtonGrad = primaryButtonGrad,
                    cardShape = cardShape,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
                BtAlertsCard(
                    notifications = notifications,
                    isListenerActive = isListenerActive,
                    cardGrad = cardGrad,
                    borderBrush = borderBrush,
                    cardShape = cardShape,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BtMediaCard(
    phoneMedia: com.aashik.music.model.BluetoothPhoneMedia,
    cardGrad: Brush,
    borderBrush: Brush,
    primaryButtonGrad: Brush,
    cardShape: RoundedCornerShape,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(cardShape)
            .background(brush = cardGrad, shape = cardShape)
            .border(BorderStroke(1.dp, borderBrush), cardShape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Connection status pill
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(
                        if (phoneMedia.isConnected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
            )
            Text(
                text = if (phoneMedia.isConnected) phoneMedia.connectedDeviceName else "No Device Connected",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = if (phoneMedia.isConnected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = if (phoneMedia.isConnected) Icons.Rounded.BluetoothConnected else Icons.Rounded.Bluetooth,
                contentDescription = null,
                tint = if (phoneMedia.isConnected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(18.dp)
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

        // Album Art + Track info
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            val art = phoneMedia.albumArt
            if (art != null) {
                Image(
                    bitmap = art.asImageBitmap(),
                    contentDescription = "Album Art",
                    modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.BluetoothAudio,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (phoneMedia.isConnected) phoneMedia.title else "No Track",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (phoneMedia.isPlaying) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = if (phoneMedia.isPlaying) Modifier.basicMarquee() else Modifier
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (phoneMedia.isConnected) phoneMedia.artist else "Connect phone via Bluetooth",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (phoneMedia.appName.isNotBlank() && phoneMedia.isConnected) {
                    Text(
                        text = phoneMedia.appName,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        maxLines = 1
                    )
                }
            }
        }

        // Seekbar
        if (phoneMedia.durationMs > 0) {
            CustomHorizontalSeekBar(
                progress = (phoneMedia.positionMs.toFloat() / phoneMedia.durationMs).coerceIn(0f, 1f),
                onProgressChanged = {},
                isPlaying = phoneMedia.isPlaying,
                currentPositionMs = phoneMedia.positionMs,
                durationMs = phoneMedia.durationMs,
                showTimeLabels = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Transport controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { BluetoothMediaManager.skipToPrevious() },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Rounded.SkipPrevious, null, modifier = Modifier.size(30.dp),
                    tint = MaterialTheme.colorScheme.onSurface)
            }

            Spacer(modifier = Modifier.width(16.dp))

            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(brush = primaryButtonGrad, shape = CircleShape)
                    .clickable { BluetoothMediaManager.togglePlayPause() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (phoneMedia.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            IconButton(
                onClick = { BluetoothMediaManager.skipToNext() },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Rounded.SkipNext, null, modifier = Modifier.size(30.dp),
                    tint = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun BtAlertsCard(
    notifications: List<AppNotification>,
    isListenerActive: Boolean,
    cardGrad: Brush,
    borderBrush: Brush,
    cardShape: RoundedCornerShape,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .clip(cardShape)
            .background(brush = cardGrad, shape = cardShape)
            .border(BorderStroke(1.dp, borderBrush), cardShape)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        // Header row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.NotificationsActive,
                    contentDescription = null,
                    tint = if (notifications.isNotEmpty()) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Alerts",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (notifications.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${notifications.size}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (notifications.isNotEmpty()) {
                    Text(
                        text = "Clear",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { AppNotificationManager.clearHistory() }
                            .padding(4.dp)
                    )
                }
                if (!isListenerActive) {
                    Text(
                        text = "Enable",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable {
                                try { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
                                catch (_: Exception) {}
                            }
                            .padding(4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (notifications.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.NotificationsNone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                        modifier = Modifier.size(40.dp)
                    )
                    Text(
                        text = if (isListenerActive) "No notifications yet" else "Notification access required",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    FilledTonalButton(
                        onClick = {
                            if (!isListenerActive) {
                                try { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
                                catch (_: Exception) {}
                            } else {
                                AppNotificationManager.sendTestNotification("WhatsApp", "Alex", "On my way! 🚗🎵")
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(if (isListenerActive) "Send Test" else "Grant Access", fontSize = 12.sp)
                    }
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                items(items = notifications, key = { it.id }) { item ->
                    NotificationHistoryItem(item)
                }
            }
        }
    }
}

@Composable
private fun NotificationHistoryItem(notification: AppNotification) {
    val iconInfo = getNotificationCategoryIcon(notification.packageName, notification.category)
    val shape = RoundedCornerShape(10.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(iconInfo.badgeColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = iconInfo.icon,
                contentDescription = null,
                tint = iconInfo.badgeColor,
                modifier = Modifier.size(18.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = notification.appName,
                    fontSize = 10.sp,
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
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (notification.text.isNotBlank()) {
                Text(
                    text = notification.text,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun formatTimeAgo(timeMs: Long): String {
    val formatter = SimpleDateFormat("h:mm a", Locale.getDefault())
    return formatter.format(Date(timeMs))
}
