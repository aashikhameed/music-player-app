package com.aashik.music.desktop.ui.shell

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aashik.music.desktop.model.DesktopSong

internal val DeskDestructive = Color(0xFFFF453A)

internal class DeskAction(val icon: ImageVector, val label: String, val tint: Color? = null, val run: () -> Unit)

/** Dimmed overlay hosting a centred card; taps outside dismiss it. */
@Composable
internal fun DeskModal(visible: Boolean, onDismiss: () -> Unit, content: @Composable () -> Unit) {
    AnimatedVisibility(visible, enter = fadeIn(), exit = fadeOut()) {
        Box(
            Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            Box(Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}) { content() }
        }
    }
}

/** ⋯ sheet: 2-up action tiles under the song header (same actions as the headunit that the desktop supports). */
@Composable
internal fun DeskSongActionSheet(song: DesktopSong?, actions: List<DeskAction>, onDismiss: () -> Unit) {
    val c = Desk.c
    val shown = remember { arrayOfNulls<DesktopSong>(1) }
    if (song != null) shown[0] = song
    val s = shown[0]
    DeskModal(visible = song != null, onDismiss = onDismiss) {
        if (s == null) return@DeskModal
        Column(Modifier.width(600.dp).deskLiquid(24.dp, lens = false, surfaceAlpha = 0.7f).padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                DeskArt(s, 64.dp)
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(s.title.ifBlank { "Untitled" }, color = c.text, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(s.artist.ifBlank { "Unknown Artist" }, color = c.textSecondary, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Spacer(Modifier.height(16.dp))
            actions.chunked(2).forEach { pair ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    pair.forEach { a ->
                        val tint = a.tint ?: c.text
                        Row(
                            Modifier.weight(1f).height(60.dp)
                                .background(if (a.tint == DeskDestructive) DeskDestructive.copy(alpha = 0.12f) else c.idleFill, DeskCard)
                                .clickable { a.run(); onDismiss() }.padding(horizontal = 18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DeskGlyph(a.icon, tint, 22.dp); Spacer(Modifier.width(14.dp))
                            Text(a.label, color = tint, fontSize = 16.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                        }
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
internal fun DeskConfirmDialog(visible: Boolean, title: String, message: String, confirmLabel: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val c = Desk.c
    DeskModal(visible, onDismiss) {
        Column(Modifier.width(480.dp).deskLiquid(24.dp, lens = false, surfaceAlpha = 0.7f).padding(24.dp)) {
            Text(title, color = c.text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(message, color = c.textSecondary, fontSize = 16.sp)
            Spacer(Modifier.height(24.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)) {
                DeskPillButton("Cancel", onDismiss, accent = false)
                Row(
                    Modifier.height(48.dp).background(DeskDestructive, DeskPill).clickable(onClick = onConfirm).padding(horizontal = 22.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) { Text(confirmLabel, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}
