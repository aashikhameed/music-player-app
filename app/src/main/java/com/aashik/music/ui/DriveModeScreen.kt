package com.aashik.music.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DarkMode
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
private val TopVisualizerCapsuleShape = RoundedCornerShape(10.dp)
private val BottomMusicCardShape = RoundedCornerShape(18.dp)
private val NightDimmerPillShape = RoundedCornerShape(10.dp)

private val DriveHudBackground = Color(0xFF090D14)
private val StealthOledBlack = Color(0xFF000000)
private val FavoriteRedColor = Color(0xFFFF5252)
private val NightDimmerAmber = Color(0xFFFFB300)

/**
 * Full-Screen Distraction-Free Drive Mode HUD.
 * Designed for Automotive Headunit (1280x720) dashboard visibility while driving.
 * Features:
 * - Top Row: 12-Hour AM/PM Clock, Sleep Timer, Live Animated Spectrum Visualizer & EQ Preset cycler,
 *            Night Highway Dimmer toggle, Exit button, and Folder selector.
 * - Center Stage: Large Album Art (190dp) + Giant Driving Controls Cluster (84dp play/pause, 68dp skip).
 * - Bottom Row: Dedicated Infotainment Music Card with full-width seekbar, large vertical roll
 *               typography (fade-up/down), and upcoming track peek pill.
 * - Blind swipe gestures (swipe left for next, swipe right for previous, double-tap to play/pause).
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

    var isNightDimmerActive by rememberSaveable { mutableStateOf(false) }

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val hudBg = when {
        isNightDimmerActive -> StealthOledBlack
        isDark -> DriveHudBackground
        else -> MaterialTheme.colorScheme.background
    }
    val primaryTextColor = when {
        isNightDimmerActive -> Color(0xFFE2E8F0)
        isDark -> Color.White
        else -> MaterialTheme.colorScheme.onBackground
    }
    val secondaryTextColor = when {
        isNightDimmerActive -> Color(0xFF8B98A9)
        isDark -> Color.White.copy(alpha = 0.65f)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val hintTextColor = when {
        isNightDimmerActive -> Color(0xFF5A6677)
        isDark -> Color.White.copy(alpha = 0.45f)
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
    }
    val controlIconColor = when {
        isNightDimmerActive -> Color(0xFFDCE4EE)
        isDark -> Color.White
        else -> MaterialTheme.colorScheme.onBackground
    }
    val controlInactiveColor = when {
        isNightDimmerActive -> Color(0xFF637184)
        isDark -> Color.White.copy(alpha = 0.65f)
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
    }

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
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // ── Top Row: Exit + Night Dimmer | Center Clock | Right: Equalizer Visualizer & Folder ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: Exit Button & Night Dimmer Toggle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onClose,
                        shape = ExitButtonShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isNightDimmerActive) Color(0xFF161C24)
                                             else if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                             else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Exit Drive Mode",
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Exit",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Night Highway Dimmer Mode Toggle
                    Row(
                        modifier = Modifier
                            .height(36.dp)
                            .background(
                                color = if (isNightDimmerActive) NightDimmerAmber.copy(alpha = 0.22f)
                                        else if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                        else MaterialTheme.colorScheme.surfaceVariant,
                                shape = NightDimmerPillShape
                            )
                            .clickable { isNightDimmerActive = !isNightDimmerActive }
                            .padding(horizontal = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DarkMode,
                            contentDescription = "Night Dimmer Mode",
                            tint = if (isNightDimmerActive) NightDimmerAmber else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = if (isNightDimmerActive) "Night ON" else "Dimmer",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isNightDimmerActive) NightDimmerAmber else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Center Telemetry: Digital Clock (AM/PM) & Sleep Timer
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DriveHudClock(
                        isDark = isDark,
                        isNightDimmer = isNightDimmerActive
                    )

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

                // Right: Equalizer Spectrum Visualizer & Folder Selector
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Live Animated Equalizer Spectrum Visualizer & Preset Switcher
                    TopBarEqualizerVisualizer(
                        viewModel = viewModel,
                        isDark = isDark,
                        isNightDimmer = isNightDimmerActive
                    )

                    // Folder Selector Chip
                    Row(
                        modifier = Modifier
                            .background(
                                color = if (isNightDimmerActive) Color(0xFF141A22)
                                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                shape = DriverBadgeShape
                            )
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { viewModel.playPreviousFolder() },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SkipPrevious,
                                contentDescription = "Previous Folder",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(15.dp)
                            )
                        }

                        Icon(
                            imageVector = Icons.Rounded.Folder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = viewModel.getCurrentFolderName(),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )

                        IconButton(
                            onClick = { viewModel.playNextFolder() },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SkipNext,
                                contentDescription = "Next Folder",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }

            // ── Center Stage: Hero Album Art + Giant Driving Controls Cluster ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Hero Album Artwork with Apple CarPlay horizontal slide transition
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .background(
                            color = if (isNightDimmerActive) Color(0xFF0D1117)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            shape = BigArtworkShape
                        )
                        .clip(BigArtworkShape)
                ) {
                    AnimatedContent(
                        targetState = navigatingTrack,
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
                        label = "DriveHudArtworkTransition"
                    ) { track ->
                        val song = track.song
                        if (song != null) {
                            AlbumArtImage(
                                path = song.path,
                                size = 180.dp,
                                borderRadius = 16.dp,
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
                                    modifier = Modifier.size(64.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(20.dp))

                // Giant Driver Touch Controls Deck
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Favorite Button
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
                                modifier = Modifier.size(34.dp)
                            )
                        }

                        // Previous Track (68dp touch target)
                        IconButton(
                            onClick = { viewModel.playPreviousSong() },
                            modifier = Modifier.size(68.dp)
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
                            modifier = Modifier.size(52.dp)
                        ) {
                            Text(
                                text = "-10s",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = controlIconColor
                            )
                        }

                        // Giant Hero Play/Pause Button (84dp)
                        Box(
                            modifier = Modifier
                                .size(84.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = CircleShape
                                )
                                .clickable { viewModel.togglePlayPause() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(52.dp)
                            )
                        }

                        // Quick Skip +10s
                        IconButton(
                            onClick = { viewModel.skipForward10s() },
                            modifier = Modifier.size(52.dp)
                        ) {
                            Text(
                                text = "+10s",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = controlIconColor
                            )
                        }

                        // Next Track (68dp touch target)
                        IconButton(
                            onClick = { viewModel.playNextSong() },
                            modifier = Modifier.size(68.dp)
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
                            modifier = Modifier.size(52.dp)
                        ) {
                            Text(
                                text = if (isShuffleOn) "SHUF" else "SEQ",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isShuffleOn) MaterialTheme.colorScheme.primary else controlInactiveColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Swipe ← / → anywhere to change track • Double-tap to pause",
                        fontSize = 11.sp,
                        color = hintTextColor
                    )
                }
            }

            // ── Bottom Row: Music Information & Seekbar Panel ──
            val bottomCardBg = when {
                isNightDimmerActive -> Color(0xFF090D13)
                isDark -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            }
            val bottomCardBorder = when {
                isNightDimmerActive -> Color(0xFF141D2A)
                isDark -> Color(0xFF222C3A)
                else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color = bottomCardBg, shape = BottomMusicCardShape)
                    .border(width = 0.8.dp, color = bottomCardBorder, shape = BottomMusicCardShape)
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Full-width Seeker Bar
                    DriveHudSeekBar(
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Track Metadata & Up-Next Peek Pill
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
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
                            label = "DriveHudBottomTypography",
                            modifier = Modifier.weight(1f)
                        ) { track ->
                            val song = track.song
                            Column {
                                Text(
                                    text = song?.title ?: "No Song Playing",
                                    fontSize = 21.sp,
                                    fontWeight = FontWeight.Black,
                                    color = primaryTextColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = song?.artist ?: "Select a track to start",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    val albumStr = song?.album.orEmpty()
                                    if (albumStr.isNotBlank()) {
                                        Text(
                                            text = "• $albumStr",
                                            fontSize = 12.sp,
                                            color = secondaryTextColor,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }

                        // Next Track Preview Pill
                        val nextSong = viewModel.getNextSong()
                        if (nextSong != null) {
                            Spacer(modifier = Modifier.width(12.dp))
                            Row(
                                modifier = Modifier
                                    .background(
                                        color = if (isNightDimmerActive) Color(0xFF141A22)
                                                else if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
                                        shape = UpNextBadgeShape
                                    )
                                    .clickable { viewModel.playNextSong() }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
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
                                    modifier = Modifier.widthIn(max = 260.dp)
                                )
                            }
                        }
                    }
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
 * Isolated leaf composable for the Top Bar Live Spectrum Visualizer & Equalizer Preset Cycler.
 * Renders 5 smooth dynamic audio bars directly on Canvas; clicking cycles the DSP preset.
 */
@Composable
private fun TopBarEqualizerVisualizer(
    viewModel: MusicViewModel,
    isDark: Boolean,
    isNightDimmer: Boolean,
    modifier: Modifier = Modifier
) {
    val isPlaying by viewModel.isPlaying.collectAsState()
    val preset by viewModel.equalizerPreset.collectAsState()

    val presetName = when (preset) {
        0 -> "Flat"
        1 -> "Acoustic"
        2 -> "Vocal"
        3 -> "Bass"
        4 -> "Rock"
        5 -> "Custom"
        else -> "EQ"
    }

    val badgeBg = when {
        isNightDimmer -> Color(0xFF131922)
        isDark -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = when {
        isNightDimmer -> Color(0xFFCCD6E0)
        isDark -> Color.White
        else -> MaterialTheme.colorScheme.onSurface
    }
    val primaryColor = MaterialTheme.colorScheme.primary

    // Dynamic phase for the spectrum bars - active only when playing to save Mali GPU fill rate
    val infiniteTransition = rememberInfiniteTransition(label = "EqVisualizerTransition")
    val phase by if (isPlaying) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 6.28318f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1100, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "EqPhase"
        )
    } else {
        remember { mutableStateOf(0f) }
    }

    Row(
        modifier = modifier
            .background(color = badgeBg, shape = TopVisualizerCapsuleShape)
            .clickable { viewModel.cycleEqualizerPreset() }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Canvas(modifier = Modifier.size(width = 24.dp, height = 15.dp)) {
            val barCount = 5
            val barWidth = 3.dp.toPx()
            val totalWidth = size.width
            val totalHeight = size.height
            val spacing = (totalWidth - (barWidth * barCount)) / (barCount - 1)
            val cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())

            for (i in 0 until barCount) {
                val heightFactor = if (isPlaying) {
                    val rawSin = kotlin.math.sin(phase.toDouble() + i * 1.25).toFloat()
                    (rawSin * 0.38f + 0.58f).coerceIn(0.2f, 1.0f)
                } else {
                    0.25f
                }
                val barH = totalHeight * heightFactor
                val left = i * (barWidth + spacing)
                val top = totalHeight - barH

                drawRoundRect(
                    color = primaryColor,
                    topLeft = Offset(left, top),
                    size = Size(barWidth, barH),
                    cornerRadius = cornerRadius
                )
            }
        }

        Text(
            text = presetName,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
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
            .padding(horizontal = 12.dp, vertical = 2.dp)
    )
}

/**
 * Isolated leaf composable for the Drive HUD clock.
 * Updates at 15s intervals without triggering recompositions of the outer HUD.
 */
@Composable
private fun DriveHudClock(
    isDark: Boolean,
    isNightDimmer: Boolean = false,
    modifier: Modifier = Modifier
) {
    var currentTime by remember { mutableStateOf(getCurrentTimeWithAmPm()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(15_000L)
            currentTime = getCurrentTimeWithAmPm()
        }
    }

    val badgeBg = when {
        isNightDimmer -> Color(0xFF131922)
        isDark -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = when {
        isNightDimmer -> Color(0xFFCCD6E0)
        isDark -> Color.White
        else -> MaterialTheme.colorScheme.onSurface
    }

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
