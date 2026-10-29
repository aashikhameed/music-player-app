package com.aashik.music.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.withStyle
import com.aashik.music.theme.AppGradients
import com.aashik.music.utils.getCurrentTimeWithAmPm
import com.aashik.music.viewmodel.MusicViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Precomputed shapes hoisted to top-level constants for Mali GPU fill rate & zero GC
private val DriveHudContainerShape = RoundedCornerShape(0.dp)
private val ExitButtonShape = RoundedCornerShape(12.dp)
private val BigArtworkShape = RoundedCornerShape(20.dp)
private val DriverBadgeShape = RoundedCornerShape(8.dp)
private val SwipeFeedbackBadgeShape = RoundedCornerShape(14.dp)
private val ClockBadgeShape = RoundedCornerShape(8.dp)
private val UpNextBadgeShape = RoundedCornerShape(8.dp)

private val DriveHudBackground = Color(0xFF090D14)
private val FavoriteRedColor = Color(0xFFFF5252)

/**
 * Full-Screen Distraction-Free Drive Mode HUD.
 * Designed for Automotive Headunit (1280x720) dashboard visibility while driving.
 * Features:
 * - Huge touch targets (80dp play/pause, 52dp skip).
 * - Blind swipe gestures (swipe left for next, swipe right for previous, tap to play/pause).
 * - Massive high-contrast track typography readable from driver distance.
 */
@Composable
fun DriveModeScreen(
    viewModel: MusicViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentSong by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val favoritePaths by viewModel.favoritePaths.collectAsState()
    val isFavorite = currentSong?.path?.let { favoritePaths.contains(it) } ?: false
    val navigatingTrack by viewModel.navigatingTrack.collectAsState()

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val hudBg = if (isDark) DriveHudBackground else MaterialTheme.colorScheme.background
    val primaryTextColor = if (isDark) Color.White else MaterialTheme.colorScheme.onBackground
    val secondaryTextColor = if (isDark) Color.White.copy(alpha = 0.65f) else MaterialTheme.colorScheme.onSurfaceVariant
    val hintTextColor = if (isDark) Color.White.copy(alpha = 0.45f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
    val controlIconColor = if (isDark) Color.White else MaterialTheme.colorScheme.onBackground
    val controlInactiveColor = if (isDark) Color.White.copy(alpha = 0.65f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)

    val swipeThresholdPx = with(androidx.compose.ui.platform.LocalDensity.current) { 56.dp.toPx() }
    var swipeFeedbackText by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color = hudBg, shape = DriveHudContainerShape)
            .statusBarsPadding()
            .pointerInput(swipeThresholdPx) {
                var accumulatedDrag = 0f
                detectHorizontalDragGestures(
                    onDragStart = {
                        accumulatedDrag = 0f
                    },
                    onDragEnd = {
                        if (accumulatedDrag < -swipeThresholdPx) {
                            // Swiped Left -> Next Song
                            viewModel.playNextSong()
                            swipeFeedbackText = "⏭ Next Track"
                            coroutineScope.launch {
                                delay(900L)
                                swipeFeedbackText = null
                            }
                        } else if (accumulatedDrag > swipeThresholdPx) {
                            // Swiped Right -> Previous Song
                            viewModel.playPreviousSong()
                            swipeFeedbackText = "⏮ Previous Track"
                            coroutineScope.launch {
                                delay(900L)
                                swipeFeedbackText = null
                            }
                        }
                        accumulatedDrag = 0f
                    },
                    onDragCancel = {
                        accumulatedDrag = 0f
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        accumulatedDrag += dragAmount
                    }
                )
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        viewModel.togglePlayPause()
                    }
                )
            }
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // ── Top Bar: Exit Drive Mode & Telemetry Badge ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = onClose,
                    shape = ExitButtonShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                         else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.height(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Exit Drive Mode",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Exit Drive HUD",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Center Telemetry: Digital Clock & Sleep Timer
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DriveHudClock(isDark = isDark)

                    val sleepSec by viewModel.sleepTimerRemainingSec.collectAsState()
                    if (sleepSec > 0) {
                        val mins = (sleepSec / 60) + 1
                        Row(
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    shape = ClockBadgeShape
                                )
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "💤 ${mins}m",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                shape = DriverBadgeShape
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { viewModel.playPreviousFolder() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SkipPrevious,
                                contentDescription = "Previous Folder",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Icon(
                            imageVector = Icons.Rounded.Folder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = viewModel.getCurrentFolderName(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )

                        IconButton(
                            onClick = { viewModel.playNextFolder() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SkipNext,
                                contentDescription = "Next Folder",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // ── Middle Content: Large Artwork & Big Typography ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Large Album Art Box with Apple CarPlay horizontal slide transition
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            shape = BigArtworkShape
                        )
                        .clip(BigArtworkShape)
                ) {
                    AnimatedContent(
                        targetState = navigatingTrack,
                        transitionSpec = {
                            if (targetState.direction >= 0) {
                                // Next Track: Enter from right, exit to left
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
                                // Previous Track: Enter from left, exit to right
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
                        label = "DriveHudArtworkTransition"
                    ) { track ->
                        val song = track.song
                        if (song != null) {
                            AlbumArtImage(
                                path = song.path,
                                size = 140.dp,
                                borderRadius = 10.dp,
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
                                    modifier = Modifier.size(56.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(28.dp))

                // Song Info (Large, high-contrast text for driver glance with Apple CarPlay smooth crossfade)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
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
                        label = "DriveHudTypographyTransition"
                    ) { track ->
                        val song = track.song
                        Column {
                            Text(
                                text = song?.title ?: "No Song Playing",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                color = primaryTextColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = song?.artist ?: "Select a track to start",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            val folderName = viewModel.getCurrentFolderName()
                            val albumStr = song?.album.orEmpty()
                            val secondaryText = if (albumStr.isNotBlank()) "$albumStr • 📁 $folderName" else "📁 $folderName"

                            Text(
                                text = secondaryText,
                                fontSize = 12.sp,
                                color = secondaryTextColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    val nextSong = viewModel.getNextSong()
                    if (nextSong != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .background(
                                    color = if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                    shape = UpNextBadgeShape
                                )
                                .clickable { viewModel.playNextSong() }
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SkipNext,
                                contentDescription = "Next Track",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "NEXT: ",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${nextSong.title} • ${nextSong.artist}",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = primaryTextColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Swipe ← / → to change track • Double-tap to pause",
                        fontSize = 10.5.sp,
                        color = hintTextColor
                    )
                }
            }

            // ── Progress & Seek Bar ──
            DriveHudSeekBar(
                viewModel = viewModel,
                modifier = Modifier.fillMaxWidth()
            )

            // ── Bottom Controls: Oversized buttons ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Favorite Heart Button
                IconButton(
                    onClick = {
                        currentSong?.path?.let { viewModel.toggleFavorite(it) }
                    },
                    modifier = Modifier.size(54.dp)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = "Toggle Favorite",
                        tint = if (isFavorite) FavoriteRedColor else controlInactiveColor,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Previous Track
                IconButton(
                    onClick = { viewModel.playPreviousSong() },
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipPrevious,
                        contentDescription = "Previous Song",
                        tint = controlIconColor,
                        modifier = Modifier.size(46.dp)
                    )
                }

                // Quick Skip -10s
                IconButton(
                    onClick = { viewModel.skipBackward10s() },
                    modifier = Modifier.size(54.dp)
                ) {
                    Text(
                        text = "-10s",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = controlIconColor
                    )
                }

                // Giant Play / Pause Button (80dp)
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        )
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = {
                                    viewModel.togglePlayPause()
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(48.dp)
                    )
                }

                // Quick Skip +10s
                IconButton(
                    onClick = { viewModel.skipForward10s() },
                    modifier = Modifier.size(54.dp)
                ) {
                    Text(
                        text = "+10s",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = controlIconColor
                    )
                }

                // Next Track
                IconButton(
                    onClick = { viewModel.playNextSong() },
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = "Next Song",
                        tint = controlIconColor,
                        modifier = Modifier.size(46.dp)
                    )
                }

                // Shuffle Mode Toggle
                val isShuffleOn by viewModel.isShuffleOn.collectAsState()
                IconButton(
                    onClick = { viewModel.toggleShuffle() },
                    modifier = Modifier.size(54.dp)
                ) {
                    Text(
                        text = if (isShuffleOn) "SHUF" else "SEQ",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isShuffleOn) MaterialTheme.colorScheme.primary else controlInactiveColor
                    )
                }
            }
        }

        // ── Gesture Feedback Overlay Badge ──
        if (swipeFeedbackText != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(color = Color(0xF2151D2A), shape = SwipeFeedbackBadgeShape)
                    .padding(horizontal = 24.dp, vertical = 14.dp)
            ) {
                Text(
                    text = swipeFeedbackText.orEmpty(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }
        }
    }
}

/**
 * Isolated leaf composable for the Drive HUD seek bar.
 * Ensures 250ms playback ticks only recompose the seekbar and not the outer HUD screen.
 */
@Composable
private fun DriveHudSeekBar(
    viewModel: MusicViewModel,
    modifier: Modifier = Modifier
) {
    val positionMs by viewModel.positionFlow.collectAsState()
    val durationMs by viewModel.durationFlow.collectAsState()
    val progress by viewModel.currentProgressFlow.collectAsState(initial = 0f)
    val isPlaying by viewModel.isPlaying.collectAsState()

    CustomHorizontalSeekBar(
        progress = progress,
        onProgressChanged = { viewModel.seekToFraction(it) },
        isPlaying = isPlaying,
        currentPositionMs = positionMs,
        durationMs = durationMs,
        waveSeed = 42,
        showTimeLabels = true,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 2.dp)
    )
}

/**
 * Isolated leaf composable for the Drive HUD clock.
 * Updates at 30s intervals without triggering recompositions of the outer HUD.
 */
@Composable
private fun DriveHudClock(
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    var currentTime by remember { mutableStateOf(getCurrentTimeWithAmPm()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(15_000L)
            currentTime = getCurrentTimeWithAmPm()
        }
    }

    val badgeBg = if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface

    val timeParts = currentTime.split(" ")
    val timeDigits = timeParts.getOrElse(0) { currentTime }
    val amPmLabel = timeParts.getOrElse(1) { "" }

    Row(
        modifier = modifier
            .background(color = badgeBg, shape = ClockBadgeShape)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.Schedule,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(15.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = buildAnnotatedString {
                withStyle(
                    SpanStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        letterSpacing = 0.2.sp
                    )
                ) { append(timeDigits) }
                if (amPmLabel.isNotEmpty()) {
                    append("\u202F")
                    withStyle(
                        SpanStyle(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 8.5.sp,
                            baselineShift = BaselineShift.Superscript,
                            color = textColor.copy(alpha = 0.75f)
                        )
                    ) { append(amPmLabel) }
                }
            },
            color = textColor
        )
    }
}
