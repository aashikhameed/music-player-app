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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aashik.music.service.DownloadStatus
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
    val isStreamDownloadable = isStreamPlaying && (currentSong?.duration ?: 0) > 0
    val isRadioLoading by viewModel.isRadioLoading.collectAsState()
    val activeRadioStationName by viewModel.activeRadioStationName.collectAsState()
    val isRadioActive = isRadioLoading || activeRadioStationName != null
    val downloadStates by viewModel.downloadStates.collectAsState()
    val currentDownloadStatus = currentSong?.id?.let { downloadStates[it] } ?: DownloadStatus.Idle

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

                    // HUD Start Radio Pill
                    Row(
                        modifier = Modifier
                            .height(36.dp)
                            .background(
                                color = if (isRadioActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                                        else if (isNightDimmerActive) Color(0xFF161C24)
                                        else if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                        else MaterialTheme.colorScheme.surfaceVariant,
                                shape = NightDimmerPillShape
                            )
                            .clickable { viewModel.startRadioForCurrentSong() }
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (isRadioLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Starting...",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Podcasts,
                                contentDescription = "Start Radio",
                                tint = if (isRadioActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = if (activeRadioStationName != null) "Radio Active" else "Start Radio",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isRadioActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // HUD Download Pill (for streamable tracks)
                    if (isStreamDownloadable) {
                        Row(
                            modifier = Modifier
                                .height(36.dp)
                                .background(
                                    color = when (currentDownloadStatus) {
                                        is DownloadStatus.Downloaded -> Color(0xFF00E676).copy(alpha = 0.18f)
                                        is DownloadStatus.Downloading -> MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                        else -> if (isNightDimmerActive) Color(0xFF161C24)
                                                else if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                                else MaterialTheme.colorScheme.surfaceVariant
                                    },
                                    shape = NightDimmerPillShape
                                )
                                .clickable {
                                    currentSong?.let { viewModel.downloadSong(it) }
                                }
                                .padding(horizontal = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            when (currentDownloadStatus) {
                                is DownloadStatus.Downloading -> {
                                    CircularProgressIndicator(
                                        progress = { currentDownloadStatus.progress },
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "${(currentDownloadStatus.progress * 100).toInt()}%",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                is DownloadStatus.Downloaded -> {
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = "Saved Offline",
                                        tint = Color(0xFF00E676),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = "Saved",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF00E676)
                                    )
                                }
                                else -> {
                                    Icon(
                                        imageVector = Icons.Rounded.Download,
                                        contentDescription = "Download Track",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = "Download",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
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

            // ── Center Stage: 3-Column Layout (Left Actions | Hero Artwork & Track Info | Right EQ Presets) ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left Column: Quick Actions (Favorite & Download)
                Column(
                    modifier = Modifier.width(76.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Favorite Action Button
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                color = if (isFavorite) FavoriteRedColor.copy(alpha = 0.22f)
                                        else if (isNightDimmerActive) Color(0xFF131922)
                                        else if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                        else MaterialTheme.colorScheme.surfaceVariant,
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
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    // Download Action Button (for streamable tracks)
                    if (isStreamDownloadable) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    color = when (currentDownloadStatus) {
                                        is DownloadStatus.Downloaded -> Color(0xFF00E676).copy(alpha = 0.20f)
                                        is DownloadStatus.Downloading -> MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
                                        else -> if (isNightDimmerActive) Color(0xFF131922)
                                                else if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                                else MaterialTheme.colorScheme.surfaceVariant
                                    },
                                    shape = CircleShape
                                )
                                .clickable {
                                    currentSong?.let { viewModel.downloadSong(it) }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            when (currentDownloadStatus) {
                                is DownloadStatus.Downloading -> {
                                    Box(contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(
                                            progress = { currentDownloadStatus.progress },
                                            modifier = Modifier.size(24.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "${(currentDownloadStatus.progress * 100).toInt()}%",
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                is DownloadStatus.Downloaded -> {
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = "Downloaded Offline",
                                        tint = Color(0xFF00E676),
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                                else -> {
                                    Icon(
                                        imageVector = Icons.Rounded.Download,
                                        contentDescription = "Download Track",
                                        tint = controlInactiveColor,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Center Column: Hero Album Artwork, Song Title, Artist & Next Song in Queue
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Hero Album Artwork with Apple CarPlay horizontal slide transition
                    Box(
                        modifier = Modifier
                            .size(205.dp)
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
                                    size = 205.dp,
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

                    Spacer(modifier = Modifier.height(8.dp))

                    // Current Song Title (Prominent glanceability)
                    Text(
                        text = currentSong?.title ?: "No Song Playing",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = primaryTextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    // Current Song Artist & Album
                    val albumStr = currentSong?.album.orEmpty()
                    val artistStr = currentSong?.artist ?: "Select a track to start"
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
                            if (albumStr.isNotBlank()) {
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
                        fontSize = 13.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Next Song in Queue Preview Pill
                    val nextSong = viewModel.getNextSong()
                    val nextPillBg = if (isNightDimmerActive) Color(0xFF141A22)
                                     else if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                     else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f)
                    Row(
                        modifier = Modifier
                            .background(color = nextPillBg, shape = UpNextBadgeShape)
                            .clickable { viewModel.playNextSong() }
                            .padding(horizontal = 12.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SkipNext,
                            contentDescription = "Next Track",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "NEXT: ",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (nextSong != null) "${nextSong.title} • ${nextSong.artist}"
                                   else if (isStreamPlaying) "Live Community Stream"
                                   else "End of Queue",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = primaryTextColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 280.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Swipe ← / → anywhere to change track • Double-tap to pause",
                        fontSize = 10.5.sp,
                        color = hintTextColor
                    )
                }

                // Right Column: Side Equalizer Panel
                SideEqualizerPanel(
                    viewModel = viewModel,
                    isDark = isDark,
                    isNightDimmer = isNightDimmerActive,
                    modifier = Modifier.width(76.dp)
                )
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

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color = bottomCardBg, shape = BottomMusicCardShape)
                    .border(width = 0.8.dp, color = bottomCardBorder, shape = BottomMusicCardShape)
                    .padding(horizontal = 14.dp, vertical = 8.dp)
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

                    // Centered Driving Playback Controls Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Shuffle Mode Toggle
                            val isShuffleOn by viewModel.isShuffleOn.collectAsState()
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

                            // Start Radio Button
                            IconButton(
                                onClick = { viewModel.startRadioForCurrentSong() },
                                modifier = Modifier.size(44.dp)
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
                                        modifier = Modifier.size(24.dp)
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
 * Isolated leaf composable for the Side Equalizer Panel.
 * Positioned on the right side of the screen in Drive HUD.
 * Renders an animated Canvas spectrum EQ capsule and vertical preset chips
 * (Flat, Acoustic, Vocal, Bass, Rock, Custom) allowing 1-tap preset switching.
 */
@Composable
private fun SideEqualizerPanel(
    viewModel: MusicViewModel,
    isDark: Boolean,
    isNightDimmer: Boolean,
    modifier: Modifier = Modifier
) {
    val isPlaying by viewModel.isPlaying.collectAsState()
    val activePreset by viewModel.equalizerPreset.collectAsState()

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
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary

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

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Equalizer Icon / Visualizer Capsule — clicking opens the full Equalizer Screen
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .background(color = badgeBg, shape = TopVisualizerCapsuleShape)
                .clickable { viewModel.openEqualizer() }
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Canvas(modifier = Modifier.size(width = 16.dp, height = 13.dp)) {
                val barCount = 4
                val barWidth = 2.dp.toPx()
                val totalWidth = size.width
                val totalHeight = size.height
                val spacing = (totalWidth - (barWidth * barCount)) / (barCount - 1)
                val cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())

                for (i in 0 until barCount) {
                    val heightFactor = if (isPlaying) {
                        val rawSin = kotlin.math.sin(phase.toDouble() + i * 1.3).toFloat()
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

            Spacer(modifier = Modifier.width(4.dp))

            Text(
                text = "EQ",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = primaryColor
            )
        }

        // Preset Chips: Flat (0), Acoustic (1), Vocal (2), Bass (3), Rock (4)
        val presets = remember {
            listOf(
                0 to "Flat",
                1 to "Acoustic",
                2 to "Vocal",
                3 to "Bass",
                4 to "Rock"
            )
        }

        for ((presetIdx, name) in presets) {
            val isSelected = activePreset == presetIdx
            val chipBg = if (isSelected) primaryColor else badgeBg
            val chipTextColor = if (isSelected) onPrimaryColor else textColor

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp)
                    .background(color = chipBg, shape = EqPresetChipShape)
                    .clickable { viewModel.setEqualizerPreset(presetIdx) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = name,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                    color = chipTextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // If Custom is active (preset == 5), show Custom chip
        if (activePreset == 5) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp)
                    .background(color = primaryColor, shape = EqPresetChipShape)
                    .clickable { viewModel.openEqualizer() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Custom",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = onPrimaryColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
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
