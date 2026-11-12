package com.aashik.music.ui.car.glass

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aashik.music.model.PlaybackMode
import com.aashik.music.ui.designsystem.Text
import com.aashik.music.ui.mobile.glass.MobileTheme
import com.aashik.music.viewmodel.MusicViewModel

/**
 * Floating playback capsule (tablet reference, replaces AndroidAutoBottomBar).
 * Solid panel colour at 92% — no backdrop sampling (AGENTS.md §7).
 * Left: mode, previous, play/pause, next · centre: artwork + title (tap → Now Playing) ·
 * right: locate current song in the list, queue. A scrubbable progress line runs along the bottom.
 */
@Composable
internal fun CarPlaybackCapsule(
    viewModel: MusicViewModel,
    albumArt: ImageBitmap?,
    onOpenNowPlaying: () -> Unit,
    modifier: Modifier = Modifier
) {
    val song by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val mode by viewModel.playbackMode.collectAsState()
    val isQueueOpen by viewModel.isQueueOpen.collectAsState()
    val current = song ?: return

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(CarDims.CapsuleHeight)
            .carGlass(34.dp, CarCapsuleShape, flatAlpha = 0.94f)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp), // (68 − 52) / 2: concentric with the capsule
            verticalAlignment = Alignment.CenterVertically
        ) {
            val (modeIcon, modeLabel, modeActive) = when (mode) {
                PlaybackMode.SHUFFLE -> Triple(Icons.Rounded.Shuffle, "Shuffle on", true)
                PlaybackMode.REPEAT_ONE -> Triple(Icons.Rounded.RepeatOne, "Repeat one on", true)
                PlaybackMode.QUEUE -> Triple(Icons.Rounded.Repeat, "Playing in order", false)
            }
            CarIconButton(modeIcon, modeLabel, onClick = { viewModel.cyclePlaybackMode() }, size = 52.dp, glyph = 22.dp,
                tint = if (modeActive) MobileTheme.accent else MobileTheme.textSecondary)
            CarIconButton(Icons.Rounded.SkipPrevious, "Previous", onClick = { viewModel.playPreviousSong() }, size = 52.dp, glyph = 30.dp)
            CarIconButton(
                if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                if (isPlaying) "Pause" else "Play",
                onClick = { viewModel.togglePlayPause() },
                size = 52.dp,
                glyph = 30.dp,
                active = true
            )
            CarIconButton(Icons.Rounded.SkipNext, "Next", onClick = { viewModel.playNextSong() }, size = 52.dp, glyph = 30.dp)
            Spacer(Modifier.width(12.dp))

            // ── Track info: tap opens Now Playing (decision D2) ──
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onOpenNowPlaying),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnimatedContent(
                    targetState = current.id to albumArt,
                    transitionSpec = {
                        (slideInHorizontally(spring(dampingRatio = 0.85f, stiffness = 380f)) { it / 2 } + fadeIn(tween(200))) togetherWith fadeOut(tween(120))
                    },
                    contentKey = { it.first },
                    label = "capsuleArt"
                ) { (_, art) ->
                    Box(Modifier.size(44.dp).clip(CarArtShape8).background(carIdleFill), contentAlignment = Alignment.Center) {
                        if (art != null) Image(bitmap = art, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        else CarGlyph(Icons.Rounded.MusicNote, null, MobileTheme.textTertiary, 22.dp)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(current.title.ifBlank { "Untitled" }, color = MobileTheme.text, fontSize = 16.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(current.artist.ifBlank { "Unknown Artist" }, color = MobileTheme.textSecondary, fontSize = 13.sp, lineHeight = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                carFormatBadge(current)?.let { badge ->
                    Spacer(Modifier.width(10.dp))
                    Box(Modifier.background(carIdleFill, CarPillShape).padding(horizontal = 8.dp, vertical = 3.dp)) {
                        Text(badge, color = MobileTheme.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(Modifier.width(12.dp))
                CapsuleTimeCounter(viewModel)
            }
            Spacer(Modifier.width(8.dp))
            CarIconButton(Icons.Rounded.MyLocation, "Show current song in list", onClick = { viewModel.triggerScrollToCurrentSong() }, size = 52.dp, glyph = 22.dp, tint = MobileTheme.textSecondary)
            CarIconButton(Icons.AutoMirrored.Rounded.QueueMusic, "Queue", onClick = { viewModel.toggleQueue() }, size = 52.dp, glyph = 24.dp,
                tint = if (isQueueOpen) MobileTheme.accent else MobileTheme.text)
        }
        CapsuleProgress(viewModel, Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 28.dp))
    }
}

/** Leaf: elapsed / total; only this recomposes on position ticks. */
@Composable
private fun CapsuleTimeCounter(viewModel: MusicViewModel) {
    val positionMs by viewModel.positionFlow.collectAsState()
    val durationMs by viewModel.durationFlow.collectAsState()
    Text(
        text = if (durationMs > 0) "${carFormatTime(positionMs)} / ${carFormatTime(durationMs)}" else carFormatTime(positionMs),
        color = MobileTheme.textTertiary,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        maxLines = 1
    )
}

/**
 * Leaf: 3 dp progress line along the capsule's lower edge with a 16 dp touch band —
 * tap or drag to seek (keeps the legacy BottomBarSeekBar scrubbing).
 */
@Composable
private fun CapsuleProgress(viewModel: MusicViewModel, modifier: Modifier = Modifier) {
    val positionMs by viewModel.positionFlow.collectAsState()
    val durationMs by viewModel.durationFlow.collectAsState()
    var scrub by remember { mutableStateOf<Float?>(null) }
    val progress = scrub ?: if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    val accent = MobileTheme.accent
    val track = MobileTheme.text.copy(alpha = 0.12f)
    Canvas(
        modifier = modifier
            .height(16.dp)
            .pointerInput(durationMs) {
                if (durationMs <= 0) return@pointerInput
                detectTapGestures { offset -> viewModel.seekToFraction((offset.x / size.width).coerceIn(0f, 1f)) }
            }
            .pointerInput(durationMs) {
                if (durationMs <= 0) return@pointerInput
                detectHorizontalDragGestures(
                    onDragStart = { scrub = (it.x / size.width).coerceIn(0f, 1f) },
                    onDragEnd = { scrub?.let { viewModel.seekToFraction(it) }; scrub = null },
                    onDragCancel = { scrub = null }
                ) { change, _ ->
                    change.consume()
                    scrub = (change.position.x / size.width).coerceIn(0f, 1f)
                }
            }
    ) {
        val h = (if (scrub != null) 5.dp else 3.dp).toPx()
        val y = size.height - h - 2.dp.toPx()
        val r = CornerRadius(h / 2, h / 2)
        drawRoundRect(track, topLeft = Offset(0f, y), size = Size(size.width, h), cornerRadius = r)
        drawRoundRect(accent, topLeft = Offset(0f, y), size = Size(size.width * progress, h), cornerRadius = r)
    }
}
