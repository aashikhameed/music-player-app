package com.aashik.music.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.ShuffleOn
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aashik.music.theme.AppGradients
import com.aashik.music.viewmodel.MusicViewModel
import java.util.Locale

/**
 * Premium Automotive Media Dock — Blaupunkt Santa Rosa 985 (1280x720 @ 160 dpi)
 *
 * 3 Floating Islands (No bottom bar background):
 *   1. [Combined Song Info + Seeker] (weight 0.60f)
 *   2. [Player Controls: Shuffle, Prev, Play/Pause, Next] (weight 0.24f)
 *   3. [Status Island: Theme Toggle | 12-hr Clock] (wrapContentWidth)
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AndroidAutoBottomBar(
    viewModel: MusicViewModel,
    @Suppress("UNUSED_PARAMETER") isSearchVisible: Boolean = false,
    @Suppress("UNUSED_PARAMETER") onToggleSearch: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isPlaying   by viewModel.isPlaying.collectAsState()
    val isShuffleOn by viewModel.isShuffleOn.collectAsState()
    val currentSong by viewModel.currentSong.collectAsState()
    val progress    by viewModel.currentProgressFlow.collectAsState(initial = 0f)
    val positionMs  by viewModel.positionFlow.collectAsState()
    val durationMs  by viewModel.durationFlow.collectAsState()
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()

    // 12-hour clock — refreshes every second
    var currentTime by remember { mutableStateOf(getCurrentTimeWithAmPm()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = getCurrentTimeWithAmPm()
            kotlinx.coroutines.delay(1000)
        }
    }

    val isDark          = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val capsuleGrad     = AppGradients.capsule(isActive = false)
    val borderBrush     = AppGradients.border(isActive = false)
    val primaryGradient = AppGradients.primaryButton()
    val accentColor     = if (isDark) Color(0xFF00E5FF) else Color(0xFF0055FF)

    // Breathing halo animation for the play button
    val infiniteTransition = rememberInfiniteTransition(label = "PlayHalo")
    val haloScale by if (isPlaying) {
        infiniteTransition.animateFloat(
            initialValue = 1.0f,
            targetValue  = 1.20f,
            animationSpec = infiniteRepeatable(
                animation  = tween(900, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "haloScale"
        )
    } else {
        remember { mutableStateOf(1f) }
    }

    val cardShape = RoundedCornerShape(16.dp)

    // Floating dock with transparent background (no bottom bar background stripe)
    Surface(
        color           = Color.Transparent,
        shadowElevation = 0.dp,
        tonalElevation  = 0.dp,
        modifier        = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .padding(horizontal = 10.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {

            // ─── A: Combined Song Info & Seeker ──────────────────────────────────
            Row(
                modifier = Modifier
                    .weight(0.60f)
                    .fillMaxHeight()
                    .clip(cardShape)
                    .background(brush = capsuleGrad, shape = cardShape)
                    .border(BorderStroke(0.8.dp, borderBrush), cardShape)
                    .clickable { viewModel.triggerScrollToCurrentSong() }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Album art with ambient glow halo when playing
                Box(contentAlignment = Alignment.Center) {
                    if (isPlaying) {
                        Box(
                            modifier = Modifier
                                .size(58.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.radialGradient(
                                        listOf(accentColor.copy(alpha = 0.4f), Color.Transparent)
                                    )
                                )
                        )
                    }
                    if (currentSong != null) {
                        AlbumArtImage(
                            path         = currentSong?.path.orEmpty(),
                            size         = 50.dp,
                            borderRadius = 10.dp,
                            isPlaying    = isPlaying
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector        = Icons.Rounded.MusicNote,
                                contentDescription = null,
                                tint               = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                modifier           = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                // Combined Track Info & Squiggly Seeker
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.Center
                ) {
                    // Top row: Title, bullet, Artist, format badge, and time counter
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier              = Modifier.weight(1f, fill = false),
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Pulsing playback status dot
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isPlaying) Color(0xFF00E676)
                                        else if (isDark) Color(0xFF475569)
                                        else Color(0xFFB0BEC5)
                                    )
                            )
                            Text(
                                text     = currentSong?.title ?: "No media playing",
                                style    = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight    = FontWeight.Bold,
                                    fontSize      = 14.sp,
                                    letterSpacing = 0.15.sp
                                ),
                                color    = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .weight(1f, fill = false)
                                    .then(if (isPlaying) Modifier.basicMarquee() else Modifier)
                            )
                            Text(
                                text     = "•",
                                color    = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                                fontSize = 11.sp
                            )
                            Text(
                                text     = currentSong?.artist ?: "Select a track",
                                style    = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            // Format badge
                            val ext = currentSong?.path
                                ?.substringAfterLast('.', "")
                                ?.uppercase().orEmpty().take(4)
                            if (ext.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(
                                            MaterialTheme.colorScheme.primary
                                                .copy(alpha = if (isDark) 0.22f else 0.12f)
                                        )
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text       = ext,
                                        fontSize   = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color      = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        // Time display: "00:02 / 00:06"
                        Text(
                            text = "${formatTime(positionMs)} / ${formatTime(durationMs)}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize      = 11.sp,
                                fontWeight    = FontWeight.SemiBold,
                                letterSpacing = 0.3.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // Squiggly wave seekbar
                    CustomHorizontalSeekBar(
                        progress          = progress,
                        onProgressChanged = { viewModel.seekToFraction(it) },
                        isPlaying         = isPlaying,
                        currentPositionMs = positionMs,
                        durationMs        = durationMs,
                        showTimeLabels    = false,
                        modifier          = Modifier.fillMaxWidth()
                    )
                }
            }

            // ─── B: Playback Controls (No horizontal padding, spacious buttons) ──
            Row(
                modifier = Modifier
                    .weight(0.24f)
                    .fillMaxHeight()
                    .clip(cardShape)
                    .background(brush = capsuleGrad, shape = cardShape)
                    .border(BorderStroke(0.8.dp, borderBrush), cardShape),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally)
            ) {
                // Shuffle
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(
                            if (isShuffleOn)
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                            else Color.Transparent
                        )
                        .clickable { viewModel.toggleShuffle() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector        = if (isShuffleOn) Icons.Rounded.ShuffleOn else Icons.Rounded.Shuffle,
                        contentDescription = "Shuffle",
                        tint               = if (isShuffleOn) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                        modifier           = Modifier.size(18.dp)
                    )
                }

                // Previous
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            MaterialTheme.colorScheme.onSurface.copy(
                                alpha = if (isDark) 0.09f else 0.06f
                            )
                        )
                        .clickable { viewModel.playPreviousSong() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector        = Icons.Rounded.SkipPrevious,
                        contentDescription = "Previous",
                        tint               = MaterialTheme.colorScheme.onSurface,
                        modifier           = Modifier.size(26.dp)
                    )
                }

                // Hero Play/Pause with breathing halo
                Box(
                    modifier         = Modifier.size(64.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isPlaying) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .scale(haloScale)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(accentColor.copy(alpha = 0.30f), Color.Transparent)
                                    )
                                )
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(brush = primaryGradient, shape = CircleShape)
                            .clickable { viewModel.togglePlayPause() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector        = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint               = Color.White,
                            modifier           = Modifier.size(30.dp)
                        )
                    }
                }

                // Next
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            MaterialTheme.colorScheme.onSurface.copy(
                                alpha = if (isDark) 0.09f else 0.06f
                            )
                        )
                        .clickable { viewModel.playNextSong() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector        = Icons.Rounded.SkipNext,
                        contentDescription = "Next",
                        tint               = MaterialTheme.colorScheme.onSurface,
                        modifier           = Modifier.size(26.dp)
                    )
                }
            }

            // ─── C: Status Island (Theme Toggle + 12-hr Clock) ─────────────────
            Row(
                modifier = Modifier
                    .wrapContentWidth()
                    .fillMaxHeight()
                    .clip(cardShape)
                    .background(brush = capsuleGrad, shape = cardShape)
                    .border(BorderStroke(0.8.dp, borderBrush), cardShape)
                    .padding(horizontal = 10.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Theme toggle: sun (in dark) / moon (in light)
                IconButton(
                    onClick  = { viewModel.toggleTheme() },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector        = if (isDarkTheme) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                        contentDescription = "Toggle theme",
                        tint               = if (isDarkTheme) Color(0xFFFFD54F) else Color(0xFF7986CB),
                        modifier           = Modifier.size(20.dp)
                    )
                }

                VerticalDivider(
                    modifier = Modifier.height(22.dp).width(0.6.dp),
                    color    = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                )

                // 12-hour clock: "3:27" + superscript "PM"
                val timeParts  = currentTime.split(" ")
                val timeDigits = timeParts.getOrElse(0) { currentTime }
                val amPmLabel  = timeParts.getOrElse(1) { "" }

                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(
                                if (isPlaying) Color(0xFF00E676)
                                else if (isDark) Color(0xFF455A64)
                                else Color(0xFFB0BEC5)
                            )
                    )
                    Text(
                        text = buildAnnotatedString {
                            withStyle(
                                SpanStyle(
                                    fontWeight    = FontWeight.Bold,
                                    fontSize      = 16.sp,
                                    letterSpacing = 0.3.sp
                                )
                            ) { append(timeDigits) }
                            append("\u202F") // narrow no-break space
                            withStyle(
                                SpanStyle(
                                    fontWeight    = FontWeight.SemiBold,
                                    fontSize      = 9.sp,
                                    baselineShift = BaselineShift.Superscript,
                                    color         = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) { append(amPmLabel) }
                        },
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}


