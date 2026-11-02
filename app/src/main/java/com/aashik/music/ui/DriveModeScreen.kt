package com.aashik.music.ui

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Podcasts
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.ui.draw.scale
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import com.aashik.music.model.NavigatingTrack
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aashik.music.model.Song
import com.aashik.music.service.DownloadStatus
import com.aashik.music.utils.formatTime
import com.aashik.music.utils.getCurrentTimeWithAmPm
import com.aashik.music.viewmodel.MusicViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Precomputed shapes hoisted to top-level constants for Mali GPU fill rate & zero GC
private val DriveHudContainerShape = RoundedCornerShape(0.dp)
private val ExitButtonShape = RoundedCornerShape(12.dp)
private val BigArtworkShape = RoundedCornerShape(20.dp)
private val MobileArtworkShape = RoundedCornerShape(16.dp)
private val DriverBadgeShape = RoundedCornerShape(8.dp)
private val SwipeFeedbackBadgeShape = RoundedCornerShape(14.dp)
private val ClockBadgeShape = RoundedCornerShape(8.dp)
private val UpNextBadgeShape = RoundedCornerShape(10.dp)
private val UpNextQueueCardShape = RoundedCornerShape(12.dp)
private val UpNextItemRowShape = RoundedCornerShape(8.dp)
private val TopVisualizerCapsuleShape = RoundedCornerShape(10.dp)
private val EqPresetChipShape = RoundedCornerShape(10.dp)
private val BottomMusicCardShape = RoundedCornerShape(18.dp)
private val NightDimmerPillShape = RoundedCornerShape(10.dp)
private val ActionArtThumbnailShape = RoundedCornerShape(8.dp)
private val NextSongActionBarShape = RoundedCornerShape(10.dp)
private val VideoOverlayCardShape = RoundedCornerShape(16.dp)
private val LyricLineShape = RoundedCornerShape(6.dp)
private val LyricBadgeShape = RoundedCornerShape(6.dp)
private val VideoOverlayBorderColor = Color(0x33FFFFFF)
private val VideoOverlayCardBg = Color(0xD90D131C)

private val DriveHudBackground = Color(0xFF090D14)
private val StealthOledBlack = Color(0xFF000000)
private val FavoriteRedColor = Color(0xFFFF5252)
private val NightDimmerAmber = Color(0xFFFFB300)

/**
 * Full-Screen Distraction-Free Drive Mode HUD.
 * Designed for Automotive Headunit (1280x720) dashboard visibility while driving.
 * Features:
 * - Top Row: Left side Time (12-Hour AM/PM Clock, Exit button, Night Highway Dimmer, Sleep Timer)
 *            Right side Live Animated Canvas Equalizer with direct Preset Chips (Flat, Acoustic, Vocal, Bass, Rock).
 * - Center Stage: Distraction-free Hero Album Artwork (240dp) with Apple CarPlay horizontal slide transitions
 *                 and blind swipe gestures (swipe left/right to skip, double-tap to pause).
 * - Bottom Row: Infotainment Music Card with full-width seekbar, Song Info (Title, Artist, Album, Favorite)
 *               on the left, and Player Controls (Shuffle, Prev, -10s, Hero Play/Pause, +10s, Next, Folder) on the right.
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
    val isRadioLoading by viewModel.isRadioLoading.collectAsState()
    val activeRadioStationName by viewModel.activeRadioStationName.collectAsState()
    val isRadioActive = isRadioLoading || activeRadioStationName != null
    val downloadStates by viewModel.downloadStates.collectAsState()
    val currentDownloadStatus = currentSong?.id?.let { downloadStates[it] } ?: DownloadStatus.Idle
    val isQueueOpen by viewModel.isQueueOpen.collectAsState()
    val userQueue by viewModel.userQueue.collectAsState()
    val isShuffleOn by viewModel.isShuffleOn.collectAsState()
    val upcomingSongs = remember(currentSong, userQueue, isShuffleOn) {
        viewModel.getUpcomingSongs(limit = 15)
    }

    var isNightDimmerActive by rememberSaveable { mutableStateOf(false) }

    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT

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

    val playVideoForStreams by viewModel.playVideoForStreams.collectAsState()
    val currentVideoId by viewModel.currentVideoId.collectAsState()
    val isVideoActive = playVideoForStreams && (!currentVideoId.isNullOrBlank() || currentSong?.isYouTube() == true || currentSong?.isLocalVideo() == true)
    var areControlsVisible by remember { mutableStateOf(true) }
    var lastInteractionTimestamp by remember { mutableLongStateOf(0L) }
    var isLyricsExpanded by rememberSaveable { mutableStateOf(false) }

    val currentLyrics by viewModel.currentLyrics.collectAsState()
    val isLoadingLyrics by viewModel.isLoadingLyrics.collectAsState()

    LaunchedEffect(currentSong?.id) {
        areControlsVisible = true
        lastInteractionTimestamp = System.currentTimeMillis()
    }

    // Collapse expanded lyrics only if loading has completed and no lyrics exist for the current track
    LaunchedEffect(currentLyrics, isLoadingLyrics) {
        if (!isLoadingLyrics && currentLyrics.isEmpty() && isLyricsExpanded) {
            isLyricsExpanded = false
        }
    }

    // Auto-hide controls after 4.5 seconds of inactivity during video playback
    LaunchedEffect(isVideoActive, areControlsVisible, isPlaying, lastInteractionTimestamp) {
        if (isVideoActive && areControlsVisible && isPlaying) {
            delay(4500L)
            areControlsVisible = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color = if (isVideoActive) StealthOledBlack else hudBg, shape = DriveHudContainerShape)
            .statusBarsPadding()
            .pointerInput(swipeThresholdPx) {
                var accumulatedDrag = 0f
                detectHorizontalDragGestures(
                    onDragStart = {
                        accumulatedDrag = 0f
                    },
                    onDragEnd = {
                        if (accumulatedDrag < -swipeThresholdPx) {
                            viewModel.playNextSong()
                            swipeFeedbackText = "⏭ Next Track"
                            coroutineScope.launch {
                                delay(900L)
                                swipeFeedbackText = null
                            }
                        } else if (accumulatedDrag > swipeThresholdPx) {
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
                    onTap = {
                        if (isVideoActive) {
                            areControlsVisible = !areControlsVisible
                            if (areControlsVisible) {
                                lastInteractionTimestamp = System.currentTimeMillis()
                            }
                        }
                    },
                    onDoubleTap = {
                        viewModel.togglePlayPause()
                    }
                )
            }
    ) {
        // ── Full-Screen Edge-to-Edge Video ──
        if (isVideoActive) {
            VideoPlayerView(
                videoId = currentVideoId ?: currentSong?.getYouTubeVideoId(),
                videoUrl = if (currentSong?.isLocalVideo() == true) currentSong?.path else null,
                viewModel = viewModel,
                onTap = {
                    areControlsVisible = !areControlsVisible
                    if (areControlsVisible) {
                        lastInteractionTimestamp = System.currentTimeMillis()
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Dimming scrim when controls overlay is visible for high contrast
            if (areControlsVisible) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.35f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            areControlsVisible = false
                        }
                )
            }
        }

        // ── Controls Overlay ──
        AnimatedVisibility(
            visible = !isVideoActive || areControlsVisible,
            enter = fadeIn(animationSpec = tween(250)),
            exit = fadeOut(animationSpec = tween(250)),
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                awaitPointerEvent(PointerEventPass.Initial)
                                lastInteractionTimestamp = System.currentTimeMillis()
                            }
                        }
                    }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
            // ── Top Row: Left Side Time & Exit | Right Side Equalizer with Presets ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left Side: Exit Button, Time (Digital Clock with AM/PM), Dimmer, Sleep Timer
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

                    // Time: Continuous Digital Clock with glanceable 12-Hour AM/PM
                    DriveHudClock(
                        isDark = isDark,
                        isNightDimmer = isNightDimmerActive
                    )

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
                            color = if (isNightDimmerActive) NightDimmerAmber else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    // Sleep Timer Badge (if active)
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

                // Right Side: Play Music Video Toggle Switch (Top-Right in HUD Mode)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .height(36.dp)
                            .background(
                                color = if (playVideoForStreams) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                                        else if (isNightDimmerActive) Color(0xFF161C24)
                                        else if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                        else MaterialTheme.colorScheme.surfaceVariant,
                                shape = NightDimmerPillShape
                            )
                            .border(
                                width = 1.dp,
                                color = if (playVideoForStreams) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                        else Color.Transparent,
                                shape = NightDimmerPillShape
                            )
                            .clickable { viewModel.setPlayVideoForStreams(!playVideoForStreams) }
                            .padding(start = 10.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (playVideoForStreams) "Music Video" else "Video Off",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (playVideoForStreams) MaterialTheme.colorScheme.primary
                                    else if (isNightDimmerActive) Color(0xFF8B98A9)
                                    else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                        Switch(
                            checked = playVideoForStreams,
                            onCheckedChange = null,
                            modifier = Modifier.scale(0.75f),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }

            if (isPortrait) {
                MobileDriveHudStage(
                    viewModel = viewModel,
                    currentSong = currentSong,
                    isPlaying = isPlaying,
                    navigatingTrack = navigatingTrack,
                    isRadioActive = isRadioActive,
                    isNightDimmerActive = isNightDimmerActive,
                    primaryTextColor = primaryTextColor,
                    isLyricsExpanded = isLyricsExpanded,
                    onToggleLyrics = { isLyricsExpanded = it },
                    onHideControls = { areControlsVisible = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 4.dp)
                )
            } else {
                // ── Landscape Infotainment Stage: Unified Centered Layout ──
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 4.dp),
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
                                    areControlsVisible = false
                                }
                        )
                    } else {
                        // Anchored Hero Visual & Info Container: Album Art is strictly anchored and NEVER shifts
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                modifier = Modifier
                                    .widthIn(max = 700.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Start
                            ) {
                                // Hero Album Artwork with Apple CarPlay horizontal slide transition
                                Box(
                                    modifier = Modifier
                                        .size(160.dp)
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
                                                artworkUrl = song.artworkUrl,
                                                size = 160.dp,
                                                borderRadius = 20.dp,
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
                                                    modifier = Modifier.size(68.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(24.dp))

                                // Content Area: Song Info or Synced Lyrics (Fixed height 160.dp matching Album Art)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(160.dp),
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
                                        label = "LandscapeHudLyricsToggleTransition",
                                        modifier = Modifier.fillMaxSize()
                                    ) { expanded ->
                                        if (expanded) {
                                            HudExpandedLyricsView(
                                                viewModel = viewModel,
                                                primaryTextColor = primaryTextColor,
                                                secondaryTextColor = secondaryTextColor,
                                                onClose = { isLyricsExpanded = false },
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            // Current Song Title & Artist with Automotive Vertical Slide Transition
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
                                                label = "DriveHudTypographyTransition",
                                                modifier = Modifier.fillMaxWidth()
                                            ) { track ->
                                                val song = track.song
                                                Column(
                                                    horizontalAlignment = Alignment.Start,
                                                    verticalArrangement = Arrangement.Center
                                                ) {
                                                    Text(
                                                        text = song?.title ?: "No Song Playing",
                                                        fontSize = 20.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = primaryTextColor,
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis
                                                    )

                                                    Spacer(modifier = Modifier.height(4.dp))

                                                    val albumStr = song?.album.orEmpty()
                                                    val artistStr = song?.artist ?: "Select a track to start"
                                                    Text(
                                                        text = buildAnnotatedString {
                                                            withStyle(
                                                                SpanStyle(
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = MaterialTheme.colorScheme.primary
                                                                )
                                                            ) {
                                                                append(artistStr)
                                                            }
                                                            if (albumStr.isNotBlank() && albumStr != song?.title) {
                                                                withStyle(
                                                                    SpanStyle(
                                                                        fontWeight = FontWeight.Normal,
                                                                        color = secondaryTextColor
                                                                    )
                                                                ) {
                                                                    append(" • $albumStr")
                                                                }
                                                            }
                                                        },
                                                        fontSize = 13.sp,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )

                                                    HudLyricLineDisplay(
                                                        viewModel = viewModel,
                                                        textColor = primaryTextColor.copy(alpha = 0.9f),
                                                        fontSize = 12.5.sp,
                                                        onClick = { isLyricsExpanded = true },
                                                        modifier = Modifier.padding(top = 6.dp)
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
            }

            // ── Bottom Row: Seeker Bar and Playback Controls ──
            val bottomCardBg = when {
                isVideoActive -> Color(0xD90A0E17)
                isNightDimmerActive -> Color(0xFF090D13)
                isDark -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            }
            val bottomCardBorder = when {
                isNightDimmerActive -> Color(0xFF141D2A)
                isDark -> Color(0xFF222C3A)
                else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            }

            // ── Unified Action Bar (Album Art, Queue, Fav, Download, Radio, Next Song) ──
            DriveHudActionBar(
                viewModel = viewModel,
                currentSong = currentSong,
                isPlaying = isPlaying,
                upcomingSongs = upcomingSongs,
                isFavorite = isFavorite,
                isRadioActive = isRadioActive,
                isRadioLoading = isRadioLoading,
                currentDownloadStatus = currentDownloadStatus,
                isNightDimmerActive = isNightDimmerActive,
                isDark = isDark,
                controlInactiveColor = controlInactiveColor,
                isVideoActive = isVideoActive,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = if (isPortrait) 6.dp else 0.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color = bottomCardBg, shape = BottomMusicCardShape)
                    .border(width = 0.8.dp, color = bottomCardBorder, shape = BottomMusicCardShape)
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                if (isPortrait) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Full-width Seeker Bar in portrait
                        DriveHudSeekBar(
                            viewModel = viewModel,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Centered Driving Playback Controls Row in portrait
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            HudPlaybackControlButtons(
                                viewModel = viewModel,
                                isPlaying = isPlaying,
                                isShuffleOn = isShuffleOn,
                                isStream = currentSong?.isStream() == true,
                                controlIconColor = controlIconColor,
                                controlInactiveColor = controlInactiveColor
                            )
                        }
                    }
                } else {
                    // ── Landscape Headunit: 50% Seekbar & 50% Playback Button Container Side-by-Side ──
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 50% Left: Seeker Bar Container
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            DriveHudSeekBar(
                                viewModel = viewModel,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // 50% Right: Playback Buttons Container
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            HudPlaybackControlButtons(
                                viewModel = viewModel,
                                isPlaying = isPlaying,
                                isShuffleOn = isShuffleOn,
                                isStream = currentSong?.isStream() == true,
                                controlIconColor = controlIconColor,
                                controlInactiveColor = controlInactiveColor
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

        // ── Full-Screen Playback Queue Drawer Overlay (when toggled from Left Column) ──
        if (isQueueOpen) {
            QueueDrawer(
                viewModel = viewModel,
                onClose = { viewModel.closeQueue() },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * Unified Quick Action Bar for Drive HUD.
 * Displays:
 * [Current Song Art (only art)] -> [Queue] -> [Favorite] -> [Download] -> [Radio] -> [Next Song (replaces EQ list)] -> [EQ Dialog].
 * Uses zero dynamic allocations (all precomputed constants) and Mali GPU friendly flat colors.
 */
@Composable
private fun DriveHudActionBar(
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
                    .size(38.dp)
                    .background(color = actionButtonBg, shape = ActionArtThumbnailShape)
                    .clip(ActionArtThumbnailShape)
            ) {
                AlbumArtImage(
                    path = currentSong.path,
                    artworkUrl = currentSong.artworkUrl,
                    size = 38.dp,
                    borderRadius = 8.dp,
                    isPlaying = isPlaying,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // 2. Queue Drawer Button
        Box(
            modifier = Modifier
                .size(38.dp)
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
                .size(38.dp)
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
        // 4. Download Button (Circular 38dp, styled identical to Fav button)
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
                    .size(38.dp)
                    .background(color = downloadBg, shape = CircleShape)
                    .clickable { currentSong?.let { viewModel.downloadSong(it) } },
                contentAlignment = Alignment.Center
            ) {
                if (isDownloading) {
                    val progress = (currentDownloadStatus as? DownloadStatus.Downloading)?.progress ?: 0f
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.size(18.dp),
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

        // 5. Start Radio Button (Circular 38dp, styled identical to Fav button)
        if (isStream) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(
                        color = if (isRadioActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f) else actionButtonBg,
                        shape = CircleShape
                    )
                    .clickable { viewModel.startRadioForCurrentSong() },
                contentAlignment = Alignment.Center
            ) {
                if (isRadioLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
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

        // 6. Next Song Container (Redesigned with thumbnail, stacked title & artist, skip icon)
        val nextSong = upcomingSongs.firstOrNull()
        if (nextSong != null) {
            Row(
                modifier = Modifier
                    .height(38.dp)
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
                    size = 28.dp,
                    borderRadius = 6.dp,
                    isPlaying = false,
                    modifier = Modifier.size(28.dp)
                )

                Column(
                    modifier = Modifier.widthIn(min = 90.dp, max = 220.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.Start
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "NEXT",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.4.sp
                        )
                        Text(
                            text = nextSong.title,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isNightDimmerActive) Color(0xFFE2E8F0) else if (isDark) Color.White else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                    Text(
                        text = nextSong.artist,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Normal,
                        color = if (isNightDimmerActive) Color(0xFF8B98A9) else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .height(38.dp)
                    .background(color = actionButtonBg, shape = NextSongActionBarShape)
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                    contentDescription = null,
                    tint = controlInactiveColor,
                    modifier = Modifier.size(13.dp)
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

/**
 * Isolated leaf composable displaying the currently sung synchronized lyric line.
 * Recomposition-scoped to playback ticks without triggering recompositions of the outer HUD stage.
 * Features smooth slide + crossfade animation when moving between lines.
 */
@Composable
private fun HudLyricLineDisplay(
    viewModel: MusicViewModel,
    textColor: Color,
    fontSize: androidx.compose.ui.unit.TextUnit,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val lyrics by viewModel.currentLyrics.collectAsState()
    val positionMs by viewModel.positionFlow.collectAsState()
    val isLoadingLyrics by viewModel.isLoadingLyrics.collectAsState()

    if (lyrics.isEmpty() && !isLoadingLyrics) return

    val currentLine = remember(lyrics, positionMs, isLoadingLyrics) {
        if (lyrics.isEmpty() && isLoadingLyrics) {
            "♪ Loading lyrics..."
        } else {
            val idx = lyrics.indexOfLast { it.timeMs <= positionMs }
            if (idx >= 0) {
                val line = lyrics[idx]
                val nextLineTime = lyrics.getOrNull(idx + 1)?.timeMs
                if (nextLineTime != null && positionMs >= nextLineTime && line.text.isBlank()) {
                    "♪ ♪ ♪"
                } else {
                    line.text.ifBlank { "♪ ♪ ♪" }
                }
            } else {
                "♪ Lyrics"
            }
        }
    }

    AnimatedContent(
        targetState = currentLine,
        transitionSpec = {
            (slideInVertically(animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)) { height -> height / 2 } +
             fadeIn(animationSpec = tween(durationMillis = 260)))
                .togetherWith(
                    slideOutVertically(animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)) { height -> -height / 2 } +
                    fadeOut(animationSpec = tween(durationMillis = 180))
                )
        },
        label = "HudLyricLineAnimation",
        modifier = modifier
    ) { lineText ->
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    shape = LyricLineShape
                )
                .clip(LyricLineShape)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.MusicNote,
                contentDescription = "Lyrics",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = lineText,
                fontSize = fontSize,
                fontWeight = FontWeight.SemiBold,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Isolated leaf composable displaying full synchronized scrolling lyrics when expanded.
 * Tapping a lyric line seeks to that timestamp.
 * Tapping "Show Info" / Close returns to title and artist view.
 */
@Composable
private fun HudExpandedLyricsView(
    viewModel: MusicViewModel,
    primaryTextColor: Color,
    secondaryTextColor: Color,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lyrics by viewModel.currentLyrics.collectAsState()
    val positionMs by viewModel.positionFlow.collectAsState()
    val isLoadingLyrics by viewModel.isLoadingLyrics.collectAsState()

    if (lyrics.isEmpty()) {
        Column(
            modifier = modifier
                .clip(LyricLineShape)
                .clickable { onClose() }
                .padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = if (isLoadingLyrics) "Loading synchronized lyrics..." else "No synchronized lyrics available",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = secondaryTextColor
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isLoadingLyrics) "Fetching lyrics..." else "Tap to show track info",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        return
    }

    val currentIndex = remember(lyrics, positionMs) {
        lyrics.indexOfLast { it.timeMs <= positionMs }.coerceAtLeast(0)
    }

    val listState = rememberLazyListState()

    LaunchedEffect(currentIndex) {
        if (currentIndex in lyrics.indices) {
            listState.animateScrollToItem(
                index = maxOf(0, currentIndex - 1),
                scrollOffset = 0
            )
        }
    }

    Column(
        modifier = modifier
    ) {
        // Top Header: "LYRICS" Badge on Left, "Show Info" Toggle on Right
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                        shape = LyricBadgeShape
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.MusicNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = "LYRICS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 0.5.sp
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier
                    .clip(LyricBadgeShape)
                    .clickable { onClose() }
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "Show Info",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = secondaryTextColor
                )
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Close Lyrics",
                    tint = secondaryTextColor,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        // Synced Lyrics Scrollable List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            itemsIndexed(
                items = lyrics,
                key = { index, line -> "${index}_${line.timeMs}" }
            ) { index, line ->
                val isCurrent = index == currentIndex
                val isPast = index < currentIndex

                Text(
                    text = line.text.ifBlank { "♪" },
                    fontSize = if (isCurrent) 16.sp else 12.5.sp,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                    color = when {
                        isCurrent -> MaterialTheme.colorScheme.primary
                        isPast -> secondaryTextColor.copy(alpha = 0.45f)
                        else -> primaryTextColor.copy(alpha = 0.85f)
                    },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(LyricLineShape)
                        .clickable {
                            viewModel.seekTo(line.timeMs)
                        }
                        .padding(vertical = 2.dp, horizontal = 4.dp)
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
    val isPlayerLoading by viewModel.isPlayerLoading.collectAsState()
    val currentSong by viewModel.currentSong.collectAsState()

    CustomHorizontalSeekBar(
        progress = progress,
        onProgressChanged = { viewModel.seekToFraction(it) },
        isPlaying = isPlaying,
        currentPositionMs = positionMs,
        durationMs = durationMs,
        waveSeed = currentSong?.id?.hashCode() ?: 42,
        showTimeLabels = true,
        isLoading = isPlayerLoading,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
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



@Composable
private fun MobileDriveHudStage(
    viewModel: MusicViewModel,
    currentSong: Song?,
    isPlaying: Boolean,
    navigatingTrack: NavigatingTrack,
    isRadioActive: Boolean,
    isNightDimmerActive: Boolean,
    primaryTextColor: Color,
    isLyricsExpanded: Boolean = false,
    onToggleLyrics: (Boolean) -> Unit = {},
    onHideControls: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val playVideoForStreams by viewModel.playVideoForStreams.collectAsState()
    val currentVideoId by viewModel.currentVideoId.collectAsState()
    val isVideoActive = playVideoForStreams && (!currentVideoId.isNullOrBlank() || currentSong?.isYouTube() == true || currentSong?.isLocalVideo() == true)

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
                // Hero Album Artwork with Apple CarPlay horizontal slide transition
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .background(
                            color = if (isNightDimmerActive) Color(0xFF0D1117)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            shape = MobileArtworkShape
                        )
                        .clip(MobileArtworkShape)
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
                        label = "MobileCarPlaySlideTransition"
                    ) { navState ->
                        val song = navState.song
                        if (song != null) {
                            AlbumArtImage(
                                path = song.path,
                                artworkUrl = song.artworkUrl,
                                size = 130.dp,
                                borderRadius = 16.dp,
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
                                    modifier = Modifier.size(52.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(18.dp))

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

@Composable
private fun HudPlaybackControlButtons(
    viewModel: MusicViewModel,
    isPlaying: Boolean,
    isShuffleOn: Boolean,
    isStream: Boolean,
    controlIconColor: Color,
    controlInactiveColor: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Shuffle Mode Toggle (only for local playback - hidden when streaming)
        if (!isStream) {
            IconButton(
                onClick = { viewModel.toggleShuffle() },
                modifier = Modifier.size(44.dp)
            ) {
                Text(
                    text = if (isShuffleOn) "SHUF" else "SEQ",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isShuffleOn) MaterialTheme.colorScheme.primary else controlInactiveColor
                )
            }
        }

        // Previous Track
        IconButton(
            onClick = { viewModel.playPreviousSong() },
            modifier = Modifier.size(52.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.SkipPrevious,
                contentDescription = "Previous Song",
                tint = controlIconColor,
                modifier = Modifier.size(36.dp)
            )
        }

        // Quick Skip -10s
        IconButton(
            onClick = { viewModel.skipBackward10s() },
            modifier = Modifier.size(44.dp)
        ) {
            Text(
                text = "-10s",
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                color = controlIconColor
            )
        }

        // Hero Play/Pause Button
        Box(
            modifier = Modifier
                .size(60.dp)
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
                modifier = Modifier.size(38.dp)
            )
        }

        // Quick Skip +10s
        IconButton(
            onClick = { viewModel.skipForward10s() },
            modifier = Modifier.size(44.dp)
        ) {
            Text(
                text = "+10s",
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                color = controlIconColor
            )
        }

        // Next Track
        IconButton(
            onClick = { viewModel.playNextSong() },
            modifier = Modifier.size(52.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.SkipNext,
                contentDescription = "Next Song",
                tint = controlIconColor,
                modifier = Modifier.size(36.dp)
            )
        }
    }
}
