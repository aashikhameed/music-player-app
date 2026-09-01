package com.aashik.music.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BluetoothConnected
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.ShuffleOn
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aashik.music.theme.AppGradients
import com.aashik.music.viewmodel.MusicViewModel

/**
 * Modern Luxury Automotive Dock & Media Taskbar (Android Auto / Coolwalk Inspired).
 * Styled with theme-matched linear gradients and zero shadow elevation.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AndroidAutoBottomBar(
    viewModel: MusicViewModel,
    isMapOpen: Boolean,
    onToggleMap: () -> Unit,
    @Suppress("UNUSED_PARAMETER") isSearchVisible: Boolean = false,
    @Suppress("UNUSED_PARAMETER") onToggleSearch: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isPlaying by viewModel.isPlaying.collectAsState()
    val isShuffleOn by viewModel.isShuffleOn.collectAsState()
    val currentSong by viewModel.currentSong.collectAsState()
    val progress by viewModel.currentProgressFlow.collectAsState(initial = 0f)
    val positionMs by viewModel.positionFlow.collectAsState()
    val durationMs by viewModel.durationFlow.collectAsState()

    var currentTime by remember { mutableStateOf(getCurrentTimeWithAmPm()) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = getCurrentTimeWithAmPm()
            kotlinx.coroutines.delay(1000)
        }
    }

    val dockGradient = AppGradients.dock()
    val capsuleGradient = AppGradients.capsule(isActive = false)
    val navGradient = AppGradients.capsule(isActive = true)
    val borderBrush = AppGradients.border(isActive = false)
    val activeBorderBrush = AppGradients.border(isActive = true)
    val primaryButtonGradient = AppGradients.primaryButton()

    Surface(
        color = Color.Transparent,
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .background(brush = dockGradient)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Subtle luxury hairline top border
            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(76.dp)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ==========================================
                // 1. LEFT ISLAND: Navigation Launcher
                // ==========================================
                if (!isMapOpen) {
                    val navShape = RoundedCornerShape(18.dp)
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .clip(navShape)
                            .background(brush = navGradient, shape = navShape)
                            .border(border = BorderStroke(1.dp, activeBorderBrush), shape = navShape)
                            .clickable { onToggleMap() }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Map,
                                    contentDescription = "Map Navigation",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Column(verticalArrangement = Arrangement.Center) {
                                Text(
                                    text = "NAV",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Live Map",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // ==========================================
                // 2. CENTER ISLAND: Floating Media Cockpit
                // ==========================================
                val centerShape = RoundedCornerShape(18.dp)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(centerShape)
                        .background(brush = capsuleGradient, shape = centerShape)
                        .border(border = BorderStroke(1.dp, borderBrush), shape = centerShape)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Section A: Mini Album Thumbnail + Track Title & Artist
                        Row(
                            modifier = Modifier
                                .weight(0.24f)
                                .fillMaxHeight(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (currentSong != null) {
                                AlbumArtImage(
                                    path = currentSong?.path.orEmpty(),
                                    size = 46.dp,
                                    borderRadius = 10.dp,
                                    isPlaying = isPlaying
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.MusicNote,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            Column(
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = currentSong?.title ?: "No Track Playing",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = if (isPlaying) Modifier.basicMarquee() else Modifier
                                )
                                Text(
                                    text = currentSong?.artist ?: "Select music",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Section B: Waveform Progress Bar
                        Box(
                            modifier = Modifier
                                .weight(0.46f)
                                .fillMaxHeight(),
                            contentAlignment = Alignment.Center
                        ) {
                            CustomHorizontalSeekBar(
                                progress = progress,
                                onProgressChanged = { newProgress ->
                                    viewModel.seekToFraction(newProgress)
                                },
                                isPlaying = isPlaying,
                                currentPositionMs = positionMs,
                                durationMs = durationMs,
                                showTimeLabels = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Section C: Tactile Large Automotive Playback Controls
                        Row(
                            modifier = Modifier
                                .weight(0.30f)
                                .fillMaxHeight(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End)
                        ) {
                            // Shuffle Toggle
                            IconButton(
                                onClick = { viewModel.toggleShuffle() },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = if (isShuffleOn) Icons.Rounded.ShuffleOn else Icons.Rounded.Shuffle,
                                    contentDescription = "Shuffle",
                                    tint = if (isShuffleOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Previous Track - Large Touch Target
                            IconButton(
                                onClick = { viewModel.playPreviousSong() },
                                modifier = Modifier.size(46.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.SkipPrevious,
                                    contentDescription = "Previous",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(30.dp)
                                )
                            }

                            // Hero Play/Pause Circle Button - Large High-Visibility Driver Button
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(brush = primaryButtonGradient, shape = CircleShape)
                                    .clickable { viewModel.togglePlayPause() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            // Next Track - Large Touch Target
                            IconButton(
                                onClick = { viewModel.playNextSong() },
                                modifier = Modifier.size(46.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.SkipNext,
                                    contentDescription = "Next",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                    }
                }

                // ==========================================
                // 3. RIGHT ISLAND: Cockpit Status & Clock
                // ==========================================
                val rightShape = RoundedCornerShape(18.dp)
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .clip(rightShape)
                        .background(brush = capsuleGradient, shape = rightShape)
                        .border(border = BorderStroke(1.dp, borderBrush), shape = rightShape)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Day/Night Theme Toggle
                        IconButton(
                            onClick = { viewModel.toggleTheme() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.LightMode,
                                contentDescription = "Toggle Day/Night Mode",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        VerticalDivider(
                            modifier = Modifier
                                .height(24.dp)
                                .width(0.5.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        )

                        // Digital Clock & Bluetooth Sync Indicator
                        Column(
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.Start
                        ) {

                            Text(
                                text = currentTime,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp,
                                    fontSize = 14.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}
