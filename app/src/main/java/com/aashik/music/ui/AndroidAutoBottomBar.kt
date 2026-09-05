package com.aashik.music.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.ShuffleOn
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aashik.music.theme.AppGradients
import com.aashik.music.viewmodel.MusicViewModel

private val DockCardShape = RoundedCornerShape(16.dp)
private val BadgeShape = RoundedCornerShape(4.dp)
private val ArtPlaceholderShape = RoundedCornerShape(10.dp)

/**
 * Premium Automotive Media Dock — Blaupunkt Santa Rosa 985 (1280x720 @ 160 dpi)
 * Highly optimized with scoped recompositions to isolate 250ms playback ticks from the dock UI.
 *
 * 2 Floating Segments:
 *   1. Left: [Combined Track Info + Seeker] (weight 0.68f)
 *   2. Right: [Player Controls: Shuffle, Prev, Play/Pause, Next] (weight 0.32f)
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AndroidAutoBottomBar(
    viewModel: MusicViewModel,
    @Suppress("UNUSED_PARAMETER") isSearchVisible: Boolean = false,
    @Suppress("UNUSED_PARAMETER") onToggleSearch: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Only collect state that changes on track change or user action
    // High frequency flows (progress, position) are scoped inside BottomBarTimeCounter and BottomBarSeekBar
    val isPlaying   by viewModel.isPlaying.collectAsState()
    val isShuffleOn by viewModel.isShuffleOn.collectAsState()
    val currentSong by viewModel.currentSong.collectAsState()

    val isDark          = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val capsuleGrad     = AppGradients.capsule(isActive = false)
    val borderBrush     = AppGradients.border(isActive = false)
    val primaryGradient = AppGradients.primaryButton()

    // Floating dock with transparent background
    Surface(
        color           = Color.Transparent,
        shadowElevation = 0.dp,
        tonalElevation  = 0.dp,
        modifier        = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {

            // ─── Segment 1: Combined Track Info & Seeker (Left) ───────────────────
            Row(
                modifier = Modifier
                    .weight(0.68f)
                    .fillMaxHeight()
                    .clip(DockCardShape)
                    .background(brush = capsuleGrad, shape = DockCardShape)
                    .border(width = 0.8.dp, brush = borderBrush, shape = DockCardShape)
                    .clickable { viewModel.triggerScrollToCurrentSong() }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Album art (never recomposes on progress ticks)
                Box(contentAlignment = Alignment.Center) {
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
                                .clip(ArtPlaceholderShape)
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

                // Track Metadata & Seeker
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.Center
                ) {
                    // Top row: Title, bullet, Artist, format badge, and scoped Time Counter
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
                            // Playback status dot
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
                                modifier = Modifier.weight(1f, fill = false)
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
                                        .clip(BadgeShape)
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

                        // Scoped Time Display (only this element recomposes on time changes)
                        BottomBarTimeCounter(viewModel = viewModel)
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // Scoped Clean Horizontal Seekbar (only this element recomposes on progress changes)
                    BottomBarSeekBar(viewModel = viewModel, isPlaying = isPlaying)
                }
            }

            // ─── Segment 2: Playback Action Controls (Right) ─────────────────────
            // Never recomposes during playback! Only on button clicks or play/pause state change.
            Row(
                modifier = Modifier
                    .weight(0.32f)
                    .fillMaxHeight()
                    .clip(DockCardShape)
                    .background(brush = capsuleGrad, shape = DockCardShape)
                    .border(width = 0.8.dp, brush = borderBrush, shape = DockCardShape)
                    .padding(horizontal = 8.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
            ) {
                // Shuffle
                Box(
                    modifier = Modifier
                        .size(38.dp)
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
                        modifier           = Modifier.size(20.dp)
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

                // Hero Play/Pause
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
        }
    }
}

/**
 * Isolated Time Counter — limits 250ms recompositions strictly to this Text widget.
 */
@Composable
private fun BottomBarTimeCounter(viewModel: MusicViewModel) {
    val positionMs by viewModel.positionFlow.collectAsState()
    val durationMs by viewModel.durationFlow.collectAsState()

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

/**
 * Isolated Seekbar — limits progress recompositions strictly to the canvas.
 */
@Composable
private fun BottomBarSeekBar(viewModel: MusicViewModel, isPlaying: Boolean) {
    val progress by viewModel.currentProgressFlow.collectAsState(initial = 0f)
    val positionMs by viewModel.positionFlow.collectAsState()
    val durationMs by viewModel.durationFlow.collectAsState()

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
