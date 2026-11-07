package com.aashik.music.ui.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.Podcasts
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
 * Layout: [Favorite] [Download]  ...  [Shuffle] [Lyrics] [Radio]
 * All buttons adhere to Material 3 container & on-container tokens with standard touch targets.
 */
@Composable
fun MobilePlayerActionBar(
    viewModel: MusicViewModel,
    currentSong: Song?,
    isFavorite: Boolean,
    isLyricsExpanded: Boolean,
    isRadioActive: Boolean,
    isRadioLoading: Boolean,
    currentDownloadStatus: DownloadStatus,
    @Suppress("UNUSED_PARAMETER") controlInactiveColor: Color = Color.Unspecified,
    @Suppress("UNUSED_PARAMETER") isNightDimmerActive: Boolean = false,
    @Suppress("UNUSED_PARAMETER") isDark: Boolean = true,
    onToggleLyrics: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val isStream = currentSong?.isStream() == true
    val isDownloadedSong = currentSong?.let {
        viewModel.isSongDownloaded(it.id) || it.id.startsWith("download_") || it.path.contains("MusicPlayer")
    } == true
    val isStreamDownloadable = (isStream && (currentSong?.duration ?: 0) > 0) || isDownloadedSong
    val isDownloaded = currentDownloadStatus is DownloadStatus.Downloaded || isDownloadedSong
    val isDownloading = currentDownloadStatus is DownloadStatus.Downloading

    val surfaceContainer = MaterialTheme.colorScheme.surfaceContainerHigh
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val primaryColor = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val onPrimaryContainer = MaterialTheme.colorScheme.onPrimaryContainer

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // ── Left Side: Favorite + Download ──
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Favorite Button (Material 3 Error/ErrorContainer)
            IconButton(
                onClick = { currentSong?.path?.let { viewModel.toggleFavorite(it) } },
                modifier = Modifier.size(44.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            color = if (isFavorite) MaterialTheme.colorScheme.errorContainer
                                   else surfaceContainer,
                            shape = ActionButtonCircle
                        )
                        .clip(ActionButtonCircle),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                        tint = if (isFavorite) MaterialTheme.colorScheme.error else onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Download Button (only for downloadable streams)
            if (isStreamDownloadable) {
                IconButton(
                    onClick = {
                        currentSong?.let {
                            if (isDownloaded) {
                                viewModel.deleteDownload(it)
                            } else {
                                viewModel.downloadSong(it)
                            }
                        }
                    },
                    modifier = Modifier.size(44.dp)
                ) {
                    val downloadBg = when {
                        isDownloaded -> MaterialTheme.colorScheme.tertiaryContainer
                        isDownloading -> primaryContainer
                        else -> surfaceContainer
                    }
                    val downloadTint = when {
                        isDownloaded -> MaterialTheme.colorScheme.onTertiaryContainer
                        isDownloading -> primaryColor
                        else -> onSurfaceVariant
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(color = downloadBg, shape = ActionButtonCircle)
                            .clip(ActionButtonCircle),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isDownloading) {
                            val progress = (currentDownloadStatus as? DownloadStatus.Downloading)?.progress ?: 0f
                            CircularProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp,
                                color = primaryColor
                            )
                        } else {
                            Icon(
                                imageVector = if (isDownloaded) Icons.Rounded.CheckCircle else Icons.Rounded.Download,
                                contentDescription = if (isDownloaded) "Saved Offline" else "Download Track",
                                tint = downloadTint,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }

        // ── Right Side: Playback Mode (Shuffle / Queue / Repeat One) + Lyrics Toggle + Radio ──
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Playback Mode Toggle: Shuffle, Queue, Repeat Current Song
            val playbackMode by viewModel.playbackMode.collectAsState()
            IconButton(
                onClick = { viewModel.cyclePlaybackMode() },
                modifier = Modifier.size(44.dp)
            ) {
                val (modeIcon, modeDesc, isModeActive) = when (playbackMode) {
                    PlaybackMode.SHUFFLE -> Triple(Icons.Rounded.Shuffle, "Shuffle", true)
                    PlaybackMode.QUEUE -> Triple(Icons.AutoMirrored.Rounded.QueueMusic, "Sequential Queue", false)
                    PlaybackMode.REPEAT_ONE -> Triple(Icons.Rounded.RepeatOne, "Repeat Current Song", true)
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

            // Lyrics Toggle Button
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

            // Radio Button (only for streams)
            if (isStream) {
                IconButton(
                    onClick = { viewModel.startRadioForCurrentSong() },
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
                                contentDescription = "Start Radio",
                                tint = if (isRadioActive) onPrimaryContainer else onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
