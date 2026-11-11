package com.aashik.music.ui

import com.aashik.music.ui.components.DriveHudActionBar
import com.aashik.music.ui.components.DriveHudClock
import com.aashik.music.ui.components.DriveHudSeekBar
import com.aashik.music.ui.components.HudExpandedLyricsView
import com.aashik.music.ui.components.HudLyricLineDisplay
import com.aashik.music.ui.components.HudPlaybackControlButtons
import com.aashik.music.ui.mobile.MobileDriveHudStage
import com.aashik.music.ui.mobile.MobileHeroStage
import com.aashik.music.ui.mobile.MobilePlayerActionBar

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.rounded.SmartDisplay
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Podcasts
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import com.aashik.music.ui.designsystem.Button
import com.aashik.music.ui.designsystem.ButtonDefaults
import com.aashik.music.ui.designsystem.CircularProgressIndicator
import com.aashik.music.ui.designsystem.Icon
import com.aashik.music.ui.designsystem.IconButton
import com.aashik.music.ui.designsystem.MaterialTheme
import com.aashik.music.ui.designsystem.Surface
import com.aashik.music.ui.designsystem.Switch
import com.aashik.music.ui.designsystem.SwitchDefaults
import com.aashik.music.ui.designsystem.Text
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
private val BigArtworkShape = RoundedCornerShape(16.dp)
private val MinimizedArtworkShape = RoundedCornerShape(12.dp)
private val MobileArtworkShape = RoundedCornerShape(16.dp)
private val MobileMinimizedArtworkShape = RoundedCornerShape(12.dp)
private val DriverBadgeShape = RoundedCornerShape(8.dp)
private val SwipeFeedbackBadgeShape = RoundedCornerShape(16.dp)
private val ClockBadgeShape = RoundedCornerShape(8.dp)
private val UpNextBadgeShape = RoundedCornerShape(8.dp)
private val UpNextQueueCardShape = RoundedCornerShape(12.dp)
private val UpNextItemRowShape = RoundedCornerShape(8.dp)
private val TopVisualizerCapsuleShape = RoundedCornerShape(8.dp)
private val EqPresetChipShape = RoundedCornerShape(8.dp)
private val BottomMusicCardShape = RoundedCornerShape(16.dp)
private val NightDimmerPillShape = RoundedCornerShape(8.dp)
private val ActionArtThumbnailShape = RoundedCornerShape(8.dp)
private val NextSongActionBarShape = RoundedCornerShape(8.dp)
private val VideoOverlayCardShape = RoundedCornerShape(16.dp)
private val LyricLineShape = RoundedCornerShape(8.dp)
private val LyricBadgeShape = RoundedCornerShape(8.dp)
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
 * - Center Stage: Distraction-free Hero Album Artwork (240dp) with automotive horizontal slide transitions
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
    val currentSpeedKmh by viewModel.currentSpeedKmh.collectAsState()
    val isSpeedLockActive = currentSpeedKmh > 1.0f
    val isVideoActive = playVideoForStreams && !isSpeedLockActive && (!currentVideoId.isNullOrBlank() || currentSong?.isVideo() == true)
    var areControlsVisible by remember { mutableStateOf(true) }
    var lastInteractionTimestamp by remember { mutableLongStateOf(0L) }
    var isLyricsExpanded by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(currentSong?.id) {
        areControlsVisible = true
        lastInteractionTimestamp = System.currentTimeMillis()
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
                videoId = currentVideoId ?: currentSong?.getVideoStreamId(),
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
            // ── Top Row: In portrait mode render a clean, balanced mobile top bar; in landscape render full HUD cockpit bar ──
            if (isPortrait) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left: Standard back navigation button matching AppTopBar
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Exit Drive Mode",
                            tint = if (isNightDimmerActive) Color(0xFFE2E8F0) else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Right: Clock and Sleep Status (moved to right in portrait)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val sleepSec by viewModel.sleepTimerRemainingSec.collectAsState()
                        if (sleepSec > 0) {
                            val mins = (sleepSec / 60) + 1
                            Row(
                                modifier = Modifier
                                    .background(
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                        shape = ClockBadgeShape
                                    )
                                    .padding(horizontal = 7.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "💤 ${mins}m",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        DriveHudClock(
                            isDark = isDark,
                            isNightDimmer = isNightDimmerActive
                        )
                    }
                }
            } else {
                // ── Landscape Headunit Cockpit Top Bar ──
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

                        // In-cabin speed & SCVC telemetry badge (Task 4.3)
                        val scvcGainDb by viewModel.scvcGainDb.collectAsState()
                        Row(
                            modifier = Modifier
                                .height(36.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    shape = ClockBadgeShape
                                )
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${currentSpeedKmh.toInt()} km/h • +${String.format(java.util.Locale.US, "%.1f", scvcGainDb)} dB",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Driving Safety Video Interlock Pill (Task 7.2)
                        if (isSpeedLockActive && playVideoForStreams) {
                            Row(
                                modifier = Modifier
                                    .height(36.dp)
                                    .background(
                                        color = Color(0x33FFB300),
                                        shape = ClockBadgeShape
                                    )
                                    .padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🛡️ Audio-Only (Driving Safety)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFFB300)
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

                        // OpenSL ES Audio Pipeline Latency Readout Badge (Task 4.3)
                        Row(
                            modifier = Modifier
                                .height(36.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    shape = NightDimmerPillShape
                                )
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "48kHz • 96KB • 5.3ms",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            if (isPortrait) {
                // ── Centered Mobile Portrait Stage: Hero + Info + Action Bar ──
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // ── Portrait Hero Stage (Art / Lyrics / Video) ──
                    MobileHeroStage(
                        viewModel = viewModel,
                        currentSong = currentSong,
                        isPlaying = isPlaying,
                        navigatingTrack = navigatingTrack,
                        isLyricsExpanded = isLyricsExpanded,
                        isNightDimmerActive = isNightDimmerActive,
                        primaryTextColor = primaryTextColor,
                        secondaryTextColor = secondaryTextColor,
                        isFavorite = isFavorite,
                        currentDownloadStatus = currentDownloadStatus,
                        onToggleLyrics = { isLyricsExpanded = it },
                        onHideControls = { areControlsVisible = false },
                        modifier = if (isLyricsExpanded) {
                            Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(horizontal = 20.dp, vertical = 4.dp)
                        } else {
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 6.dp)
                        }
                    )

                    // ── Portrait Title & Artist (Hidden when lyrics expanded to give maximum height) ──
                    AnimatedVisibility(
                        visible = !isLyricsExpanded,
                        enter = fadeIn(animationSpec = tween(220)) + expandVertically(animationSpec = tween(250)),
                        exit = fadeOut(animationSpec = tween(180)) + shrinkVertically(animationSpec = tween(200))
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Spacer(modifier = Modifier.height(10.dp))

                            // ── Portrait Title & Artist (Centered) ──
                            AnimatedContent(
                                targetState = navigatingTrack,
                                transitionSpec = {
                                    if (targetState.direction >= 0) {
                                        (slideInVertically(
                                            animationSpec = tween(durationMillis = 300, delayMillis = 30, easing = FastOutSlowInEasing),
                                            initialOffsetY = { h -> h / 2 }
                                        ) + fadeIn(animationSpec = tween(durationMillis = 260, delayMillis = 30)))
                                            .togetherWith(
                                                slideOutVertically(
                                                    animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
                                                    targetOffsetY = { h -> -h / 2 }
                                                ) + fadeOut(animationSpec = tween(durationMillis = 180))
                                            )
                                    } else {
                                        (slideInVertically(
                                            animationSpec = tween(durationMillis = 300, delayMillis = 30, easing = FastOutSlowInEasing),
                                            initialOffsetY = { h -> -h / 2 }
                                        ) + fadeIn(animationSpec = tween(durationMillis = 260, delayMillis = 30)))
                                            .togetherWith(
                                                slideOutVertically(
                                                    animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
                                                    targetOffsetY = { h -> h / 2 }
                                                ) + fadeOut(animationSpec = tween(durationMillis = 180))
                                            )
                                    }
                                },
                                label = "MobilePortraitTitleTransition",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 2.dp)
                            ) { track ->
                                val song = if (currentSong?.id == track.song?.id) (currentSong ?: track.song) else (track.song ?: currentSong)
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = song?.title ?: "No Song Playing",
                                        fontSize = 21.sp,
                                        fontWeight = FontWeight.Black,
                                        color = primaryTextColor,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    val albumStr = song?.album.orEmpty()
                                    val artistStr = song?.artist ?: "Select a track to start"
                                    Text(
                                        text = if (albumStr.isNotBlank() && albumStr != song?.title) "$artistStr • $albumStr" else artistStr,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    // ── Portrait Action Bar (Mode, Queue, Radio, Lyrics) - Hidden when lyrics expanded ──
                    AnimatedVisibility(
                        visible = !isLyricsExpanded,
                        enter = fadeIn(animationSpec = tween(220)) + expandVertically(animationSpec = tween(250)),
                        exit = fadeOut(animationSpec = tween(180)) + shrinkVertically(animationSpec = tween(200))
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(10.dp))
                            MobilePlayerActionBar(
                                viewModel = viewModel,
                                currentSong = currentSong,
                                isFavorite = isFavorite,
                                isLyricsExpanded = isLyricsExpanded,
                                isRadioActive = isRadioActive,
                                isRadioLoading = isRadioLoading,
                                currentDownloadStatus = currentDownloadStatus,
                                controlInactiveColor = controlInactiveColor,
                                isNightDimmerActive = isNightDimmerActive,
                                isDark = isDark,
                                onToggleLyrics = { isLyricsExpanded = it },
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
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
                            val landscapeStageHeight by animateDpAsState(
                                targetValue = if (isLyricsExpanded) 380.dp else 160.dp,
                                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                                label = "LandscapeStageHeight"
                            )

                            Row(
                                modifier = Modifier
                                    .widthIn(max = if (isLyricsExpanded) 840.dp else 700.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Start
                            ) {
                                val landscapeArtworkSize by animateDpAsState(
                                    targetValue = if (isLyricsExpanded) 0.dp else 160.dp,
                                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                                    label = "DriveHudArtworkSize"
                                )

                                // Hero Album Artwork with automotive horizontal slide transition
                                Box(
                                    modifier = Modifier
                                        .size(landscapeArtworkSize)
                                        .background(
                                            color = if (isNightDimmerActive) Color(0xFF0D1117)
                                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                            shape = if (isLyricsExpanded) MinimizedArtworkShape else BigArtworkShape
                                        )
                                        .clip(if (isLyricsExpanded) MinimizedArtworkShape else BigArtworkShape)
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
                                        label = "DriveHudArtworkTransition"
                                    ) { track ->
                                        val song = if (currentSong?.id == track.song?.id) (currentSong ?: track.song) else (track.song ?: currentSong)
                                        if (song != null) {
                                            AlbumArtImage(
                                                path = song.path,
                                                artworkUrl = song.artworkUrl,
                                                size = landscapeArtworkSize,
                                                borderRadius = if (isLyricsExpanded) 12.dp else 20.dp,
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
                                                    modifier = Modifier.size(if (isLyricsExpanded) 40.dp else 68.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(if (isLyricsExpanded) 0.dp else 24.dp))

                                // Content Area: Song Info or Synced Lyrics (Expanded to 340.dp when lyrics active)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(landscapeStageHeight),
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
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clip(BigArtworkShape)
                                                    .border(
                                                        width = if (!isDark) 1.dp else 0.dp,
                                                        color = if (!isDark) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f) else Color.Transparent,
                                                        shape = BigArtworkShape
                                                    )
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
                                                val song = if (currentSong?.id == track.song?.id) (currentSong ?: track.song) else (track.song ?: currentSong)
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

            // ── Bottom Row: Seeker Bar and Playback Controls (Material 3 standard) ──
            val bottomCardBg = when {
                isVideoActive -> Color(0xD90A0E17)
                isNightDimmerActive -> Color(0xFF090D13)
                else -> MaterialTheme.colorScheme.surfaceContainer
            }
            val bottomCardBorder = when {
                isNightDimmerActive -> Color(0xFF141D2A)
                else -> MaterialTheme.colorScheme.outlineVariant
            }

            // ── Action Bar: landscape/car only (hidden when lyrics expanded) ──
            if (!isPortrait) {
                AnimatedVisibility(
                    visible = !isLyricsExpanded,
                    enter = fadeIn(animationSpec = tween(220)) + expandVertically(animationSpec = tween(250)),
                    exit = fadeOut(animationSpec = tween(180)) + shrinkVertically(animationSpec = tween(200))
                ) {
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
                        isLyricsExpanded = isLyricsExpanded,
                        onToggleLyrics = { isLyricsExpanded = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 0.dp)
                    )
                }
            }

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
                        // Full-width Seeker Bar in portrait (no timestamps)
                        DriveHudSeekBar(
                            viewModel = viewModel,
                            showTimeLabels = false,
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

