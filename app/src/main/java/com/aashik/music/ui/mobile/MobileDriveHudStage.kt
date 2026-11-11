package com.aashik.music.ui.mobile

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DirectionsCar
import com.aashik.music.ui.designsystem.Icon
import com.aashik.music.ui.designsystem.MaterialTheme
import com.aashik.music.ui.designsystem.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aashik.music.model.NavigatingTrack
import com.aashik.music.model.Song
import com.aashik.music.ui.AlbumArtImage
import com.aashik.music.ui.components.HudExpandedLyricsView
import com.aashik.music.ui.components.HudLyricLineDisplay
import com.aashik.music.viewmodel.MusicViewModel

private val MobileArtworkShape = RoundedCornerShape(16.dp)
private val MobileMinimizedArtworkShape = RoundedCornerShape(10.dp)

@Composable
fun MobileDriveHudStage(
    viewModel: MusicViewModel,
    currentSong: Song?,
    isPlaying: Boolean,
    navigatingTrack: NavigatingTrack,
    @Suppress("UNUSED_PARAMETER") isRadioActive: Boolean,
    isNightDimmerActive: Boolean,
    primaryTextColor: Color,
    isLyricsExpanded: Boolean = false,
    onToggleLyrics: (Boolean) -> Unit = {},
    onHideControls: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val playVideoForStreams by viewModel.playVideoForStreams.collectAsState()
    val currentVideoId by viewModel.currentVideoId.collectAsState()
    val currentSpeedKmh by viewModel.currentSpeedKmh.collectAsState()
    val isSpeedLockActive = currentSpeedKmh > 1.0f
    val isVideoActive = playVideoForStreams && !isSpeedLockActive && (!currentVideoId.isNullOrBlank() || currentSong?.isVideo() == true)

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (isVideoActive) {
            // In video mode, video is full-screen and completely unobstructed.
            // Current song album art is before Queue in the action bar, and Next Song replaces the EQ list.
            Spacer(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        onHideControls()
                    }
            )
        } else {
            // Row: Hero Visual (Artwork) on Left, Song Title & Artist next to it on Right
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                val mobileArtworkSize by animateDpAsState(
                    targetValue = if (isLyricsExpanded) 0.dp else 130.dp,
                    animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
                    label = "MobileHudArtworkSize"
                )

                // Hero Album Artwork with automotive horizontal slide transition
                Box(
                    modifier = Modifier
                        .size(mobileArtworkSize)
                        .background(
                            color = if (isNightDimmerActive) Color(0xFF0D1117)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            shape = if (isLyricsExpanded) MobileMinimizedArtworkShape else MobileArtworkShape
                        )
                        .clip(if (isLyricsExpanded) MobileMinimizedArtworkShape else MobileArtworkShape)
                ) {
                    AnimatedContent(
                        targetState = navigatingTrack,
                        modifier = Modifier.fillMaxSize(),
                        transitionSpec = {
                            if (targetState.direction >= 0) {
                                (slideInHorizontally(
                                    animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
                                    initialOffsetX = { fullWidth -> fullWidth }
                                ) + fadeIn(animationSpec = tween(durationMillis = 280)))
                                    .togetherWith(
                                        slideOutHorizontally(
                                            animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
                                            targetOffsetX = { fullWidth -> -fullWidth }
                                        ) + fadeOut(animationSpec = tween(durationMillis = 200))
                                    )
                            } else {
                                (slideInHorizontally(
                                    animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
                                    initialOffsetX = { fullWidth -> -fullWidth }
                                ) + fadeIn(animationSpec = tween(durationMillis = 280)))
                                    .togetherWith(
                                        slideOutHorizontally(
                                            animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
                                            targetOffsetX = { fullWidth -> fullWidth }
                                        ) + fadeOut(animationSpec = tween(durationMillis = 200))
                                    )
                            }
                        },
                        label = "MobileCarPlaySlideTransition"
                    ) { navState ->
                        val song = navState.song
                        if (song != null) {
                            AlbumArtImage(
                                path = song.path,
                                artworkUrl = song.artworkUrl,
                                size = mobileArtworkSize,
                                borderRadius = if (isLyricsExpanded) 10.dp else 16.dp,
                                isPlaying = isPlaying,
                                isHighRes = true,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DirectionsCar,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                    modifier = Modifier.size(if (isLyricsExpanded) 32.dp else 52.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(if (isLyricsExpanded) 12.dp else 18.dp))

                // Song Info Typography or Expanded Lyrics (Fixed height 130.dp, weight(1f))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(130.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    AnimatedContent(
                        targetState = isLyricsExpanded,
                        transitionSpec = {
                            (slideInVertically(animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)) { fullHeight -> fullHeight / 4 } +
                             fadeIn(animationSpec = tween(durationMillis = 260)))
                                .togetherWith(
                                    slideOutVertically(animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)) { fullHeight -> -fullHeight / 4 } +
                                    fadeOut(animationSpec = tween(durationMillis = 180))
                                )
                        },
                        label = "MobileHudLyricsToggleTransition",
                        modifier = Modifier.fillMaxSize()
                    ) { expanded ->
                        if (expanded) {
                            HudExpandedLyricsView(
                                viewModel = viewModel,
                                primaryTextColor = primaryTextColor,
                                secondaryTextColor = primaryTextColor.copy(alpha = 0.7f),
                                onClose = { onToggleLyrics(false) },
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            // Song Info Typography (Title, Artist, Album) - Next to Album Art
                            AnimatedContent(
                                targetState = navigatingTrack,
                                transitionSpec = {
                                    if (targetState.direction >= 0) {
                                        (slideInVertically(
                                            animationSpec = tween(durationMillis = 300, delayMillis = 30, easing = FastOutSlowInEasing),
                                            initialOffsetY = { fullHeight -> fullHeight / 2 }
                                        ) + fadeIn(animationSpec = tween(durationMillis = 260, delayMillis = 30)))
                                            .togetherWith(
                                                slideOutVertically(
                                                    animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
                                                    targetOffsetY = { fullHeight -> -fullHeight / 2 }
                                                ) + fadeOut(animationSpec = tween(durationMillis = 180))
                                            )
                                    } else {
                                        (slideInVertically(
                                            animationSpec = tween(durationMillis = 300, delayMillis = 30, easing = FastOutSlowInEasing),
                                            initialOffsetY = { fullHeight -> -fullHeight / 2 }
                                        ) + fadeIn(animationSpec = tween(durationMillis = 260, delayMillis = 30)))
                                            .togetherWith(
                                                slideOutVertically(
                                                    animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
                                                    targetOffsetY = { fullHeight -> fullHeight / 2 }
                                                ) + fadeOut(animationSpec = tween(durationMillis = 180))
                                            )
                                    }
                                },
                                label = "MobileTypographyTransition",
                                modifier = Modifier.fillMaxWidth()
                            ) { track ->
                                val song = track.song
                                Column(
                                    horizontalAlignment = Alignment.Start,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = song?.title ?: currentSong?.title ?: "No Song Playing",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Black,
                                        color = primaryTextColor,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Spacer(modifier = Modifier.height(3.dp))

                                    val albumStr = song?.album ?: currentSong?.album.orEmpty()
                                    val artistStr = song?.artist ?: currentSong?.artist ?: "Select a track to start"
                                    val fullSongTitle = song?.title ?: currentSong?.title
                                    Text(
                                        text = if (albumStr.isNotBlank() && albumStr != fullSongTitle) "$artistStr • $albumStr" else artistStr,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    HudLyricLineDisplay(
                                        viewModel = viewModel,
                                        textColor = primaryTextColor.copy(alpha = 0.9f),
                                        fontSize = 12.sp,
                                        onClick = { onToggleLyrics(true) },
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
