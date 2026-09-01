package com.aashik.music.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BluetoothAudio
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.ShuffleOn
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aashik.music.bluetooth.BluetoothMediaManager
import com.aashik.music.theme.AppGradients
import com.aashik.music.viewmodel.MusicViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BottomControlStrip(
    viewModel: MusicViewModel,
    isSearchVisible: Boolean,
    isMapOpen: Boolean,
    onToggleSearch: () -> Unit,
    onToggleMap: () -> Unit
) {
    val isPlaying by viewModel.isPlaying.collectAsState()
    val isShuffleOn by viewModel.isShuffleOn.collectAsState()
    val currentSong by viewModel.currentSong.collectAsState()
    val phoneMedia by BluetoothMediaManager.phoneMedia.collectAsState()

    // Use Bluetooth source if a phone is actively streaming audio to us
    val isBtStreaming = phoneMedia.isConnected && phoneMedia.isPlaying

    val dockGradient = AppGradients.dock()
    val capsuleGradient = AppGradients.capsule(isActive = false)
    val borderBrush = AppGradients.border(isActive = false)
    val primaryButtonGradient = AppGradients.primaryButton()

    Surface(
        color = Color.Transparent,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .background(brush = dockGradient, shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .border(border = BorderStroke(1.dp, borderBrush), shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 12.dp)
        ) {
            // Mini track information banner — switches between Local and Bluetooth source
            val showBanner = isBtStreaming || currentSong != null
            if (showBanner) {
                val bannerShape = RoundedCornerShape(12.dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(bannerShape)
                        .background(brush = capsuleGradient, shape = bannerShape)
                        .border(border = BorderStroke(1.dp, borderBrush), shape = bannerShape)
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isBtStreaming) {
                            // Bluetooth album art or fallback icon
                            val btArt = phoneMedia.albumArt
                            if (btArt != null) {
                                Image(
                                    bitmap = btArt.asImageBitmap(),
                                    contentDescription = "Album Art",
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.BluetoothAudio,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        } else {
                            AlbumArtImage(
                                path = currentSong?.path.orEmpty(),
                                size = 40.dp,
                                borderRadius = 8.dp,
                                isPlaying = isPlaying
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isBtStreaming) phoneMedia.title else currentSong?.title.orEmpty(),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isBtStreaming) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.basicMarquee()
                            )
                            Text(
                                text = if (isBtStreaming) "${phoneMedia.artist} • ${phoneMedia.appName}" else currentSong?.artist.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (isBtStreaming) {
                            // BT source badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "BT",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        } else {
                            // Theme toggle button in mini banner
                            IconButton(
                                onClick = { viewModel.toggleTheme() },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.LightMode,
                                    contentDescription = "Theme",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
            }

            // Balanced actions bottom row with Map toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Map Split Toggle
                IconButton(
                    onClick = onToggleMap,
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Map,
                        contentDescription = "Live Map",
                        tint = if (isMapOpen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Search Toggle
                IconButton(
                    onClick = onToggleSearch,
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = if (isSearchVisible) Icons.Rounded.SearchOff else Icons.Rounded.Search,
                        contentDescription = "Search",
                        tint = if (isSearchVisible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Previous - Large Target
                IconButton(
                    onClick = { if (isBtStreaming) BluetoothMediaManager.skipToPrevious() else viewModel.playPreviousSong() },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipPrevious,
                        contentDescription = "Previous",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(30.dp)
                    )
                }

                // Play / Pause Hero Button — routes to BT or local player
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(brush = primaryButtonGradient, shape = CircleShape)
                        .clickable {
                            if (isBtStreaming) BluetoothMediaManager.togglePlayPause()
                            else viewModel.togglePlayPause()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (if (isBtStreaming) phoneMedia.isPlaying else isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Next - Large Target
                IconButton(
                    onClick = { if (isBtStreaming) BluetoothMediaManager.skipToNext() else viewModel.playNextSong() },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = "Next",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(30.dp)
                    )
                }

                // Shuffle
                IconButton(
                    onClick = { viewModel.toggleShuffle() },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = if (isShuffleOn) Icons.Rounded.ShuffleOn else Icons.Rounded.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (isShuffleOn) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}
