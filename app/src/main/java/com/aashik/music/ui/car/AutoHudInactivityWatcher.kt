package com.aashik.music.ui.car

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.rounded.*
import com.aashik.music.ui.designsystem.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aashik.music.model.*
import com.aashik.music.ui.*
import com.aashik.music.ui.components.*
import com.aashik.music.ui.mobile.*
import com.aashik.music.ui.car.*
import com.aashik.music.viewmodel.MusicViewModel
import kotlinx.coroutines.isActive

private val PortraitSongCardShape = RoundedCornerShape(16.dp)
private val LandscapeFullSongShape = RoundedCornerShape(16.dp)
private val HeaderCapsuleShape = RoundedCornerShape(12.dp)
private val PillShape10 = RoundedCornerShape(8.dp)
private val StatusCapsuleShape = RoundedCornerShape(12.dp)
private val RadioCardShape = RoundedCornerShape(16.dp)
private val GenreChipShape = RoundedCornerShape(8.dp)
private val LiveBadgeShape = RoundedCornerShape(4.dp)
private val MoodCardShape = RoundedCornerShape(16.dp)
private val MoodStripeShape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
private val BackPillShape = RoundedCornerShape(8.dp)
private val MobileMiniPlayerShape = RoundedCornerShape(16.dp)
private val MiniCodecBadgeShape = RoundedCornerShape(4.dp)
private val MobileNavBarShape = RoundedCornerShape(28.dp)
private val MobileActiveTabShape = RoundedCornerShape(16.dp)
private val ShelfArtworkShape = RoundedCornerShape(12.dp)
private val ShelfCardContainerShape = RoundedCornerShape(12.dp)
private val CircleBackButtonShape = CircleShape
private val CircleAvatarShape = CircleShape
private val MoreCardShape = RoundedCornerShape(12.dp)
private val MoreIconContainerShape = RoundedCornerShape(8.dp)

private val ThemeSunColor = Color(0xFFFFD54F)
private val ThemeMoonColor = Color(0xFF7986CB)
private val ClockPlayingGreen = Color(0xFF00E676)
private val ClockDarkGray = Color(0xFF455A64)
private val ClockLightGray = Color(0xFFB0BEC5)
private val FavoriteHeartRed = Color(0xFFFF5252)

/**
 * Isolated background inactivity watcher for Auto HUD mode.
 * Collects high-frequency/state triggers without triggering recompositions of the main music library.
 */
@Composable
fun AutoHudInactivityWatcher(
    viewModel: MusicViewModel,
    userInteractionChannel: kotlinx.coroutines.channels.Channel<Unit>
) {
    val autoHudEnabled by viewModel.autoHudEnabled.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val isDriveModeOpen by viewModel.isDriveModeOpen.collectAsState()
    val isSettingsOpen by viewModel.isSettingsOpen.collectAsState()
    val isEqualizerOpen by viewModel.isEqualizerOpen.collectAsState()
    val isQueueOpen by viewModel.isQueueOpen.collectAsState()

    LaunchedEffect(autoHudEnabled, isPlaying, isDriveModeOpen, isSettingsOpen, isEqualizerOpen, isQueueOpen) {
        if (!autoHudEnabled || !isPlaying || isDriveModeOpen || isSettingsOpen || isEqualizerOpen || isQueueOpen) return@LaunchedEffect

        while (isActive) {
            val interaction = kotlinx.coroutines.withTimeoutOrNull(20_000L) {
                userInteractionChannel.receive()
            }
            if (interaction == null) {
                if (autoHudEnabled && isPlaying && !isDriveModeOpen && !isSettingsOpen && !isEqualizerOpen && !isQueueOpen) {
                    viewModel.openDriveMode()
                }
                return@LaunchedEffect
            }
        }
    }
}
