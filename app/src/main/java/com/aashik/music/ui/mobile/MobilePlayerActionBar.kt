package com.aashik.music.ui.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.Podcasts
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aashik.music.model.PlaybackMode
import com.aashik.music.model.Song
import com.aashik.music.service.DownloadStatus
import com.aashik.music.viewmodel.MusicViewModel

private val ActionButtonCircle = CircleShape

/**
 * Mobile Portrait Action Bar (Strict Material 3 Standard).
 * Layout: [Playback Mode / Shuffle]  [Queue]  [Radio]  [Lyrics]
 * - Heart & Download are anchored inside the Hero Album Art.
 * - Queue button and Radio button are placed directly to the right of the Shuffle/Playback Mode button.
 * - When queue is from radio, shuffle is removed, toggling strictly between straight and repeat.
 * All buttons adhere to Material 3 container & on-container tokens with standard touch targets.
 */
@Composable
fun MobilePlayerActionBar(
    viewModel: MusicViewModel,
    currentSong: Song?,
    @Suppress("UNUSED_PARAMETER") isFavorite: Boolean = false,
    isLyricsExpanded: Boolean,
    isRadioActive: Boolean = false,
    isRadioLoading: Boolean = false,
    @Suppress("UNUSED_PARAMETER") currentDownloadStatus: DownloadStatus = DownloadStatus.Idle,
    @Suppress("UNUSED_PARAMETER") controlInactiveColor: Color = Color.Unspecified,
    @Suppress("UNUSED_PARAMETER") isNightDimmerActive: Boolean = false,
    @Suppress("UNUSED_PARAMETER") isDark: Boolean = true,
    onToggleLyrics: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val playbackMode by viewModel.playbackMode.collectAsState()
    val isQueueOpen by viewModel.isQueueOpen.collectAsState()

    val surfaceContainer = MaterialTheme.colorScheme.surfaceContainerHigh
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val primaryColor = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val onPrimaryContainer = MaterialTheme.colorScheme.onPrimaryContainer

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // 1. Playback Mode Toggle:
        // When radio is active, shuffle is excluded; toggles between Straight (Repeat off) and Repeat Current Song.
        // When normal, cycles Shuffle -> Straight -> Repeat Current Song.
        IconButton(
            onClick = { viewModel.cyclePlaybackMode() },
            modifier = Modifier.size(44.dp)
        ) {
            val (modeIcon, modeDesc, isModeActive) = when {
                isRadioActive -> {
                    if (playbackMode == PlaybackMode.REPEAT_ONE) {
                        Triple(Icons.Rounded.RepeatOne, "Repeat Current Song", true)
                    } else {
                        Triple(Icons.Rounded.Repeat, "Straight (Sequential)", false)
                    }
                }
                playbackMode == PlaybackMode.SHUFFLE -> Triple(Icons.Rounded.Shuffle, "Shuffle", true)
                playbackMode == PlaybackMode.REPEAT_ONE -> Triple(Icons.Rounded.RepeatOne, "Repeat Current Song", true)
                else -> Triple(Icons.Rounded.Repeat, "Straight (Sequential)", false)
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        color = if (isModeActive) primaryContainer else surfaceContainer,
                        shape = ActionButtonCircle
                    )
                    .clip(ActionButtonCircle),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = modeIcon,
                    contentDescription = modeDesc,
                    tint = if (isModeActive) onPrimaryContainer else onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // 2. Playback Queue Drawer Button (Placed directly to the right of Shuffle/Mode)
        IconButton(
            onClick = { viewModel.toggleQueue() },
            modifier = Modifier.size(44.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        color = if (isQueueOpen) primaryContainer else surfaceContainer,
                        shape = ActionButtonCircle
                    )
                    .clip(ActionButtonCircle),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                    contentDescription = "Playback Queue",
                    tint = if (isQueueOpen) onPrimaryContainer else onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // 3. Radio Button (Placed to the right of Queue)
        IconButton(
            onClick = {
                currentSong?.let {
                    viewModel.startRadioForSong(it)
                }
            },
            modifier = Modifier.size(44.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        color = if (isRadioActive) primaryContainer else surfaceContainer,
                        shape = ActionButtonCircle
                    )
                    .clip(ActionButtonCircle),
                contentAlignment = Alignment.Center
            ) {
                if (isRadioLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = primaryColor
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Podcasts,
                        contentDescription = if (isRadioActive) "Radio Active" else "Start Radio Station",
                        tint = if (isRadioActive) onPrimaryContainer else onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // 4. Lyrics Toggle Button
        IconButton(
            onClick = { onToggleLyrics(!isLyricsExpanded) },
            modifier = Modifier.size(44.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        color = if (isLyricsExpanded) primaryContainer else surfaceContainer,
                        shape = ActionButtonCircle
                    )
                    .clip(ActionButtonCircle),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Lyrics,
                    contentDescription = if (isLyricsExpanded) "Hide Lyrics" else "Show Lyrics",
                    tint = if (isLyricsExpanded) onPrimaryContainer else onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
