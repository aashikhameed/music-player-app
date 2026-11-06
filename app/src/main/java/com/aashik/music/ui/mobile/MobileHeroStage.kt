package com.aashik.music.ui.mobile

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aashik.music.model.NavigatingTrack
import com.aashik.music.model.Song
import com.aashik.music.ui.AlbumArtImage
import com.aashik.music.ui.components.HudExpandedLyricsView
import com.aashik.music.viewmodel.MusicViewModel

private val HeroArtworkShape = RoundedCornerShape(20.dp)
private val DimArtworkBorder = Color(0x33FFFFFF)

/**
 * Full-width portrait hero stage for the mobile player.
 *
 * Priority (mutually exclusive, top wins):
 *  1. isVideoActive  → transparent spacer; video renders beneath via VideoPlayerView
 *  2. isLyricsExpanded → HudExpandedLyricsView (synced scrollable lyrics)
 *  3. default        → Album artwork with CarPlay slide transition
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
        // 1. Clipped Card Content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(HeroArtworkShape)
                .background(
                    color = if (isNightDimmerActive) Color(0xFF0D1117)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
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
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                color = if (isNightDimmerActive) Color(0xFF05080E).copy(alpha = scrimAlpha)
                                        else Color(0xFF070B12).copy(alpha = scrimAlpha)
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
            }
        }
    }
}

