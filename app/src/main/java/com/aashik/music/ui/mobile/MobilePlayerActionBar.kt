package com.aashik.music.ui.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.Podcasts
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aashik.music.service.DownloadStatus
import com.aashik.music.model.Song
import com.aashik.music.viewmodel.MusicViewModel

private val ActionButtonCircle = CircleShape
private val FavRedColor = Color(0xFFFF5252)
private val DownloadGreenColor = Color(0xFF00E676)

/**
 * Mobile portrait action bar.
 * Layout: [Favorite] [Download]  ...  [Lyrics] [Radio]
 * No vertical dots, no queue button.
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
    controlInactiveColor: Color,
    isNightDimmerActive: Boolean,
    isDark: Boolean,
    onToggleLyrics: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val actionButtonBg = when {
        isNightDimmerActive -> Color(0xFF131922)
        isDark -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val isStream = currentSong?.isStream() == true
    val isStreamDownloadable = isStream && (currentSong?.duration ?: 0) > 0
    val isDownloaded = currentDownloadStatus is DownloadStatus.Downloaded
    val isDownloading = currentDownloadStatus is DownloadStatus.Downloading

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // ── Left Side: Favorite + Download ──
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Favorite
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        color = if (isFavorite) FavRedColor.copy(alpha = 0.18f) else actionButtonBg,
                        shape = ActionButtonCircle
                    )
                    .clickable { currentSong?.path?.let { viewModel.toggleFavorite(it) } },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                    tint = if (isFavorite) FavRedColor else controlInactiveColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Download (only for downloadable streams)
            if (isStreamDownloadable) {
                val downloadTint = when {
                    isDownloaded -> DownloadGreenColor
                    isDownloading -> MaterialTheme.colorScheme.primary
                    else -> controlInactiveColor
                }
                val downloadBg = when {
                    isDownloaded -> DownloadGreenColor.copy(alpha = 0.18f)
                    isDownloading -> MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                    else -> actionButtonBg
                }

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(color = downloadBg, shape = ActionButtonCircle)
                        .clickable { currentSong?.let { viewModel.downloadSong(it) } },
                    contentAlignment = Alignment.Center
                ) {
                    if (isDownloading) {
                        val progress = (currentDownloadStatus as? DownloadStatus.Downloading)?.progress ?: 0f
                        CircularProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
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

        // ── Right Side: Lyrics Toggle + Radio ──
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Lyrics toggle
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        color = if (isLyricsExpanded) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else actionButtonBg,
                        shape = ActionButtonCircle
                    )
                    .clickable { onToggleLyrics(!isLyricsExpanded) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Lyrics,
                    contentDescription = if (isLyricsExpanded) "Hide Lyrics" else "Show Lyrics",
                    tint = if (isLyricsExpanded) MaterialTheme.colorScheme.primary else controlInactiveColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Radio (only for streams)
            if (isStream) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            color = if (isRadioActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else actionButtonBg,
                            shape = ActionButtonCircle
                        )
                        .clickable { viewModel.startRadioForCurrentSong() },
                    contentAlignment = Alignment.Center
                ) {
                    if (isRadioLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Podcasts,
                            contentDescription = "Start Radio",
                            tint = if (isRadioActive) MaterialTheme.colorScheme.primary else controlInactiveColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

