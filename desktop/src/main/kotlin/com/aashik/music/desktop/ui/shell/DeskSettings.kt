package com.aashik.music.desktop.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aashik.music.desktop.audio.DesktopAudioPlayer
import com.aashik.music.desktop.service.DesktopLocalScanner
import com.aashik.music.desktop.utils.DesktopFolderPicker

private enum class Section(val label: String, val icon: ImageVector) {
    APPEARANCE("Appearance", Icons.Rounded.Palette),
    PLAYBACK("Playback", Icons.Rounded.PlayCircle),
    LIBRARY("Library & Storage", Icons.Rounded.LibraryMusic),
    AUDIO("Equalizer & Audio", Icons.Rounded.GraphicEq),
    ABOUT("About", Icons.Rounded.Info)
}

/** Master/detail settings (section list left, controls right), same structure as the headunit. */
@Composable
internal fun DeskSettings(
    themeMode: DeskThemeMode,
    onTheme: (DeskThemeMode) -> Unit,
    onStorageDirChanged: (String) -> Unit,
    onOpenEqualizer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val c = Desk.c
    var section by remember { mutableStateOf(Section.APPEARANCE) }
    Row(modifier.padding(24.dp)) {
        Column(Modifier.width(280.dp).fillMaxHeight()) {
            Text("Settings", color = c.text, fontSize = 32.sp, fontWeight = FontWeight.Bold, modifier = Modifier.height(64.dp).padding(top = 8.dp))
            Section.values().forEach { s ->
                val on = s == section
                Row(
                    Modifier.fillMaxWidth().height(56.dp).clip(DeskItem).background(if (on) c.selectedFill else Color.Transparent).clickable { section = s }.padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DeskGlyph(s.icon, if (on) c.accent else c.text, 22.dp); Spacer(Modifier.width(14.dp))
                    Text(s.label, color = if (on) c.accent else c.text, fontSize = 16.sp, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium)
                }
            }
        }
        Spacer(Modifier.width(24.dp))
        Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Spacer(Modifier.height(8.dp))
            when (section) {
                Section.APPEARANCE -> Card("Theme", "Light, Dark, or colours taken from the playing artwork.") {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { DeskThemeMode.values().forEach { Chip(it.label, it == themeMode) { _ -> onTheme(it) } } }
                }
                Section.PLAYBACK -> Card("Volume", "Also available from the speaker icon in the player bar.") { VolumeSlider() }
                Section.LIBRARY -> {
                    var dir by remember { mutableStateOf(DesktopLocalScanner.getStoredStorageDir()) }
                    Card("Music folder", "Songs are scanned from this folder.") {
                        Text(dir, color = c.accent, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            DeskPillButton("Choose Folder…", { DesktopFolderPicker.pickFolder(dir)?.let { dir = it; onStorageDirChanged(it) } }, icon = Icons.Rounded.Folder)
                            DeskPillButton("Rescan", { onStorageDirChanged(dir) }, icon = Icons.Rounded.Refresh, accent = false)
                        }
                    }
                }
                Section.AUDIO -> Card("Equalizer", "10-band equalizer, applied to the audio engine.") { DeskPillButton("Open Equalizer", onOpenEqualizer, icon = Icons.Rounded.GraphicEq) }
                Section.ABOUT -> Card("Music Player for macOS", "Version 0.4.3 • JavaFX audio engine") {}
            }
        }
    }
}

@Composable
private fun Card(title: String, subtitle: String?, content: @Composable () -> Unit) {
    val c = Desk.c
    Column(Modifier.fillMaxWidth().background(c.idleFill, DeskCard).padding(20.dp)) {
        Text(title, color = c.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        if (subtitle != null) Text(subtitle, color = c.textSecondary, fontSize = 14.sp, modifier = Modifier.padding(top = 2.dp))
        Spacer(Modifier.height(14.dp))
        content()
    }
}

@Composable
private fun Chip(label: String, selected: Boolean, onClick: (Unit) -> Unit) {
    val c = Desk.c
    Box(Modifier.height(48.dp).background(if (selected) c.accent else c.idleFill, DeskPill).clickable { onClick(Unit) }.padding(horizontal = 22.dp), Alignment.Center) {
        Text(label, color = if (selected) c.onAccent else c.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
    }
}

@Composable
private fun VolumeSlider() {
    val vol by DesktopAudioPlayer.volume.collectAsState()
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f)) { DeskSeekSlider(progress = { vol }, onSeek = { DesktopAudioPlayer.setVolume(it) }) }
        Spacer(Modifier.width(16.dp))
        Text("${(vol * 100).toInt()}%", color = Desk.c.textSecondary, fontSize = 15.sp, modifier = Modifier.width(48.dp))
    }
}

// ─── Equalizer ───────────────────────────────────────────────────────────────

private val Freqs = listOf("32", "64", "125", "250", "500", "1k", "2k", "4k", "8k", "16k")
private val Presets = linkedMapOf(
    "Flat" to listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f),
    "Bass Booster" to listOf(7f, 6f, 4f, 2f, 0f, 0f, 0f, 0f, 0f, 0f),
    "Vocal" to listOf(-2f, -2f, 0f, 2f, 4f, 4f, 2f, 0f, -1f, -2f),
    "Electronic" to listOf(5f, 4f, 1f, 0f, -2f, 2f, 1f, 3f, 5f, 5f),
    "Rock" to listOf(5f, 3f, 2f, -1f, -2f, 1f, 3f, 4f, 5f, 5f),
    "Pop" to listOf(-1f, 2f, 4f, 4f, 2f, 0f, -1f, -1f, 1f, 2f),
    "Classical" to listOf(4f, 3f, 2f, 1f, -1f, -1f, 0f, 2f, 3f, 4f),
    "Acoustic" to listOf(4f, 3f, 1f, 1f, 2f, 2f, 3f, 3f, 3f, 2f)
)

@Composable
internal fun DeskEqualizer(modifier: Modifier = Modifier) {
    val c = Desk.c
    val gains = remember { mutableStateListOf<Float>().also { it.addAll(DesktopAudioPlayer.eqBandGains) } }
    var preset by remember { mutableStateOf(Presets.entries.firstOrNull { e -> e.value == gains.toList() }?.key ?: "Custom") }
    Column(modifier.padding(24.dp)) {
        Column(Modifier.height(64.dp), verticalArrangement = Arrangement.Center) {
            Text("Equalizer", color = c.text, fontSize = 32.sp, fontWeight = FontWeight.Bold)
            Text(preset, color = c.textSecondary, fontSize = 14.sp)
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Presets.keys.forEach { name ->
                Chip(name, name == preset) { _ ->
                    preset = name
                    val v = Presets.getValue(name)
                    v.forEachIndexed { i, g -> gains[i] = g }
                    DesktopAudioPlayer.resetEq(v)
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth().weight(1f).background(c.idleFill, DeskSheet).padding(horizontal = 24.dp, vertical = 20.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            Freqs.forEachIndexed { i, f ->
                Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${if (gains[i] > 0) "+" else ""}${gains[i].toInt()} dB", color = c.textSecondary, fontSize = 12.sp)
                    Spacer(Modifier.height(8.dp))
                    BandSlider(gains[i], Modifier.weight(1f)) { g -> gains[i] = g; preset = "Custom"; DesktopAudioPlayer.setEqBand(i, g) }
                    Spacer(Modifier.height(8.dp))
                    Text(f, color = c.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        Spacer(Modifier.height(96.dp))
    }
}

@Composable
private fun BandSlider(value: Float, modifier: Modifier, onChange: (Float) -> Unit) {
    val c = Desk.c
    var h by remember { mutableStateOf(1f) }
    Box(
        modifier.width(56.dp).onSizeChanged { h = it.height.toFloat().coerceAtLeast(1f) }
            .pointerInput(Unit) {
                detectDragGestures(onDragStart = { onChange(yToGain(it.y, h)) }) { ch, _ -> ch.consume(); onChange(yToGain(ch.position.y, h)) }
            }
            .pointerInput(Unit) { detectTapGestures { onChange(yToGain(it.y, h)) } },
        contentAlignment = Alignment.TopCenter
    ) {
        Box(Modifier.width(8.dp).fillMaxHeight().background(c.text.copy(alpha = 0.14f), DeskPill))
        BoxWithConstraints(Modifier.fillMaxHeight().width(56.dp)) {
            val frac = (12f - value) / 24f
            val thumbH = 24.dp
            val y = (maxHeight - thumbH) * frac
            Box(Modifier.align(Alignment.TopCenter).offset(y = y).size(40.dp, thumbH).background(Color.White, DeskPill))
        }
    }
}

private fun yToGain(y: Float, h: Float): Float = (12f - (y / h).coerceIn(0f, 1f) * 24f).let { Math.round(it).toFloat() }
