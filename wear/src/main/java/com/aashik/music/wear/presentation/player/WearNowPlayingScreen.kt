package com.aashik.music.wear.presentation.player

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Downloading
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.rounded.AvTimer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.Text
import com.aashik.music.wear.player.WearPlayerState
import com.aashik.music.wear.presentation.components.CircularProgressArc
import com.aashik.music.wear.presentation.components.LinearProgressCapsule
import com.aashik.music.wear.presentation.components.RotarySeekBubble
import com.aashik.music.wear.presentation.components.RotaryVolumeBubble
import com.aashik.music.wear.presentation.util.isScreenRound
import com.aashik.music.wear.theme.WearBackgroundBlack
import com.aashik.music.wear.theme.WearOnSurfaceDim
import com.aashik.music.wear.theme.WearPrimaryCyan
import com.aashik.music.wear.theme.WearSurfaceCard
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class RotaryMode {
    SEEK,
    VOLUME
}

private val SquareThumbnailShape = RoundedCornerShape(10.dp)

@Composable
fun WearNowPlayingScreen(
    state: WearPlayerState,
    onPlayPauseClicked: () -> Unit,
    onNextClicked: () -> Unit,
    onPreviousClicked: () -> Unit,
    onVolumeAdjusted: (Float) -> Unit,
    onSeekAdjusted: (Long) -> Unit = {},
    onLibraryClicked: () -> Unit,
    onQueueClicked: () -> Unit,
    onRadioClicked: (() -> Unit)? = null,
    onDownloadClicked: ((com.aashik.music.wear.model.WearSong) -> Unit)? = null,
    downloadStatus: com.aashik.music.wear.storage.WearDownloadStatus = com.aashik.music.wear.storage.WearDownloadStatus.Idle,
    isDownloaded: Boolean = false,
    modifier: Modifier = Modifier
) {
    val isRound = isScreenRound()
    val focusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()
    var rotaryMode by remember { mutableStateOf(RotaryMode.SEEK) }
    var showHudBubble by remember { mutableStateOf(false) }
    var lastSeekDeltaSec by remember { mutableIntStateOf(0) }
    var hideHudJob by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(WearBackgroundBlack)
            .focusRequester(focusRequester)
            .focusable()
            .onRotaryScrollEvent { event ->
                val isForward = event.verticalScrollPixels > 0
                if (rotaryMode == RotaryMode.SEEK) {
                    val stepMs = 5000L
                    val deltaMs = if (isForward) stepMs else -stepMs
                    lastSeekDeltaSec = if (isForward) 5 else -5
                    onSeekAdjusted(deltaMs)
                } else {
                    val deltaVol = if (isForward) 0.05f else -0.05f
                    onVolumeAdjusted(deltaVol)
                }

                showHudBubble = true
                hideHudJob?.cancel()
                hideHudJob = coroutineScope.launch {
                    delay(1400L)
                    showHudBubble = false
                    lastSeekDeltaSec = 0
                }
                true
            },
        contentAlignment = Alignment.Center
    ) {
        if (isRound) {
            // ─── CIRCULAR WEAR OS LAYOUT ──────────────────────────────────────────
            CircularProgressArc(progress = state.progress)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top: Rotary HUD Indicator (Seek or Volume)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (rotaryMode == RotaryMode.SEEK) {
                        RotarySeekBubble(
                            visible = showHudBubble,
                            positionMs = state.positionMs,
                            durationMs = state.durationMs,
                            deltaSec = lastSeekDeltaSec
                        )
                    } else {
                        RotaryVolumeBubble(visible = showHudBubble, volume = state.volume)
                    }
                }

                // Center: Artwork Thumbnail + Metadata
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(WearSurfaceCard),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = "Artwork",
                            tint = WearPrimaryCyan,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = state.currentSong?.title ?: "No Track",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = state.currentSong?.artist ?: "Tap Library to play",
                        color = WearOnSurfaceDim,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }

                // Controls Row: Prev (34dp), Play/Pause (48dp), Next (34dp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onPreviousClicked,
                        colors = ButtonDefaults.secondaryButtonColors(backgroundColor = WearSurfaceCard),
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = onPlayPauseClicked,
                        colors = ButtonDefaults.primaryButtonColors(
                            backgroundColor = WearPrimaryCyan,
                            contentColor = Color.Black
                        ),
                        modifier = Modifier.size(46.dp)
                    ) {
                        Icon(
                            imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (state.isPlaying) "Pause" else "Play",
                            tint = Color.Black,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = onNextClicked,
                        colors = ButtonDefaults.secondaryButtonColors(backgroundColor = WearSurfaceCard),
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Bottom: Volume, Queue, Radio & Download Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            rotaryMode = if (rotaryMode == RotaryMode.SEEK) RotaryMode.VOLUME else RotaryMode.SEEK
                            showHudBubble = true
                            hideHudJob?.cancel()
                            hideHudJob = coroutineScope.launch {
                                delay(1200L)
                                showHudBubble = false
                            }
                        },
                        colors = ButtonDefaults.secondaryButtonColors(backgroundColor = Color.Transparent),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (rotaryMode == RotaryMode.SEEK) Icons.Rounded.AvTimer else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = if (rotaryMode == RotaryMode.SEEK) "Rotary: Seek" else "Rotary: Volume",
                            tint = WearPrimaryCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    if (onRadioClicked != null) {
                        Button(
                            onClick = onRadioClicked,
                            colors = ButtonDefaults.secondaryButtonColors(backgroundColor = Color.Transparent),
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Radio,
                                contentDescription = "Radio",
                                tint = WearPrimaryCyan,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    if (onDownloadClicked != null && state.currentSong != null) {
                        val isStream = state.currentSong.path.startsWith("http://", true) ||
                                       state.currentSong.path.startsWith("https://", true)
                        if (isStream) {
                            Button(
                                onClick = { onDownloadClicked(state.currentSong) },
                                colors = ButtonDefaults.secondaryButtonColors(backgroundColor = Color.Transparent),
                                modifier = Modifier.size(28.dp)
                            ) {
                                when (downloadStatus) {
                                    is com.aashik.music.wear.storage.WearDownloadStatus.Downloading -> {
                                        Icon(
                                            imageVector = Icons.Default.Downloading,
                                            contentDescription = "Downloading",
                                            tint = com.aashik.music.wear.theme.WearSecondaryAmber,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    is com.aashik.music.wear.storage.WearDownloadStatus.Downloaded -> {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Downloaded",
                                            tint = Color(0xFF30D158),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    else -> {
                                        if (isDownloaded) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = "Downloaded",
                                                tint = Color(0xFF30D158),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.Download,
                                                contentDescription = "Download",
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Button(
                        onClick = onQueueClicked,
                        colors = ButtonDefaults.secondaryButtonColors(backgroundColor = Color.Transparent),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                            contentDescription = "Queue",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Button(
                        onClick = onLibraryClicked,
                        colors = ButtonDefaults.secondaryButtonColors(backgroundColor = Color.Transparent),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LibraryMusic,
                            contentDescription = "Library",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        } else {
            // ─── SQUARE WEAR OS LAYOUT ────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top: Linear Capsule Progress Bar + Timestamp readouts
                Column(modifier = Modifier.fillMaxWidth()) {
                    LinearProgressCapsule(progress = state.progress)
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatMs(state.positionMs),
                            fontSize = 9.sp,
                            color = WearOnSurfaceDim
                        )
                        Text(
                            text = formatMs(state.durationMs),
                            fontSize = 9.sp,
                            color = WearOnSurfaceDim
                        )
                    }
                }

                // Center: Square-Rounded Thumbnail + Title & Artist
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(SquareThumbnailShape)
                            .background(WearSurfaceCard),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = "Artwork",
                            tint = WearPrimaryCyan,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = state.currentSong?.title ?: "No Track Selected",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = state.currentSong?.artist ?: "Tap Library to select",
                            color = WearOnSurfaceDim,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Controls Row: Prev, Play/Pause, Next
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onPreviousClicked,
                        colors = ButtonDefaults.secondaryButtonColors(backgroundColor = WearSurfaceCard),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Button(
                        onClick = onPlayPauseClicked,
                        colors = ButtonDefaults.primaryButtonColors(
                            backgroundColor = WearPrimaryCyan,
                            contentColor = Color.Black
                        ),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (state.isPlaying) "Pause" else "Play",
                            tint = Color.Black,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Button(
                        onClick = onNextClicked,
                        colors = ButtonDefaults.secondaryButtonColors(backgroundColor = WearSurfaceCard),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Bottom: Rotary Mode, Queue & Library Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Button(
                            onClick = {
                                rotaryMode = if (rotaryMode == RotaryMode.SEEK) RotaryMode.VOLUME else RotaryMode.SEEK
                                showHudBubble = true
                                hideHudJob?.cancel()
                                hideHudJob = coroutineScope.launch {
                                    delay(1200L)
                                    showHudBubble = false
                                }
                            },
                            colors = ButtonDefaults.secondaryButtonColors(backgroundColor = WearSurfaceCard),
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = if (rotaryMode == RotaryMode.SEEK) Icons.Rounded.AvTimer else Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = if (rotaryMode == RotaryMode.SEEK) "Rotary: Seek" else "Rotary: Volume",
                                tint = WearPrimaryCyan,
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        if (rotaryMode == RotaryMode.SEEK) {
                            RotarySeekBubble(
                                visible = showHudBubble,
                                positionMs = state.positionMs,
                                durationMs = state.durationMs,
                                deltaSec = lastSeekDeltaSec
                            )
                        } else {
                            RotaryVolumeBubble(visible = showHudBubble, volume = state.volume)
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (onRadioClicked != null) {
                            Button(
                                onClick = onRadioClicked,
                                colors = ButtonDefaults.secondaryButtonColors(backgroundColor = WearSurfaceCard),
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Radio,
                                    contentDescription = "Radio",
                                    tint = WearPrimaryCyan,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                        }

                        if (onDownloadClicked != null && state.currentSong != null) {
                            val isStream = state.currentSong.path.startsWith("http://", true) ||
                                           state.currentSong.path.startsWith("https://", true)
                            if (isStream) {
                                Button(
                                    onClick = { onDownloadClicked(state.currentSong) },
                                    colors = ButtonDefaults.secondaryButtonColors(backgroundColor = WearSurfaceCard),
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    when (downloadStatus) {
                                        is com.aashik.music.wear.storage.WearDownloadStatus.Downloading -> {
                                            Icon(
                                                imageVector = Icons.Default.Downloading,
                                                contentDescription = "Downloading",
                                                tint = com.aashik.music.wear.theme.WearSecondaryAmber,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                        is com.aashik.music.wear.storage.WearDownloadStatus.Downloaded -> {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = "Downloaded",
                                                tint = Color(0xFF30D158),
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                        else -> {
                                            if (isDownloaded) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = "Downloaded",
                                                    tint = Color(0xFF30D158),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Default.Download,
                                                    contentDescription = "Download",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                        }

                        Button(
                            onClick = onQueueClicked,
                            colors = ButtonDefaults.secondaryButtonColors(backgroundColor = WearSurfaceCard),
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                contentDescription = "Queue",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        Button(
                            onClick = onLibraryClicked,
                            colors = ButtonDefaults.secondaryButtonColors(backgroundColor = WearSurfaceCard),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LibraryMusic,
                                    contentDescription = "Library",
                                    tint = WearPrimaryCyan,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "Library", fontSize = 11.sp, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatMs(ms: Long): String {
    if (ms <= 0) return "0:00"
    val totalSecs = ms / 1000
    val mins = totalSecs / 60
    val secs = totalSecs % 60
    return String.format(java.util.Locale.US, "%d:%02d", mins, secs)
}
