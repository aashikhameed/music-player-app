package com.aashik.music.ui.mobile.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aashik.music.service.AppUpdateManager
import com.aashik.music.service.StreamDownloadManager
import com.aashik.music.service.UpdateState
import com.aashik.music.ui.components.DownloadLocationDialog
import com.aashik.music.ui.mobile.glass.LiquidToggle
import com.aashik.music.ui.designsystem.Text
import com.aashik.music.ui.mobile.glass.LiquidButton
import com.aashik.music.ui.mobile.glass.LiquidGlassCard
import com.aashik.music.ui.mobile.glass.LiquidSlider
import com.aashik.music.ui.mobile.glass.MobileTheme
import com.aashik.music.viewmodel.MusicViewModel
import com.kyant.backdrop.Backdrop
import kotlin.math.roundToInt

// Settings sections ported from the headunit SettingsScreen into the mobile glass style.
// Flat capsule chips (no per-chip backdrop sampling) keep the long settings list cheap to draw.

private val ChipShape = RoundedCornerShape(50)

@Composable
private fun SettingsCard(title: String, backdrop: Backdrop, content: @Composable ColumnScope.() -> Unit) {
    LiquidGlassCard(backdrop = backdrop, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = title,
                color = MobileTheme.textSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
            )
            content()
        }
    }
}

@Composable
private fun RowLabel(title: String, subtitle: String?, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(text = title, color = MobileTheme.text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        if (subtitle != null) {
            Text(text = subtitle, color = MobileTheme.textSecondary, fontSize = 11.sp, lineHeight = 14.sp)
        }
    }
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, backdrop: Backdrop, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RowLabel(title, subtitle, Modifier.weight(1f).padding(end = 12.dp))
        LiquidToggle(selected = { checked }, onSelect = onChange, backdrop = backdrop)
    }
}

@Composable
private fun <T> ChipRow(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            Box(
                modifier = Modifier
                    .height(32.dp)
                    .clip(ChipShape)
                    .background(
                        if (isSelected) MobileTheme.accent.copy(alpha = 0.22f)
                        else MobileTheme.text.copy(alpha = 0.08f)
                    )
                    .clickable { onSelect(option) }
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label(option),
                    color = if (isSelected) MobileTheme.accent else MobileTheme.text,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun ActionButton(text: String, backdrop: Backdrop, enabled: Boolean = true, onClick: () -> Unit) {
    LiquidButton(
        onClick = { if (enabled) onClick() },
        backdrop = backdrop,
        surfaceColor = MobileTheme.text.copy(alpha = 0.10f),
        modifier = Modifier.height(34.dp)
    ) {
        Text(text, fontSize = 11.sp, color = if (enabled) MobileTheme.accent else MobileTheme.textTertiary)
    }
}

private fun formatClock(sec: Int): String = "%d:%02d".format(sec / 60, sec % 60)

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1024L * 1024L -> "%.1f MB".format(bytes / (1024f * 1024f))
    bytes >= 1024L -> "%.1f KB".format(bytes / 1024f)
    else -> "$bytes B"
}

// ─── PLAYBACK ─────────────────────────────────────────────────────────────────
@Composable
fun MobilePlaybackSettingsCard(viewModel: MusicViewModel, backdrop: Backdrop) {
    val autoResume by viewModel.autoResume.collectAsState()
    val crossfade by viewModel.crossfadeSeconds.collectAsState()
    val sleepMinutes by viewModel.sleepTimerMinutes.collectAsState()
    val sleepRemaining by viewModel.sleepTimerRemainingSec.collectAsState()
    val playVideo by viewModel.playVideoForStreams.collectAsState()
    val maxQuality by viewModel.maxVideoQuality.collectAsState()

    SettingsCard("PLAYBACK", backdrop) {
        ToggleRow(
            "Auto-Resume Last Track",
            "Restores the last song and position when the app starts",
            autoResume,
            backdrop
        ) { viewModel.setAutoResume(it) }

        RowLabel(
            "Crossfade",
            if (crossfade > 0) "Blends songs over ${crossfade}s" else "Off — gapless playback"
        )
        ChipRow(listOf(0, 1, 2, 3, 5, 8), crossfade, { if (it == 0) "Off" else "${it}s" }) {
            viewModel.setCrossfadeSeconds(it)
        }

        RowLabel(
            "Sleep Timer",
            if (sleepMinutes > 0) "Pausing in ${formatClock(sleepRemaining)}" else "Off"
        )
        ChipRow(listOf(0, 15, 30, 45, 60), sleepMinutes, { if (it == 0) "Off" else "$it min" }) {
            viewModel.setSleepTimer(it)
        }

        ToggleRow(
            "Play Music Videos",
            "Shows the music video instead of artwork when a stream has one",
            playVideo,
            backdrop
        ) { viewModel.setPlayVideoForStreams(it) }

        if (playVideo) {
            RowLabel("Maximum Video Quality", "Resolution cap for smooth streaming")
            ChipRow(listOf("360p", "480p", "720p", "1080p"), maxQuality, { it }) {
                viewModel.setMaxVideoQuality(it)
            }
        }
    }
}

// ─── SPEED-COMPENSATED VOLUME ─────────────────────────────────────────────────
@Composable
fun MobileSpeedVolumeSettingsCard(viewModel: MusicViewModel, backdrop: Backdrop) {
    val mode by viewModel.scvcMode.collectAsState()
    val gainDb by viewModel.scvcGainDb.collectAsState()
    val speed by viewModel.currentSpeedKmh.collectAsState()
    val simulate by viewModel.testSpeedSimulation.collectAsState()
    val testSpeed by viewModel.testSpeedKmh.collectAsState()
    val effectiveSpeed = if (simulate) testSpeed else speed

    SettingsCard("SPEED VOLUME (SCVC)", backdrop) {
        RowLabel(
            "Speed-Compensated Volume",
            when (mode) {
                0 -> "Off — raises volume at highway speeds (Mild +3, Moderate +5, Dynamic +7 dB)"
                else -> "${effectiveSpeed.roundToInt()} km/h  •  +${"%.1f".format(gainDb)} dB now"
            }
        )
        ChipRow(
            listOf(0, 1, 2, 3), mode,
            { listOf("Off", "Mild", "Moderate", "Dynamic")[it] }
        ) { viewModel.setScvcMode(it) }

        if (mode > 0) {
            ToggleRow(
                "Simulate Vehicle Speed",
                "Test the volume curve without driving",
                simulate,
            backdrop
            ) { viewModel.setTestSpeedSimulation(it) }
            if (simulate) {
                LiquidSlider(
                    value = testSpeed,
                    onValueChange = { viewModel.setTestSpeedKmh(it) },
                    valueRange = 0f..160f,
                    backdrop = backdrop
                )
            }
        }
    }
}

// ─── LIBRARY, DOWNLOADS & STORAGE ─────────────────────────────────────────────
@Composable
fun MobileLibraryStorageSettingsCard(viewModel: MusicViewModel, backdrop: Backdrop) {
    val songs by viewModel.songs.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val isRefetching by viewModel.isRefetching.collectAsState()
    val refetchStatus by viewModel.refetchStatus.collectAsState()
    val preset by viewModel.downloadStoragePreset.collectAsState()
    val path by viewModel.downloadLocationPath.collectAsState()
    val autoDownload by viewModel.autoDownloadStreamTracks.collectAsState()
    val cacheSize by viewModel.artworkCacheSize.collectAsState()
    var pickerOpen by remember { mutableStateOf(false) }

    val presetLabel = when (preset) {
        StreamDownloadManager.PRESET_PUBLIC_MUSIC -> "Public Music"
        StreamDownloadManager.PRESET_PUBLIC_DOWNLOADS -> "Public Downloads"
        StreamDownloadManager.PRESET_APP_INTERNAL -> "App Storage"
        StreamDownloadManager.PRESET_CUSTOM -> "Custom Folder"
        else -> "Default Music"
    }

    SettingsCard("LIBRARY & STORAGE", backdrop) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            RowLabel(
                "Deep Rescan",
                refetchStatus ?: "${songs.size} songs • ${folders.size} folders (internal + USB)",
                Modifier.weight(1f).padding(end = 12.dp)
            )
            ActionButton(if (isRefetching) "Scanning…" else "Refetch", backdrop, enabled = !isRefetching) {
                viewModel.refetchSongs()
            }
        }

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(end = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Download Location", color = MobileTheme.text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text(presetLabel, color = MobileTheme.accent, fontSize = 11.sp)
                Text(path, color = MobileTheme.textSecondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            ActionButton("Change", backdrop) { pickerOpen = true }
        }

        ToggleRow(
            "Auto-Download Listened Songs",
            "Saves online tracks after you listen for at least a minute",
            autoDownload,
            backdrop
        ) { viewModel.setAutoDownloadStreamTracks(it) }

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            RowLabel(
                "Artwork Cache",
                "${formatBytes(cacheSize)} on disk",
                Modifier.weight(1f).padding(end = 12.dp)
            )
            ActionButton("Clear", backdrop) { viewModel.clearArtworkCache() }
        }
    }

    if (pickerOpen) {
        DownloadLocationDialog(
            currentPreset = preset,
            currentPath = path,
            onSelectPreset = { p, custom ->
                viewModel.setDownloadStorage(p, custom)
                pickerOpen = false
            },
            onTestPath = { viewModel.testCustomPathWritable(it) },
            onDismiss = { pickerOpen = false }
        )
    }
}

// ─── HEADUNIT (LANDSCAPE) ─────────────────────────────────────────────────────
@Composable
fun MobileHeadunitSettingsCard(viewModel: MusicViewModel, backdrop: Backdrop) {
    val autoHud by viewModel.autoHudEnabled.collectAsState()
    val hideDock by viewModel.hideBottomBar.collectAsState()
    val gridColumns by viewModel.gridColumns.collectAsState()

    SettingsCard("HEADUNIT (LANDSCAPE)", backdrop) {
        ToggleRow(
            "Auto HUD Drive Mode",
            "Switches to the Drive HUD after 20s of inactivity while playing",
            autoHud,
            backdrop
        ) { viewModel.setAutoHudEnabled(it) }
        ToggleRow(
            "Hide Bottom Player Dock",
            "Gives the library the full screen on the headunit",
            hideDock,
            backdrop
        ) { viewModel.setHideBottomBar(it) }
        RowLabel("Library Grid Columns", null)
        ChipRow(listOf(2, 3), gridColumns, { "$it columns" }) {
            if (it != gridColumns) viewModel.toggleGridColumns()
        }
        RowLabel(
            "Steering Mute Auto-Pause",
            "Pauses when the volume is muted from the steering wheel (always on)"
        )
    }
}

// ─── ABOUT: UPDATES & SYSTEM ──────────────────────────────────────────────────
@Composable
fun MobileAboutSettingsCard(viewModel: MusicViewModel, backdrop: Backdrop) {
    val context = LocalContext.current
    val state by viewModel.updateState.collectAsState()
    val installed = remember {
        "v${AppUpdateManager.getInstalledVersionName(context)} (Build ${AppUpdateManager.getInstalledVersionCode(context)})"
    }

    SettingsCard("ABOUT", backdrop) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            val (subtitle, action) = when (val s = state) {
                is UpdateState.Checking -> "Checking for updates…" to null
                is UpdateState.UpdateAvailable -> "Update available: v${s.latestVersion}" to "Install"
                is UpdateState.Downloading -> "Downloading… ${s.progressPercent}%" to null
                is UpdateState.ReadyToInstall -> "Downloaded v${s.version}" to "Install"
                is UpdateState.UpToDate -> "Up to date (v${s.currentVersion})" to "Check"
                is UpdateState.Error -> s.message to "Retry"
                else -> "Installed $installed" to "Check"
            }
            RowLabel("App Updates", subtitle, Modifier.weight(1f).padding(end = 12.dp))
            if (action != null) {
                ActionButton(action, backdrop) {
                    when (state) {
                        is UpdateState.UpdateAvailable, is UpdateState.ReadyToInstall ->
                            viewModel.downloadAndInstallUpdate(context)
                        else -> viewModel.checkForUpdates()
                    }
                }
            }
        }
        RowLabel(
            "Audio Engine",
            "C++ OpenSL ES • lock-free ring buffer • biquad DSP • SQLite WAL • NEON artwork cache"
        )
        RowLabel("Version", installed)
    }
}
