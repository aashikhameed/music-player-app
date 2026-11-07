package com.aashik.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Podcasts
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aashik.music.model.Song
import com.aashik.music.service.DownloadStatus
import com.aashik.music.ui.AlbumArtImage
import com.aashik.music.viewmodel.MusicViewModel

private val ActionArtThumbnailShape = RoundedCornerShape(8.dp)
private val NextSongActionBarShape = RoundedCornerShape(12.dp)
private val FavoriteRedColor = Color(0xFFFF4081)

/**
 * Unified Quick Action Bar for Drive HUD.
 * Displays:
 * [Current Song Art (only art)] -> [Queue] -> [Favorite] -> [Download] -> [Radio] -> [Next Song (replaces EQ list)] -> [EQ Dialog].
 * Uses zero dynamic allocations (all precomputed constants) and Mali GPU friendly flat colors.
 */
@Composable
fun DriveHudActionBar(
    viewModel: MusicViewModel,
    currentSong: Song?,
    isPlaying: Boolean,
    upcomingSongs: List<Song>,
    isFavorite: Boolean,
    isRadioActive: Boolean,
    isRadioLoading: Boolean,
    currentDownloadStatus: DownloadStatus,
    isNightDimmerActive: Boolean,
    isDark: Boolean,
    controlInactiveColor: Color,
    isVideoActive: Boolean = false,
    @Suppress("UNUSED_PARAMETER") isLyricsExpanded: Boolean = false,
    @Suppress("UNUSED_PARAMETER") onToggleLyrics: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isQueueOpen by viewModel.isQueueOpen.collectAsState()
    val isStream = currentSong?.isStream() == true
    val isStreamDownloadable = isStream && (currentSong?.duration ?: 0) > 0

    val actionButtonBg = when {
        isNightDimmerActive -> Color(0xFF131922)
        isDark -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
    ) {
        // 1. Current Song: Only Album Art immediately before Queue button
        if (currentSong != null && isVideoActive) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(color = actionButtonBg, shape = ActionArtThumbnailShape)
                    .clip(ActionArtThumbnailShape)
            ) {
                AlbumArtImage(
                    path = currentSong.path,
                    artworkUrl = currentSong.artworkUrl,
                    size = 44.dp,
                    borderRadius = 8.dp,
                    isPlaying = isPlaying,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // 2. Queue Drawer Button
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(
                    color = if (isQueueOpen) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f) else actionButtonBg,
                    shape = CircleShape
                )
                .clickable { viewModel.toggleQueue() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                contentDescription = "Upcoming Queue Drawer",
                tint = if (isQueueOpen) MaterialTheme.colorScheme.primary else controlInactiveColor,
                modifier = Modifier.size(20.dp)
            )
        }

        // 3. Favorite Button
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(
                    color = if (isFavorite) FavoriteRedColor.copy(alpha = 0.22f) else actionButtonBg,
                    shape = CircleShape
                )
                .clickable {
                    currentSong?.path?.let { viewModel.toggleFavorite(it) }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                contentDescription = "Toggle Favorite",
                tint = if (isFavorite) FavoriteRedColor else controlInactiveColor,
                modifier = Modifier.size(20.dp)
            )
        }

        // 4. Download Button (if streamable)
        if (isStreamDownloadable) {
            val isDownloaded = currentDownloadStatus is DownloadStatus.Downloaded
            val isDownloading = currentDownloadStatus is DownloadStatus.Downloading
            val downloadTint = when {
                isDownloaded -> Color(0xFF00E676)
                isDownloading -> MaterialTheme.colorScheme.primary
                else -> controlInactiveColor
            }
            val downloadBg = when {
                isDownloaded -> Color(0xFF00E676).copy(alpha = 0.22f)
                isDownloading -> MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                else -> actionButtonBg
            }

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(color = downloadBg, shape = CircleShape)
                    .clickable {
                        currentSong?.let {
                            if (isDownloaded) {
                                viewModel.redownloadSong(it)
                            } else {
                                viewModel.downloadSong(it)
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isDownloading) {
                    val progress = (currentDownloadStatus as? DownloadStatus.Downloading)?.progress ?: 0f
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Icon(
                        imageVector = if (isDownloaded) Icons.Rounded.CheckCircle else Icons.Rounded.Download,
                        contentDescription = if (isDownloaded) "Saved Offline" else "Download Track",
                        tint = downloadTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // 5. Start Radio Button
        if (isStream) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        color = if (isRadioActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f) else actionButtonBg,
                        shape = CircleShape
                    )
                    .clickable { viewModel.startRadioForCurrentSong() },
                contentAlignment = Alignment.Center
            ) {
                if (isRadioLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Podcasts,
                        contentDescription = "Start Radio",
                        tint = if (isRadioActive) MaterialTheme.colorScheme.primary else controlInactiveColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // 6. Next Song Container
        val nextSong = upcomingSongs.firstOrNull()
        if (nextSong != null) {
            Row(
                modifier = Modifier
                    .height(44.dp)
                    .background(color = actionButtonBg, shape = NextSongActionBarShape)
                    .border(
                        width = 1.dp,
                        color = if (isNightDimmerActive) Color(0xFF192330)
                                else if (isDark) Color(0xFF283445)
                                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        shape = NextSongActionBarShape
                    )
                    .clickable { viewModel.play(nextSong, direction = 1) }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AlbumArtImage(
                    path = nextSong.path,
                    artworkUrl = nextSong.artworkUrl,
                    size = 32.dp,
                    borderRadius = 6.dp,
                    isPlaying = false,
                    modifier = Modifier.size(32.dp)
                )

                Column(
                    modifier = Modifier.widthIn(min = 80.dp, max = 180.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.Start
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = nextSong.title,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isNightDimmerActive) Color(0xFFE2E8F0) else if (isDark) Color.White else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .height(44.dp)
                    .background(color = actionButtonBg, shape = NextSongActionBarShape)
                    .border(
                        width = 1.dp,
                        color = if (isNightDimmerActive) Color(0xFF192330)
                                else if (isDark) Color(0xFF283445)
                                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        shape = NextSongActionBarShape
                    )
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                    contentDescription = null,
                    tint = controlInactiveColor,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "Queue empty",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = controlInactiveColor
                )
            }
        }
    }
}
