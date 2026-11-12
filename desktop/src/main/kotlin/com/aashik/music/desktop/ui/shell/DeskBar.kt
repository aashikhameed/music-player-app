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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ArrowCircleDown
import androidx.compose.material.icons.rounded.Podcasts
import androidx.compose.material.icons.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
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

private val BarHeight = 64.dp
private val BarBottom = 14.dp
private const val BarFraction = 0.92f

/**
 * Content area with the floating mini player (macOS Music style): Kyant liquid glass (vibrancy, blur, lens)
 * over [backdrop], which records [content]. [overlay] draws above the content but outside the recorded
 * layer (e.g. the search suggestions panel, which is glass too and must not sample itself).
 */
@Composable
internal fun DeskFloatingLayer(
    song: DesktopSong?,
    isPlaying: Boolean,
    shuffle: Boolean,
    repeat: DeskRepeat,
    lyricsOpen: Boolean,
    queueOpen: Boolean,
    actions: DeskTransportActions,
    onLyrics: () -> Unit,
    onQueue: () -> Unit,
    onMore: () -> Unit,
    radioLoading: Boolean,
    onRadio: () -> Unit,
    download: DownloadState,
    onDownload: () -> Unit,
    modifier: Modifier = Modifier,
    overlay: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit = {}
) {
    val c = Desk.c
    BoxWithConstraints(modifier) {
        val barW = maxWidth * BarFraction
        overlay()
        Box(
            Modifier.align(Alignment.BottomCenter).padding(bottom = BarBottom).width(barW).height(BarHeight)
                .deskLiquid(32.dp, lens = true, surfaceAlpha = 0.28f, floating = true).pointerInput(Unit) {}
        ) {
            Row(Modifier.fillMaxSize().padding(start = 14.dp, end = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                val dim = if (song != null) 1f else 0.4f
                DeskIconButton(Icons.Rounded.Shuffle, actions.onToggleShuffle, size = 36.dp, glyph = 18.dp, tint = if (shuffle) c.accent else c.textSecondary)
                DeskIconButton(Icons.Rounded.FastRewind, actions.onPrevious, size = 42.dp, glyph = 28.dp, tint = c.text.copy(alpha = dim))
                DeskIconButton(if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, actions.onPlayPause, size = 46.dp, glyph = 36.dp)
                DeskIconButton(Icons.Rounded.FastForward, actions.onNext, size = 42.dp, glyph = 28.dp, tint = c.text.copy(alpha = dim))
                DeskIconButton(if (repeat == DeskRepeat.ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat, actions.onCycleRepeat, size = 36.dp, glyph = 18.dp, tint = if (repeat != DeskRepeat.OFF) c.accent else c.textSecondary)
                Spacer(Modifier.width(14.dp))
                NowPlayingInfo(song, actions, Modifier.weight(1f))
                Spacer(Modifier.width(10.dp))
                DeskIconButton(Icons.Rounded.Podcasts, onRadio, size = 38.dp, glyph = 21.dp, tint = c.text.copy(alpha = dim), enabled = song != null && !radioLoading, loading = radioLoading)
                DownloadButton(download, onDownload)
                DeskIconButton(Icons.Rounded.MoreHoriz, onMore, size = 38.dp, glyph = 22.dp, tint = c.text.copy(alpha = dim), enabled = song != null)
                DeskIconButton(Icons.Rounded.Chat, onLyrics, size = 38.dp, glyph = 20.dp, tint = if (lyricsOpen) c.accent else c.textSecondary)
                DeskIconButton(Icons.Rounded.FormatListBulleted, onQueue, size = 38.dp, glyph = 22.dp, tint = if (queueOpen) c.accent else c.text)
                Volume()
            }
        }
    }
}

/** None: nothing to download (no song). */
internal sealed interface DownloadState {
    data object None : DownloadState
    data object Available : DownloadState
    data class Running(val progress: Float) : DownloadState
    data object Done : DownloadState
}

@Composable
private fun DownloadButton(state: DownloadState, onClick: () -> Unit) {
    val c = Desk.c
    when (state) {
        DownloadState.None -> DeskIconButton(Icons.Rounded.ArrowCircleDown, {}, size = 38.dp, glyph = 22.dp, tint = c.text.copy(alpha = 0.4f), enabled = false)
        DownloadState.Available -> DeskIconButton(Icons.Rounded.ArrowCircleDown, onClick, size = 38.dp, glyph = 22.dp, tint = c.text)
        DownloadState.Done -> DeskIconButton(Icons.Rounded.CheckCircle, {}, size = 38.dp, glyph = 22.dp, tint = c.accent)
        is DownloadState.Running -> Box(Modifier.size(38.dp), Alignment.Center) {
            androidx.compose.material3.CircularProgressIndicator(
                progress = { state.progress.coerceAtLeast(0.03f) }, color = c.accent, trackColor = c.text.copy(alpha = 0.2f),
                strokeWidth = 2.5.dp, modifier = Modifier.size(20.dp)
            )
        }
    }
}

/** Artwork, title / artist and the progress line under them (no separate panel, as in Music). */
@Composable
private fun NowPlayingInfo(song: DesktopSong?, actions: DeskTransportActions, modifier: Modifier) {
    val c = Desk.c
    if (song == null) {
        Box(modifier.fillMaxHeight(), Alignment.Center) { DeskGlyph(Icons.Rounded.MusicNote, c.textSecondary, 24.dp) }
        return
    }
    Row(modifier.fillMaxHeight(), verticalAlignment = Alignment.CenterVertically) {
        DeskArt(song, 48.dp, DeskArt8)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(song.title.ifBlank { "Untitled" }, color = c.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(song.artist.ifBlank { "Unknown Artist" }, color = c.text.copy(alpha = 0.85f), fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            BarProgress(song, actions)
        }
    }
}

/** Leaf: thin scrubbable progress line (the only part of the bar that reads playback ticks). */
@Composable
private fun BarProgress(song: DesktopSong, actions: DeskTransportActions) {
    val c = Desk.c
    val pos by DesktopAudioPlayer.currentPositionMs.collectAsState()
    val dur by DesktopAudioPlayer.durationMs.collectAsState()
    val total = if (dur > 0) dur else song.duration
    val frac = if (total > 0) (pos.toFloat() / total).coerceIn(0f, 1f) else 0f
    Box(
        Modifier.fillMaxWidth(0.92f).height(8.dp).pointerInput(total) {
            detectTapGestures { o -> if (total > 0) actions.onSeekFraction((o.x / size.width).coerceIn(0f, 1f)) }
        },
        Alignment.CenterStart
    ) {
        Box(Modifier.fillMaxWidth().height(3.dp).background(c.text.copy(alpha = 0.22f), DeskPill))
        Box(Modifier.fillMaxWidth(frac).height(3.dp).background(c.text.copy(alpha = 0.9f), DeskPill))
    }
}

@Composable
private fun Volume() {
    val c = Desk.c
    val vol by DesktopAudioPlayer.volume.collectAsState()
    var open by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (open) Box(Modifier.width(90.dp)) { DeskSeekSlider(progress = { vol }, onSeek = { DesktopAudioPlayer.setVolume(it) }, compact = true, overPage = true) }
        DeskIconButton(Icons.Rounded.VolumeUp, { open = !open }, size = 38.dp, glyph = 22.dp, tint = if (open) c.accent else c.textSecondary)
    }
}
