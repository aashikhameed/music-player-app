package com.aashik.music.ui

import android.content.res.Configuration
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Folder
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import com.aashik.music.model.NavigatingTrack
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
    val isStreamPlaying = currentSong?.isStream() == true
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
                            color = if (isNightDimmerActive) NightDimmerAmber else MaterialTheme.colorScheme.onSurface
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
            }

            if (isPortrait) {
                MobileDriveHudStage(
                    viewModel = viewModel,
                    currentSong = currentSong,
                    isPlaying = isPlaying,
                    isFavorite = isFavorite,
                    navigatingTrack = navigatingTrack,
                    isRadioActive = isRadioActive,
                    isRadioLoading = isRadioLoading,
                    currentDownloadStatus = currentDownloadStatus,
                    upcomingSongs = upcomingSongs,
                    isDark = isDark,
                    isNightDimmerActive = isNightDimmerActive,
                    primaryTextColor = primaryTextColor,
                    controlInactiveColor = controlInactiveColor,
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
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    val playVideoForStreams by viewModel.playVideoForStreams.collectAsState()
                    val isYouTubeTrack = currentSong?.isYouTube() == true
                    val isVideoActive = playVideoForStreams && isYouTubeTrack

                    if (isVideoActive) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(190.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            VideoPlayerView(
                                videoId = currentSong?.getYouTubeVideoId(),
                                videoUrl = currentSong?.path,
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .aspectRatio(16f / 9f)
                            )
                        }
                    } else {
                        // Row: Hero Album Artwork on Left, Song Title & Artist next to it on Right
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
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

                            Spacer(modifier = Modifier.width(22.dp))

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
                                modifier = Modifier.weight(1f, fill = false).widthIn(max = 500.dp)
                            ) { track ->
                                val song = track.song
                                Column(
                                    horizontalAlignment = Alignment.Start,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    if (isStreamPlaying || isRadioActive) {
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    color = Color(0xFF00E676).copy(alpha = 0.16f),
                                                    shape = DriverBadgeShape
                                                )
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(6.dp)
                                                        .background(Color(0xFF00E676), shape = CircleShape)
                                                )
                                                Text(
                                                    text = if (isRadioActive) "LIVE COMMUNITY RADIO" else "ONLINE STREAM",
                                                    fontSize = 9.5.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = Color(0xFF00E676),
                                                    letterSpacing = 0.5.sp
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                    }

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
                                    // Single Next Song Card (shows only the next queued song)
                                    HudSingleNextSongCard(
                                        nextSong = upcomingSongs.firstOrNull(),
                                        isRadioActive = isRadioActive,
                                        isDark = isDark,
                                        isNightDimmer = isNightDimmerActive,
                                        onPlayNext = { s -> viewModel.play(s, direction = 1) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── Bottom Row: Seeker Bar and Playback Controls ──
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

            // Unified Action Bar (Fav, Download, Radio, EQ Presets, Queue)
            DriveHudActionBar(
                viewModel = viewModel,
                currentSong = currentSong,
                isFavorite = isFavorite,
                isRadioActive = isRadioActive,
                isRadioLoading = isRadioLoading,
                currentDownloadStatus = currentDownloadStatus,
                isNightDimmerActive = isNightDimmerActive,
                isDark = isDark,
                controlInactiveColor = controlInactiveColor
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
 * Unified Quick Action Bar for Drive HUD (both Car Landscape and Mobile Portrait).
 * Displays in clean horizontal alignment:
 * [Favorite] -> [Download (if stream)] -> [Radio (if stream)] -> [EQ Presets: Flat, Acoustic, Vocal, Bass, Rock] -> [Queue].
 * Uses zero dynamic allocations (all precomputed constants) and Mali GPU friendly flat colors.
 */
@Composable
private fun DriveHudActionBar(
    viewModel: MusicViewModel,
    currentSong: Song?,
    isFavorite: Boolean,
    isRadioActive: Boolean,
    isRadioLoading: Boolean,
    currentDownloadStatus: DownloadStatus,
    isNightDimmerActive: Boolean,
    isDark: Boolean,
    controlInactiveColor: Color,
    modifier: Modifier = Modifier
) {
    val activePreset by viewModel.equalizerPreset.collectAsState()
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

         // 1. Queue Drawer Button
        Box(
            modifier = Modifier
                .size(40.dp)
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
                modifier = Modifier.size(22.dp)
            )
        }

        // 2. Favorite Button
        Box(
            modifier = Modifier
                .size(40.dp)
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
                modifier = Modifier.size(22.dp)
            )
        }
        

        // 3. Download Button (moved next to Fav if streamable)
        if (isStreamDownloadable) {
            val isDownloaded = currentDownloadStatus is DownloadStatus.Downloaded
            val isDownloading = currentDownloadStatus is DownloadStatus.Downloading
            Box(
                modifier = Modifier
                    .height(36.dp)
                    .background(
                        color = when {
                            isDownloaded -> Color(0xFF00E676).copy(alpha = 0.20f)
                            isDownloading -> MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
                            else -> actionButtonBg
                        },
                        shape = NightDimmerPillShape
                    )
                    .clickable {
                        currentSong?.let { viewModel.downloadSong(it) }
                    }
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    when (val status = currentDownloadStatus) {
                        is DownloadStatus.Downloading -> {
                            CircularProgressIndicator(
                                progress = { status.progress },
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${(status.progress * 100).toInt()}%",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        is DownloadStatus.Downloaded -> {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = "Saved Offline",
                                tint = Color(0xFF00E676),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Saved",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00E676)
                            )
                        }
                        else -> {
                            Icon(
                                imageVector = Icons.Rounded.Download,
                                contentDescription = "Download Track",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Download",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // 4. Start Radio Button (moved next to Fav & Download if streamable)
        if (isStream) {
            Box(
                modifier = Modifier
                    .height(36.dp)
                    .background(
                        color = if (isRadioActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f) else actionButtonBg,
                        shape = NightDimmerPillShape
                    )
                    .clickable { viewModel.startRadioForCurrentSong() }
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    if (isRadioLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Starting...",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Podcasts,
                            contentDescription = "Start Radio",
                            tint = if (isRadioActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (isRadioActive) "Radio Active" else "Radio",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isRadioActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
        

        //5. Equalizer Presets Row (Flat, Acoustic, Vocal, Bass, Rock)
        Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
        

            val presets = remember {
                listOf(0 to "Flat", 1 to "Acoustic", 2 to "Vocal", 3 to "Bass", 4 to "Rock")
            }
            for ((id, name) in presets) {
                val isSelected = activePreset == id
                Box(
                    modifier = Modifier
                        .height(32.dp)
                        .background(
                            color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else actionButtonBg,
                            shape = EqPresetChipShape
                        )
                        .clickable { viewModel.setEqualizerPreset(id) }
                        .padding(horizontal = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = name,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (activePreset == 5) {
                Box(
                    modifier = Modifier
                        .height(32.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                            shape = EqPresetChipShape
                        )
                        .clickable { viewModel.openEqualizer() }
                        .padding(horizontal = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Custom",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

                // EQ Dialog Button
            Box(
                modifier = Modifier
                    .height(32.dp)
                    .background(color = actionButtonBg, shape = EqPresetChipShape)
                    .clickable { viewModel.openEqualizer() }
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "EQ",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
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

    CustomHorizontalSeekBar(
        progress = progress,
        onProgressChanged = { viewModel.seekToFraction(it) },
        isPlaying = isPlaying,
        currentPositionMs = positionMs,
        durationMs = durationMs,
        waveSeed = 42,
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

/**
 * Distraction-Free Single Next Song Card for Drive HUD.
 * Displays strictly the single next song in the playback queue with one-tap skip.
 */
@Composable
private fun HudSingleNextSongCard(
    nextSong: Song?,
    isRadioActive: Boolean,
    isDark: Boolean,
    isNightDimmer: Boolean,
    onPlayNext: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    val cardBg = when {
        isNightDimmer -> Color(0xFF101620)
        isDark -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.40f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.70f)
    }
    val cardBorder = when {
        isNightDimmer -> Color(0xFF192330)
        isDark -> Color(0xFF283445)
        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
    }
    val primaryText = if (isNightDimmer) Color(0xFFE2E8F0) else if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
    val secondaryText = if (isNightDimmer) Color(0xFF8B98A9) else if (isDark) Color.White.copy(alpha = 0.65f) else MaterialTheme.colorScheme.onSurfaceVariant
    val accentColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .fillMaxWidth(0.85f)
            .widthIn(max = 520.dp)
            .height(44.dp)
            .background(color = cardBg, shape = UpNextQueueCardShape)
            .border(width = 0.8.dp, color = cardBorder, shape = UpNextQueueCardShape)
            .then(
                if (nextSong != null) {
                    Modifier.clickable { onPlayNext(nextSong) }
                } else Modifier
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        if (nextSong == null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "End of queue • Start Radio to auto-queue similar tracks",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = secondaryText,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // UP NEXT Badge
                    Box(
                        modifier = Modifier
                            .background(
                                color = accentColor.copy(alpha = 0.16f),
                                shape = DriverBadgeShape
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (isRadioActive) Icons.Rounded.Podcasts else Icons.AutoMirrored.Rounded.QueueMusic,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "UP NEXT",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = accentColor,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    // Thumbnail
                    AlbumArtImage(
                        path = nextSong.path,
                        artworkUrl = nextSong.artworkUrl,
                        size = 28.dp,
                        borderRadius = 6.dp,
                        isPlaying = false
                    )

                    // Next Song Title & Artist
                    Text(
                        text = nextSong.title,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = "• ${nextSong.artist}",
                        fontSize = 11.sp,
                        color = secondaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun MobileDriveHudStage(
    viewModel: MusicViewModel,
    currentSong: Song?,
    isPlaying: Boolean,
    isFavorite: Boolean,
    navigatingTrack: NavigatingTrack,
    isRadioActive: Boolean,
    isRadioLoading: Boolean,
    currentDownloadStatus: DownloadStatus,
    upcomingSongs: List<Song>,
    isDark: Boolean,
    isNightDimmerActive: Boolean,
    primaryTextColor: Color,
    controlInactiveColor: Color,
    modifier: Modifier = Modifier
) {
    val playVideoForStreams by viewModel.playVideoForStreams.collectAsState()
    val isYouTubeTrack = currentSong?.isYouTube() == true
    val isVideoActive = playVideoForStreams && isYouTubeTrack

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        if (isVideoActive) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(210.dp),
                contentAlignment = Alignment.Center
            ) {
                VideoPlayerView(
                    videoId = currentSong?.getYouTubeVideoId(),
                    videoUrl = currentSong?.path,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                )
            }
        } else {
            // Hero Album Artwork with Apple CarPlay horizontal slide transition
            Box(
                modifier = Modifier
                    .size(190.dp)
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
                    label = "MobileCarPlaySlideTransition"
                ) { navState ->
                    val song = navState.song
                    if (song != null) {
                        AlbumArtImage(
                            path = song.path,
                            size = 190.dp,
                            borderRadius = 20.dp,
                            isPlaying = isPlaying,
                            isHighRes = true,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            // Song Info Typography (Title, Artist, Album)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                if (currentSong?.isStream() == true) {
                    Surface(
                        shape = DriverBadgeShape,
                        color = Color(0xFF00E676).copy(alpha = 0.15f),
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        Text(
                            text = if (isRadioActive) "LIVE COMMUNITY RADIO" else "ONLINE STREAM",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF00E676),
                            letterSpacing = 0.5.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = currentSong?.title ?: "No Song Playing",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = primaryTextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )

                val albumStr = currentSong?.album.orEmpty()
                val artistStr = currentSong?.artist ?: "Select a track to start"
                Text(
                    text = if (albumStr.isNotBlank() && albumStr != currentSong?.title) "$artistStr • $albumStr" else artistStr,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        // Single Next Song Card
        HudSingleNextSongCard(
            nextSong = upcomingSongs.firstOrNull(),
            isRadioActive = isRadioActive,
            isDark = isDark,
            isNightDimmer = isNightDimmerActive,
            onPlayNext = { song -> viewModel.play(song, direction = 1) }
        )

        // Quick Actions Row: Favorite, Download, Radio, EQ Presets, Queue
        DriveHudActionBar(
            viewModel = viewModel,
            currentSong = currentSong,
            isFavorite = isFavorite,
            isRadioActive = isRadioActive,
            isRadioLoading = isRadioLoading,
            currentDownloadStatus = currentDownloadStatus,
            isNightDimmerActive = isNightDimmerActive,
            isDark = isDark,
            controlInactiveColor = controlInactiveColor,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
        )
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
