package com.aashik.music.desktop.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aashik.music.desktop.audio.DesktopAudioPlayer
import com.aashik.music.desktop.model.DesktopSong
import com.aashik.music.desktop.service.DesktopLyricLine

internal enum class RightPanel { LYRICS, QUEUE }

/** Right side panel: synced lyrics or the up-next queue. */
@Composable
internal fun DeskRightPanel(
    panel: RightPanel,
    song: DesktopSong?,
    lyrics: List<DesktopLyricLine>,
    queue: List<DesktopSong>,
    onPlayFromQueue: (DesktopSong) -> Unit,
    onRemoveFromQueue: (DesktopSong) -> Unit,
    onClearQueue: () -> Unit,
    modifier: Modifier = Modifier
) {
    val c = Desk.c
    Box(modifier.padding(8.dp).deskLiquid(18.dp)) {
        when (panel) {
            RightPanel.LYRICS -> Lyrics(song, lyrics)
            RightPanel.QUEUE -> Queue(queue, onPlayFromQueue, onRemoveFromQueue, onClearQueue)
        }
    }
}

@Composable
private fun Lyrics(song: DesktopSong?, lyrics: List<DesktopLyricLine>) {
    val c = Desk.c
    if (song == null || lyrics.isEmpty()) {
        Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            if (song == null) Text("Play a song to see lyrics here.", color = c.textSecondary, fontSize = 15.sp, textAlign = TextAlign.Center)
            else {
                Text("No Lyrics Available", color = c.textSecondary, fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Spacer(Modifier.height(10.dp))
                Text("There aren’t any lyrics available for this song.", color = c.textSecondary, fontSize = 15.sp, textAlign = TextAlign.Center)
            }
        }
        return
    }
    val pos by DesktopAudioPlayer.currentPositionMs.collectAsState()
    val current by remember(lyrics) { derivedStateOf { lyrics.indexOfLast { it.timeMs <= pos }.coerceAtLeast(0) } }
    val state = rememberLazyListState()
    LaunchedEffect(current, song.id) { state.animateScrollToItem((current - 1).coerceAtLeast(0)) }
    LazyColumn(state = state, modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp), contentPadding = PaddingValues(top = 60.dp, bottom = 120.dp)) {
        itemsIndexed(lyrics) { i, line ->
            Text(line.text, color = if (i == current) c.text else c.text.copy(alpha = 0.35f), fontSize = 19.sp, fontWeight = FontWeight.Bold, lineHeight = 25.sp, modifier = Modifier.padding(vertical = 8.dp))
        }
    }
}

@Composable
private fun Queue(queue: List<DesktopSong>, onPlay: (DesktopSong) -> Unit, onRemove: (DesktopSong) -> Unit, onClear: () -> Unit) {
    val c = Desk.c
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().height(60.dp).padding(start = 16.dp, end = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Playing Next", color = c.text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            if (queue.isNotEmpty()) Text("Clear", color = c.accent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable(onClick = onClear).padding(8.dp))
        }
        if (queue.isEmpty()) Box(Modifier.fillMaxSize().padding(24.dp), Alignment.Center) { Text("Nothing up next.", color = c.textSecondary, fontSize = 15.sp) }
        else LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 110.dp)) {
            itemsIndexed(queue, key = { i, s -> "$i-${s.id}" }) { _, s ->
                Row(Modifier.fillMaxWidth().height(56.dp).clickable { onPlay(s) }.padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    DeskArt(s, 40.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(s.title.ifBlank { "Untitled" }, color = c.text, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(s.artist, color = c.textSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    DeskIconButton(Icons.Rounded.Close, { onRemove(s) }, size = 32.dp, glyph = 16.dp, tint = c.textSecondary)
                }
            }
        }
    }
}
