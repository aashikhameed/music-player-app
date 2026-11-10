package com.aashik.music.ui.mobile

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import com.aashik.music.model.NavigatingTrack
import com.aashik.music.model.Song
import com.aashik.music.service.DownloadStatus
import com.aashik.music.ui.AlbumArtImage
import com.aashik.music.ui.components.HudExpandedLyricsView
import com.aashik.music.viewmodel.MusicViewModel

private val HeroArtworkShape = RoundedCornerShape(20.dp)
private val FloatingArtButtonShape = CircleShape
private val FloatingArtButtonSize = 44.dp
private val FloatingArtButtonBg = Color(0xB3121620)
private val FloatingArtButtonBorder = Color(0x33FFFFFF)

/**
 * Full-width portrait hero stage for the mobile player.
 *
 * Priority (mutually exclusive, top wins):
 *  1. isVideoActive  → transparent spacer; video renders beneath via VideoPlayerView
 *  2. isLyricsExpanded → HudExpandedLyricsView (synced scrollable lyrics)
 *  3. default        → Album artwork with floating Heart (left) and Download (right) buttons
 */
@Composable
fun MobileHeroStage(
    viewModel: MusicViewModel,
    currentSong: Song?,
    isPlaying: Boolean,
    navigatingTrack: NavigatingTrack,
    isLyricsExpanded: Boolean,
    isNightDimmerActive: Boolean,
    primaryTextColor: Color,
    secondaryTextColor: Color,
    isFavorite: Boolean = false,
    currentDownloadStatus: DownloadStatus = DownloadStatus.Idle,
    onToggleLyrics: (Boolean) -> Unit,
    onHideControls: () -> Unit,
    modifier: Modifier = Modifier
) {
    val playVideoForStreams by viewModel.playVideoForStreams.collectAsState()
    val currentVideoId by viewModel.currentVideoId.collectAsState()
    val currentSpeedKmh by viewModel.currentSpeedKmh.collectAsState()
    val isSpeedLockActive = currentSpeedKmh > 1.0f
    val isVideoActive = playVideoForStreams
            && !isSpeedLockActive
            && (!currentVideoId.isNullOrBlank()
                || currentSong?.isYouTube() == true
                || currentSong?.isLocalVideo() == true)

    val stageModifier = if (isLyricsExpanded) {
        modifier.fillMaxWidth()
    } else {
        modifier
            .fillMaxWidth()
            .aspectRatio(1f)
    }

    Box(
        modifier = stageModifier,
        contentAlignment = Alignment.Center
    ) {
        val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

        // 1. Clipped Card Content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(HeroArtworkShape)
                .background(
                    color = if (isNightDimmerActive) Color(0xFF0D1117)
                            else if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                            else MaterialTheme.colorScheme.surfaceContainerLowest,
                    shape = HeroArtworkShape
                )
                .border(
                    width = if (!isDark && isLyricsExpanded) 1.dp else 0.dp,
                    color = if (!isDark && isLyricsExpanded) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f) else Color.Transparent,
                    shape = HeroArtworkShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isVideoActive) {
                // ── 1. Video mode: transparent spacer (video layer is below in the z-stack) ──
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onHideControls() }
                )
            } else {
                // ── 2. Full-Resolution Album Art with CarPlay horizontal slide transition ──
                AnimatedContent(
                    targetState = navigatingTrack,
                    modifier = Modifier.fillMaxSize(),
                    transitionSpec = {
                        if (targetState.direction >= 0) {
                            (slideInHorizontally(
                                animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
                                initialOffsetX = { w -> w }
                            ) + fadeIn(animationSpec = tween(durationMillis = 280)))
                                .togetherWith(
                                    slideOutHorizontally(
                                        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
                                        targetOffsetX = { w -> -w }
                                    ) + fadeOut(animationSpec = tween(durationMillis = 200))
                                )
                        } else {
                            (slideInHorizontally(
                                animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
                                initialOffsetX = { w -> -w }
                            ) + fadeIn(animationSpec = tween(durationMillis = 280)))
                                .togetherWith(
                                    slideOutHorizontally(
                                        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
                                        targetOffsetX = { w -> w }
                                    ) + fadeOut(animationSpec = tween(durationMillis = 200))
                                )
                        }
                    },
                    label = "MobileHeroArtTransition"
                ) { navState ->
                    val song = navState.song ?: currentSong
                    if (song != null) {
                        AlbumArtImage(
                            path = song.path,
                            artworkUrl = song.artworkUrl,
                            size = 360.dp,
                            borderRadius = 20.dp,
                            isPlaying = isPlaying,
                            isHighRes = true,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.MusicNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxSize(0.35f)
                        )
                    }
                }

                // ── 3. High-Contrast Dimming Backdrop when Lyrics is Shown ──
                val scrimAlpha by animateFloatAsState(
                    targetValue = if (isLyricsExpanded) 0.96f else 0.0f,
                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                    label = "LyricsScrimAlpha"
                )

                if (scrimAlpha > 0.01f) {
                    val scrimColor = when {
                        isNightDimmerActive -> Color(0xFF05080E)
                        isDark -> Color(0xFF070B12)
                        else -> MaterialTheme.colorScheme.surfaceContainerLowest
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                color = scrimColor.copy(alpha = scrimAlpha)
                            )
                    )
                }

                // ── 4. Redesigned Lyrics Layer Inside Album Art ──
                if (isLyricsExpanded) {
                    HudExpandedLyricsView(
                        viewModel = viewModel,
                        primaryTextColor = primaryTextColor,
                        secondaryTextColor = secondaryTextColor,
                        onClose = { onToggleLyrics(false) },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // ── 5. Heart (Left) and Download (Right) Buttons Inside Album Art ──
                AnimatedVisibility(
                    visible = !isLyricsExpanded,
                    enter = fadeIn(animationSpec = tween(220)),
                    exit = fadeOut(animationSpec = tween(180)),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 14.dp)
                ) {
                    val isStream = currentSong?.isStream() == true
                    val isDownloadedSong = currentSong?.let {
                        viewModel.isSongDownloaded(it.id) || it.id.startsWith("download_") || it.path.contains("MusicPlayer")
                    } == true
                    val isStreamDownloadable = (isStream && (currentSong?.duration ?: 0) > 0) || isDownloadedSong
                    val isDownloaded = currentDownloadStatus is DownloadStatus.Downloaded || isDownloadedSong
                    val isDownloading = currentDownloadStatus is DownloadStatus.Downloading

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: Favorite Heart Button
                        IconButton(
                            onClick = { currentSong?.path?.let { viewModel.toggleFavorite(it) } },
                            modifier = Modifier.size(FloatingArtButtonSize)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        color = if (isFavorite) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.92f)
                                                else FloatingArtButtonBg,
                                        shape = FloatingArtButtonShape
                                    )
                                    .border(
                                        width = 0.8.dp,
                                        color = if (isFavorite) MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                                                else FloatingArtButtonBorder,
                                        shape = FloatingArtButtonShape
                                    )
                                    .clip(FloatingArtButtonShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                    contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                                    tint = if (isFavorite) MaterialTheme.colorScheme.error else Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        // Right: Download Button (for downloadable streams or offline indicator)
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
                                modifier = Modifier.size(FloatingArtButtonSize)
                            ) {
                                val downloadBg = when {
                                    isDownloaded -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.92f)
                                    isDownloading -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f)
                                    else -> FloatingArtButtonBg
                                }
                                val downloadTint = when {
                                    isDownloaded -> MaterialTheme.colorScheme.onTertiaryContainer
                                    isDownloading -> MaterialTheme.colorScheme.primary
                                    else -> Color.White
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(color = downloadBg, shape = FloatingArtButtonShape)
                                        .border(
                                            width = 0.8.dp,
                                            color = if (isDownloaded) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.6f)
                                                    else if (isDownloading) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                                    else FloatingArtButtonBorder,
                                            shape = FloatingArtButtonShape
                                        )
                                        .clip(FloatingArtButtonShape),
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
                        } else {
                            // Empty spacer to keep heart anchored to left
                            Box(modifier = Modifier.size(FloatingArtButtonSize))
                        }
                    }
                }
            }
        }
    }
}

