package com.aashik.music.desktop.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aashik.music.desktop.audio.DesktopAudioPlayer
import com.aashik.music.desktop.model.DesktopSong

internal enum class DeskRepeat { OFF, ALL, ONE }

internal class DeskTransportActions(
    val onPlayPause: () -> Unit,
    val onPrevious: () -> Unit,
    val onNext: () -> Unit,
    val onToggleShuffle: () -> Unit,
    val onCycleRepeat: () -> Unit,
    val onSeekFraction: (Float) -> Unit
)

private val BarHeight = 56.dp
private val BarBottom = 16.dp
private const val BarFraction = 0.88f

/**
 * Content area with the floating player bar. The bar is real frosted glass: it draws a blurred copy of
 * [content] (state is hoisted, so both copies show the same rows) under a translucent tint.
 */
@Composable
internal fun DeskContentWithBar(
    song: DesktopSong?,
    isPlaying: Boolean,
    shuffle: Boolean,
    repeat: DeskRepeat,
    lyricsOpen: Boolean,
    queueOpen: Boolean,
    actions: DeskTransportActions,
    onLyrics: () -> Unit,
    onQueue: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val c = Desk.c
    BoxWithConstraints(modifier) {
        val areaW = maxWidth; val areaH = maxHeight
        val barW = areaW * BarFraction
        val barX = (areaW - barW) / 2
        val barY = areaH - BarBottom - BarHeight
        Box(Modifier.fillMaxSize()) { content() }
        Box(Modifier.align(Alignment.BottomCenter).padding(bottom = BarBottom).width(barW).height(BarHeight).clip(DeskCapsule)) {
            // frosted backdrop: blurred copy of the content, aligned to the same pixels
            val density = androidx.compose.ui.platform.LocalDensity.current
            val (wPx, hPx, xPx, yPx) = with(density) { listOf(areaW.roundToPx(), areaH.roundToPx(), barX.roundToPx(), barY.roundToPx()) }
            Box(
                Modifier
                    .layout { measurable, _ ->
                        val p = measurable.measure(androidx.compose.ui.unit.Constraints.fixed(wPx, hPx))
                        layout(0, 0) { p.place(-xPx, -yPx) }
                    }
                    .graphicsLayer { renderEffect = androidx.compose.ui.graphics.BlurEffect(36f, 36f, androidx.compose.ui.graphics.TileMode.Clamp) }
                    .background(c.background)
            ) { content() }
            Box(Modifier.fillMaxSize().background(c.panel.copy(alpha = 0.62f)).pointerInput(Unit) {})
            Row(Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                DeskIconButton(Icons.Rounded.Shuffle, actions.onToggleShuffle, size = 38.dp, glyph = 18.dp, tint = if (shuffle) c.accent else c.textSecondary)
                DeskIconButton(Icons.Rounded.SkipPrevious, actions.onPrevious, size = 40.dp, glyph = 24.dp, tint = c.text.copy(alpha = if (song != null) 1f else 0.4f))
                DeskIconButton(if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, actions.onPlayPause, size = 44.dp, glyph = 30.dp)
                DeskIconButton(Icons.Rounded.SkipNext, actions.onNext, size = 40.dp, glyph = 24.dp, tint = c.text.copy(alpha = if (song != null) 1f else 0.4f))
                DeskIconButton(if (repeat == DeskRepeat.ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat, actions.onCycleRepeat, size = 38.dp, glyph = 18.dp, tint = if (repeat != DeskRepeat.OFF) c.accent else c.textSecondary)
                Spacer(Modifier.width(8.dp))
                Center(song, actions, Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                DeskIconButton(Icons.Rounded.Chat, onLyrics, size = 38.dp, glyph = 20.dp, tint = if (lyricsOpen) c.accent else c.textSecondary)
                DeskIconButton(Icons.Rounded.QueueMusic, onQueue, size = 38.dp, glyph = 22.dp, tint = if (queueOpen) c.accent else c.textSecondary)
                Volume()
            }
        }
    }
}

@Composable
private fun Center(song: DesktopSong?, actions: DeskTransportActions, modifier: Modifier) {
    val c = Desk.c
    Box(modifier.fillMaxHeight().background(c.text.copy(alpha = 0.06f), DeskItem), Alignment.Center) {
        if (song == null) {
            DeskGlyph(Icons.Rounded.MusicNote, c.textSecondary, 22.dp)
        } else {
            Row(Modifier.fillMaxSize().padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                DeskArt(song, 40.dp, DeskArt8)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f), verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center) {
                    Text(song.title.ifBlank { "Untitled" }, color = c.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    Text(song.artist.ifBlank { "Unknown Artist" } + if (song.album != "Unknown Album") " — ${song.album}" else "", color = c.textSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    BarProgress(song, actions)
                }
            }
        }
    }
}

/** Leaf: elapsed / remaining around a scrubbable line (only this reads playback ticks). */
@Composable
private fun BarProgress(song: DesktopSong, actions: DeskTransportActions) {
    val c = Desk.c
    val pos by DesktopAudioPlayer.currentPositionMs.collectAsState()
    val dur by DesktopAudioPlayer.durationMs.collectAsState()
    val total = if (dur > 0) dur else song.duration
    Row(Modifier.fillMaxWidth().height(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(deskFormatTime(pos), color = c.textSecondary, fontSize = 10.sp, modifier = Modifier.width(34.dp))
        Box(Modifier.weight(1f).height(14.dp).pointerInput(total) {
            detectTapGestures { o -> if (total > 0) actions.onSeekFraction((o.x / size.width).coerceIn(0f, 1f)) }
        }, Alignment.Center) {
            Box(Modifier.fillMaxWidth().height(3.dp).background(c.text.copy(alpha = 0.2f), DeskPill))
            Box(Modifier.fillMaxWidth().height(3.dp), Alignment.CenterStart) {
                Box(Modifier.fillMaxWidth(if (total > 0) (pos.toFloat() / total).coerceIn(0f, 1f) else 0f).height(3.dp).background(c.text, DeskPill))
            }
        }
        Text("-" + deskFormatTime((total - pos).coerceAtLeast(0)), color = c.textSecondary, fontSize = 10.sp, textAlign = TextAlign.End, modifier = Modifier.width(38.dp))
    }
}

@Composable
private fun Volume() {
    val c = Desk.c
    val vol by DesktopAudioPlayer.volume.collectAsState()
    var open by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (open) Box(Modifier.width(90.dp)) { DeskSeekSlider(progress = { vol }, onSeek = { DesktopAudioPlayer.setVolume(it) }, compact = true) }
        DeskIconButton(Icons.Rounded.VolumeUp, { open = !open }, size = 38.dp, glyph = 22.dp, tint = if (open) c.accent else c.textSecondary)
    }
}
