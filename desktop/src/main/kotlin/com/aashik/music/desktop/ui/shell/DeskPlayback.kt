package com.aashik.music.desktop.ui.shell

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import com.aashik.music.desktop.audio.DesktopAudioPlayer
import com.aashik.music.desktop.model.DesktopSong
import com.aashik.music.desktop.service.DesktopLyricLine

internal enum class DeskPane { NONE, LYRICS, UP_NEXT }

internal class DeskPlaybackActions(
    val onPlayPause: () -> Unit,
    val onPrevious: () -> Unit,
    val onNext: () -> Unit,
    val onToggleShuffle: () -> Unit,
    val onToggleRepeat: () -> Unit,
    val onSeekFraction: (Float) -> Unit
)

// ─── Floating capsule ────────────────────────────────────────────────────────

@Composable
internal fun DeskCapsule(
    song: DesktopSong,
    isPlaying: Boolean,
    isShuffle: Boolean,
    isRepeat: Boolean,
    actions: DeskPlaybackActions,
    onOpenNowPlaying: () -> Unit,
    onOpenQueue: () -> Unit,
    modifier: Modifier = Modifier
) {
    val c = Desk.c
    Box(modifier.fillMaxWidth().height(68.dp).deskGlass(DeskCapsule, 0.94f)) {
        Row(Modifier.fillMaxSize().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            DeskIconButton(Icons.Rounded.Shuffle, actions.onToggleShuffle, tint = if (isShuffle) c.accent else c.textSecondary)
            DeskIconButton(Icons.Rounded.SkipPrevious, actions.onPrevious, glyph = 28.dp)
            DeskIconButton(if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, actions.onPlayPause, size = 52.dp, glyph = 30.dp, active = true)
            DeskIconButton(Icons.Rounded.SkipNext, actions.onNext, glyph = 28.dp)
            Spacer(Modifier.width(12.dp))
            Row(Modifier.weight(1f).fillMaxHeight().clickable(onClick = onOpenNowPlaying), verticalAlignment = Alignment.CenterVertically) {
                DeskArt(song, 44.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(song.title.ifBlank { "Untitled" }, color = c.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(song.artist.ifBlank { "Unknown Artist" }, color = c.textSecondary, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                CapsuleTime(song)
                Spacer(Modifier.width(8.dp))
            }
            DeskIconButton(if (isRepeat) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat, actions.onToggleRepeat, tint = if (isRepeat) c.accent else c.textSecondary)
            DeskIconButton(Icons.Rounded.QueueMusic, onOpenQueue)
        }
        CapsuleProgress(Modifier.align(Alignment.BottomCenter).padding(horizontal = 30.dp, vertical = 4.dp))
    }
}

/** Leaf: elapsed / total, the only place besides the progress that reads playback ticks. */
@Composable
private fun CapsuleTime(song: DesktopSong) {
    val c = Desk.c
    val pos by DesktopAudioPlayer.currentPositionMs.collectAsState()
    val dur by DesktopAudioPlayer.durationMs.collectAsState()
    Text("${deskFormatTime(pos)} / ${deskFormatTime(if (dur > 0) dur else song.duration)}", color = c.textSecondary, fontSize = 13.sp, maxLines = 1)
}

@Composable
private fun CapsuleProgress(modifier: Modifier) {
    val c = Desk.c
    val pos by DesktopAudioPlayer.currentPositionMs.collectAsState()
    val dur by DesktopAudioPlayer.durationMs.collectAsState()
    val p = if (dur > 0) (pos.toFloat() / dur).coerceIn(0f, 1f) else 0f
    Box(modifier.fillMaxWidth().height(3.dp).background(c.text.copy(alpha = 0.14f), DeskPill)) {
        Box(Modifier.fillMaxWidth(p).fillMaxHeight().background(c.accent, DeskPill))
    }
}

// ─── Seek + transport (leaves) ──────────────────────────────────────────────

@Composable
internal fun DeskSeek(song: DesktopSong?, onSeekFraction: (Float) -> Unit) {
    val c = Desk.c
    val pos by DesktopAudioPlayer.currentPositionMs.collectAsState()
    val dur by DesktopAudioPlayer.durationMs.collectAsState()
    val total = if (dur > 0) dur else (song?.duration ?: 0L)
    Column(Modifier.fillMaxWidth()) {
        DeskSeekSlider(progress = { if (total > 0) (pos.toFloat() / total).coerceIn(0f, 1f) else 0f }, onSeek = onSeekFraction)
        Row(Modifier.fillMaxWidth().offset(y = (-4).dp)) {
            Text(deskFormatTime(pos), color = c.textSecondary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.weight(1f))
            Text("-" + deskFormatTime((total - pos).coerceAtLeast(0)), color = c.textSecondary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
internal fun DeskTransport(isPlaying: Boolean, isShuffle: Boolean, isRepeat: Boolean, actions: DeskPlaybackActions, big: Boolean) {
    val c = Desk.c
    val play = if (big) 88.dp else 64.dp
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
        DeskIconButton(Icons.Rounded.Shuffle, actions.onToggleShuffle, size = 52.dp, tint = if (isShuffle) c.accent else c.textSecondary)
        DeskIconButton(Icons.Rounded.SkipPrevious, actions.onPrevious, size = 60.dp, glyph = 34.dp)
        DeskIconButton(if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, actions.onPlayPause, size = play, glyph = play * 0.5f, active = true)
        DeskIconButton(Icons.Rounded.SkipNext, actions.onNext, size = 60.dp, glyph = 34.dp)
        DeskIconButton(if (isRepeat) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat, actions.onToggleRepeat, size = 52.dp, tint = if (isRepeat) c.accent else c.textSecondary)
    }
}

// ─── Now Playing: stage ↔ split ─────────────────────────────────────────────

private val SplitArt = 340.dp

@Composable
internal fun DeskNowPlaying(
    song: DesktopSong?,
    art: ImageBitmap?,
    isPlaying: Boolean,
    isShuffle: Boolean,
    isRepeat: Boolean,
    isFavorite: Boolean,
    lyrics: List<DesktopLyricLine>,
    queue: List<DesktopSong>,
    pane: DeskPane,
    onPane: (DeskPane) -> Unit,
    actions: DeskPlaybackActions,
    onToggleFavorite: () -> Unit,
    onPlayFromQueue: (DesktopSong) -> Unit,
    onRemoveFromQueue: (DesktopSong) -> Unit,
    onClearQueue: () -> Unit,
    modifier: Modifier = Modifier
) {
    val c = Desk.c
    BoxWithConstraints(modifier.padding(horizontal = 32.dp).padding(top = 24.dp, bottom = 24.dp)) {
        val stageSide = minOf(maxHeight, maxWidth * 0.46f)
        val t by animateFloatAsState(if (pane != DeskPane.NONE) 1f else 0f, spring(dampingRatio = 0.86f, stiffness = 320f))
        val artSide = lerp(stageSide, SplitArt, t)
        val pauseScale by animateFloatAsState(if (isPlaying) 1f else 0.97f, spring(dampingRatio = 0.7f, stiffness = 300f))

        // Stage: control column right of the big artwork
        AnimatedVisibility(visible = pane == DeskPane.NONE, enter = fadeIn(tween(240, 80)), exit = fadeOut(tween(100)), modifier = Modifier.fillMaxSize().padding(start = stageSide + 48.dp)) {
            Column(Modifier.fillMaxSize()) {
                Text(song?.title?.ifBlank { "Untitled" } ?: "Not Playing", color = c.text, fontSize = 38.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 44.sp)
                Spacer(Modifier.height(4.dp))
                Text(song?.artist?.ifBlank { "Unknown Artist" } ?: "Pick a song from your library", color = c.textSecondary, fontSize = 20.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (song != null) Text(song.album, color = c.textTertiary, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(18.dp))
                if (song != null) FavoriteChip(isFavorite, onToggleFavorite)
                Spacer(Modifier.weight(1f))
                DeskSeek(song, actions.onSeekFraction)
                Spacer(Modifier.height(4.dp))
                DeskTransport(isPlaying, isShuffle, isRepeat, actions, big = true)
                Spacer(Modifier.height(16.dp))
                PaneSwitch(selected = DeskPane.NONE, onPane = onPane)
            }
        }

        // Split: compact column + panel
        AnimatedVisibility(visible = pane != DeskPane.NONE, enter = fadeIn(tween(240, 90)), exit = fadeOut(tween(100)), modifier = Modifier.fillMaxSize()) {
            val shown = remember { androidx.compose.runtime.mutableStateOf(DeskPane.LYRICS) }
            androidx.compose.runtime.SideEffect { if (pane != DeskPane.NONE) shown.value = pane }
            Row(Modifier.fillMaxSize()) {
                Column(Modifier.width(SplitArt).fillMaxHeight()) {
                    Spacer(Modifier.height(SplitArt + 16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(song?.title?.ifBlank { "Untitled" } ?: "Not Playing", color = c.text, fontSize = 24.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(song?.artist ?: "", color = c.textSecondary, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        if (song != null) DeskIconButton(if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, onToggleFavorite, tint = if (isFavorite) c.accent else c.text)
                    }
                    Spacer(Modifier.weight(1f))
                    DeskSeek(song, actions.onSeekFraction)
                    DeskTransport(isPlaying, isShuffle, isRepeat, actions, big = false)
                }
                Spacer(Modifier.width(32.dp))
                Column(Modifier.weight(1f).fillMaxHeight().clip(DeskSheet).background(c.panel)) {
                    AnimatedContent(shown.value, transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(120)) }, modifier = Modifier.weight(1f).fillMaxWidth()) { p ->
                        if (p == DeskPane.LYRICS) DeskLyrics(lyrics) else DeskQueueList(queue, onPlayFromQueue, onRemoveFromQueue, onClearQueue)
                    }
                    Box(Modifier.padding(12.dp)) { PaneSwitch(selected = pane, onPane = onPane) }
                }
            }
        }

        // Artwork (shared; shrinks in place when a panel opens)
        AnimatedContent(
            targetState = song?.id to art,
            contentKey = { it.first },
            transitionSpec = { fadeIn(tween(240)) togetherWith fadeOut(tween(160)) },
            modifier = Modifier.size(artSide).clickable(enabled = pane != DeskPane.NONE, interactionSource = remember { MutableInteractionSource() }, indication = null) { onPane(DeskPane.NONE) }
        ) { (_, bmp) ->
            Box(Modifier.fillMaxSize().graphicsLayer { scaleX = pauseScale; scaleY = pauseScale }.clip(DeskHeroArt).background(c.idleFill), contentAlignment = Alignment.Center) {
                if (bmp != null) Image(bmp, "Artwork", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                else DeskGlyph(Icons.Rounded.MusicNote, c.textTertiary, 96.dp)
            }
        }
    }
}

@Composable
private fun FavoriteChip(isFavorite: Boolean, onClick: () -> Unit) {
    val c = Desk.c
    androidx.compose.foundation.layout.Row(
        Modifier.height(48.dp).background(if (isFavorite) c.selectedFill else c.idleFill, DeskPill).clickable(onClick = onClick).padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DeskGlyph(if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, if (isFavorite) c.accent else c.text, 20.dp)
        Spacer(Modifier.width(8.dp))
        Text(if (isFavorite) "Favorited" else "Favorite", color = if (isFavorite) c.accent else c.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun PaneSwitch(selected: DeskPane, onPane: (DeskPane) -> Unit) {
    val c = Desk.c
    Row(Modifier.fillMaxWidth().height(56.dp).background(c.idleFill, DeskPill).padding(4.dp)) {
        listOf(DeskPane.LYRICS to ("Lyrics" to Icons.Rounded.Lyrics), DeskPane.UP_NEXT to ("Up Next" to Icons.Rounded.QueueMusic)).forEach { (pane, meta) ->
            val on = selected == pane
            Row(
                Modifier.weight(1f).fillMaxHeight().background(if (on) c.accent else androidx.compose.ui.graphics.Color.Transparent, DeskPill)
                    .clickable { onPane(if (on) DeskPane.NONE else pane) },
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center
            ) {
                DeskGlyph(meta.second, if (on) c.onAccent else c.text, 20.dp)
                Spacer(Modifier.width(8.dp))
                Text(meta.first, color = if (on) c.onAccent else c.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun DeskLyrics(lyrics: List<DesktopLyricLine>) {
    val c = Desk.c
    if (lyrics.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No lyrics for this song", color = c.textSecondary, fontSize = 18.sp) }
        return
    }
    val pos by DesktopAudioPlayer.currentPositionMs.collectAsState()
    val current by remember(lyrics) { derivedStateOf { lyrics.indexOfLast { it.timeMs <= pos }.coerceAtLeast(0) } }
    val state = rememberLazyListState()
    LaunchedEffect(current) { state.animateScrollToItem((current - 1).coerceAtLeast(0)) }
    LazyColumn(state = state, modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 24.dp)) {
        itemsIndexed(lyrics) { i, line ->
            Text(line.text, color = if (i == current) c.text else c.text.copy(alpha = 0.35f), fontSize = 28.sp, fontWeight = FontWeight.Bold, lineHeight = 36.sp, modifier = Modifier.padding(vertical = 8.dp))
        }
    }
}

@Composable
private fun DeskQueueList(queue: List<DesktopSong>, onPlay: (DesktopSong) -> Unit, onRemove: (DesktopSong) -> Unit, onClear: () -> Unit) {
    val c = Desk.c
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().height(64.dp).padding(start = 28.dp, end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Playing Next", color = c.text, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                Text(if (queue.isEmpty()) "Nothing queued" else "${queue.size} songs", color = c.textSecondary, fontSize = 13.sp)
            }
            if (queue.isNotEmpty()) DeskPillButton("Clear", onClear, accent = false)
        }
        LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
            itemsIndexed(queue, key = { i, s -> "${s.id}-$i" }) { _, s ->
                Row(Modifier.fillMaxWidth().height(64.dp).clickable { onPlay(s) }.padding(start = 28.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    DeskArt(s, 44.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(s.title.ifBlank { "Untitled" }, color = c.text, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(s.artist, color = c.textSecondary, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    DeskIconButton(Icons.Rounded.Close, { onRemove(s) }, size = 44.dp, glyph = 20.dp, tint = c.textSecondary)
                }
            }
        }
    }
}
