package com.aashik.music.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Tune
import kotlin.math.roundToInt
import androidx.compose.material.icons.rounded.ViewColumn
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aashik.music.theme.AppGradients
import com.aashik.music.viewmodel.MusicViewModel

// Precomputed shapes hoisted to top-level constants for Mali GPU fill rate & zero GC allocation
private val SettingsContainerShape = RoundedCornerShape(18.dp)
private val CardSectionShape = RoundedCornerShape(14.dp)
private val ChipPillShape = RoundedCornerShape(10.dp)
private val InfoBadgeShape = RoundedCornerShape(8.dp)
private val ActionButtonShape = RoundedCornerShape(10.dp)

private val ThemeSunColor = Color(0xFFFFD54F)
private val ThemeMoonColor = Color(0xFF7986CB)
private val RefetchGreenColor = Color(0xFF00E676)

private val PRESET_NAMES = listOf(
    "Flat",
    "Acoustic Clarity",
    "Vocal Separation",
    "Punchy Bass",
    "Rock Dynamic",
    "Custom"
)

private val CROSSFADE_OPTIONS = listOf(0, 1, 2, 3, 5, 8)

private val SLEEP_TIMER_OPTIONS = listOf(0, 15, 30, 45, 60)

private fun formatSleepTimer(sec: Int): String {
    val m = sec / 60
    val s = sec % 60
    return String.format("%02d:%02d", m, s)
}

private fun formatCacheSize(bytes: Long): String {
    if (bytes <= 0) return "0 KB"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format("%.1f KB", kb)
    val mb = kb / 1024.0
    return String.format("%.2f MB", mb)
}

/**
 * Unified Automotive Settings Screen.
 * Consolidates Song Refetching, Lite/Dark Theme, Equalizer & DSP tuning, Cache Cleaning,
 * Playback Preferences, Grid Layout, and Hardware Diagnostics.
 * Optimized for Automotive Headunit (1280x720 @ 160 dpi).
 */
@Composable
fun SettingsScreen(
    viewModel: MusicViewModel,
    onClose: () -> Unit,
    onOpenEqualizer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val cardBg = AppGradients.card(isActive = false)
    val cardBorder = AppGradients.border(isActive = false)

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (!isLandscape) {
                        Modifier
                            .statusBarsPadding()
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    } else {
                        Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                    }
                )
                .background(brush = cardBg, shape = SettingsContainerShape)
                .border(width = 1.dp, brush = cardBorder, shape = SettingsContainerShape)
                .padding(horizontal = if (isLandscape) 14.dp else 10.dp, vertical = if (isLandscape) 10.dp else 8.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // ── Top Header Navigation Bar ──
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Back to Library",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )

                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                text = "Settings & Preferences",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Automotive Audio & Headunit Setup",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = onClose,
                        shape = ActionButtonShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text(
                            text = "Done",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // ── Unified Single Scrollable Column (No Split Screen) ──
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    EqualizerNavigationCard(
                        viewModel = viewModel,
                        onClick = onOpenEqualizer
                    )
                    SpeedCompensatedVolumeSection(viewModel = viewModel)
                    RefetchSection(viewModel = viewModel)
                    ThemeDisplaySection(viewModel = viewModel)
                    PlaybackPreferencesSection(viewModel = viewModel)
                    CacheManagementSection(viewModel = viewModel)
                    SystemInfoSection()
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

private val SCVC_MODES = listOf(
    0 to "Off",
    1 to "Mild (+3 dB)",
    2 to "Moderate (+5 dB)",
    3 to "Dynamic (+7 dB)"
)

/**
 * Speed-Compensated Volume Control (SCVC / Auto-Volume) Section.
 * Dynamically adjusts volume curve based on vehicle GPS driving speed.
 */
@Composable
private fun SpeedCompensatedVolumeSection(viewModel: MusicViewModel) {
    val scvcMode by viewModel.scvcMode.collectAsState()
    val currentSpeedKmh by viewModel.currentSpeedKmh.collectAsState()
    val scvcGainDb by viewModel.scvcGainDb.collectAsState()
    val testSimulation by viewModel.testSpeedSimulation.collectAsState()
    val testSpeedKmh by viewModel.testSpeedKmh.collectAsState()

    val cardBorder = AppGradients.border(isActive = scvcMode > 0)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(width = 0.8.dp, brush = cardBorder, shape = CardSectionShape),
        shape = CardSectionShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Speed,
                        contentDescription = null,
                        tint = if (scvcMode > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Speed-Compensated Volume (SCVC)",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Dynamic Gain Badge
                val badgeText = if (scvcMode == 0) {
                    "Disabled"
                } else if (scvcGainDb > 0.05f) {
                    "+${String.format("%.1f", scvcGainDb)} dB Boost"
                } else {
                    "Monitoring (≤ 15 km/h)"
                }
                Box(
                    modifier = Modifier
                        .background(
                            if (scvcMode > 0 && scvcGainDb > 0.05f) RefetchGreenColor.copy(alpha = 0.18f)
                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            shape = InfoBadgeShape
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = badgeText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (scvcMode > 0 && scvcGainDb > 0.05f) RefetchGreenColor
                                else MaterialTheme.colorScheme.primary,
                        maxLines = 1
                    )
                }
            }

            Text(
                text = "Automatically elevates volume curve at highway driving speeds to maintain clarity against tire, road, and cabin wind noise.",
                fontSize = 10.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Mode Selector Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SCVC_MODES.forEach { (mode, label) ->
                    val isSelected = scvcMode == mode
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setScvcMode(mode) },
                        label = { Text(text = label, fontSize = 11.sp) },
                        shape = ChipPillShape,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.height(32.dp)
                    )
                }
            }

            // Real-time speed and gain readout bar
            if (scvcMode > 0) {
                val effectiveSpeed = if (testSimulation) testSpeedKmh else currentSpeedKmh
                Surface(
                    shape = ChipPillShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        if (scvcGainDb > 0.05f) RefetchGreenColor else Color(0xFFFFB300),
                                        shape = CircleShape
                                    )
                            )
                            Text(
                                text = "Current Vehicle Speed: ${effectiveSpeed.roundToInt()} km/h",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = if (effectiveSpeed <= 15f) "Below 15 km/h threshold" else "Active DSP Compensation",
                            fontSize = 10.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Bench / Desk Test Simulation Controls
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Simulate Vehicle Speed (Test Bench Mode)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Slide speed to test real-time DSP volume compensation without moving vehicle",
                                fontSize = 9.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Switch(
                            checked = testSimulation,
                            onCheckedChange = { viewModel.setTestSpeedSimulation(it) },
                            modifier = Modifier.height(28.dp),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                            )
                        )
                    }

                    if (testSimulation) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "0 km/h",
                                fontSize = 9.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Slider(
                                value = testSpeedKmh,
                                onValueChange = { viewModel.setTestSpeedKmh(it) },
                                valueRange = 0f..160f,
                                steps = 31,
                                modifier = Modifier.weight(1f).height(24.dp),
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary
                                )
                            )
                            Text(
                                text = "160 km/h",
                                fontSize = 9.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Song Refetch & Library Synchronization Section.
 * Rescans physical storage & USB partitions and replaces stale Room entries.
 */
@Composable
private fun RefetchSection(viewModel: MusicViewModel) {
    val isRefetching by viewModel.isRefetching.collectAsState()
    val refetchStatus by viewModel.refetchStatus.collectAsState()
    val songs by viewModel.songs.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val cardBorder = AppGradients.border(isActive = false)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(width = 0.8.dp, brush = cardBorder, shape = CardSectionShape),
        shape = CardSectionShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Sync,
                        contentDescription = null,
                        tint = RefetchGreenColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Song Refetch & Media Scan",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Box(
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            shape = InfoBadgeShape
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${songs.size} Songs • ${folders.size} Folders",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1
                    )
                }
            }

            Text(
                text = "Deep-scans internal storage and plugged-in USB thumb drives to detect newly added or edited audio files.",
                fontSize = 10.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (refetchStatus != null) {
                Text(
                    text = refetchStatus.orEmpty(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isRefetching) MaterialTheme.colorScheme.primary else RefetchGreenColor
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = { viewModel.refetchSongs() },
                    enabled = !isRefetching,
                    shape = ActionButtonShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.height(36.dp)
                ) {
                    if (isRefetching) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Scanning Storage...",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Refetch All Songs",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Lite / Dark Mode & Display Layout Section.
 */
@Composable
private fun ThemeDisplaySection(viewModel: MusicViewModel) {
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    val gridColumns by viewModel.gridColumns.collectAsState()
    val cardBorder = AppGradients.border(isActive = false)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(width = 0.8.dp, brush = cardBorder, shape = CardSectionShape),
        shape = CardSectionShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Tune,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Theme & Display Layout",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Lite / Dark Mode Selector Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 6.dp)) {
                    Text(
                        text = "App Theme Mode",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isDarkTheme) "Dark Mode (Infotainment Night Mode)" else "Lite Mode (Day Driving High Contrast)",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Lite Mode Chip
                    Box(
                        modifier = Modifier
                            .background(
                                if (!isDarkTheme) ThemeSunColor
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                shape = ChipPillShape
                            )
                            .clickable { if (isDarkTheme) viewModel.setThemeMode(false) }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.LightMode,
                                contentDescription = null,
                                tint = if (!isDarkTheme) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Lite",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (!isDarkTheme) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Dark Mode Chip
                    Box(
                        modifier = Modifier
                            .background(
                                if (isDarkTheme) ThemeMoonColor
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                shape = ChipPillShape
                            )
                            .clickable { if (!isDarkTheme) viewModel.setThemeMode(true) }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.DarkMode,
                                contentDescription = null,
                                tint = if (isDarkTheme) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Dark",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkTheme) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Grid Column Setting Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 6.dp)) {
                    Text(
                        text = "Library Grid Columns",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (gridColumns == 2) "2 Columns (Optimal touch targets)" else "3 Columns (Compact Layout)",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(
                        modifier = Modifier
                            .background(
                                if (gridColumns == 2) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                shape = ChipPillShape
                            )
                            .clickable { if (gridColumns != 2) viewModel.toggleGridColumns() }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.GridView,
                                contentDescription = null,
                                tint = if (gridColumns == 2) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "2 Cols",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (gridColumns == 2) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .background(
                                if (gridColumns == 3) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                shape = ChipPillShape
                            )
                            .clickable { if (gridColumns != 3) viewModel.toggleGridColumns() }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.ViewColumn,
                                contentDescription = null,
                                tint = if (gridColumns == 3) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "3 Cols",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (gridColumns == 3) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Automotive Playback Behaviors Section.
 */
@Composable
private fun PlaybackPreferencesSection(viewModel: MusicViewModel) {
    val autoResume by viewModel.autoResume.collectAsState()
    val cardBorder = AppGradients.border(isActive = false)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(width = 0.8.dp, brush = cardBorder, shape = CardSectionShape),
        shape = CardSectionShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Automotive Playback Behaviors",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Auto-Resume Last Played Track",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Restores position and last song when the headunit boots up",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Switch(
                    checked = autoResume,
                    onCheckedChange = { viewModel.setAutoResume(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
            }

            val autoHudEnabled by viewModel.autoHudEnabled.collectAsState()
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Auto HUD Drive Mode",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Automatically switches to full-screen Drive HUD after 20s of driving inactivity",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Switch(
                    checked = autoHudEnabled,
                    onCheckedChange = { viewModel.setAutoHudEnabled(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
            }

            val hideBottomBar by viewModel.hideBottomBar.collectAsState()
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Hide Bottom Player Dock",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Maximizes screen area for media library. Shows sleek floating pill to reveal dock.",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Switch(
                    checked = hideBottomBar,
                    onCheckedChange = { viewModel.setHideBottomBar(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
            }

            val playVideoForStreams by viewModel.playVideoForStreams.collectAsState()
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Play Music Videos",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Plays the music video instead of album art when a YouTube or video stream is available",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Switch(
                    checked = playVideoForStreams,
                    onCheckedChange = { viewModel.setPlayVideoForStreams(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Steering Mute Auto-Pause",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Seamlessly pauses audio when hardware volume stream is muted from steering wheel",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            shape = InfoBadgeShape
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Hardware Linked",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Parked Battery-Saver / Sleep Timer
            val sleepTimerMinutes by viewModel.sleepTimerMinutes.collectAsState()
            val sleepTimerRemainingSec by viewModel.sleepTimerRemainingSec.collectAsState()

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Column {
                    Text(
                        text = "Parked Battery-Saver (Sleep Timer)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (sleepTimerMinutes > 0)
                            "Auto-pausing in ${formatSleepTimer(sleepTimerRemainingSec)} to protect car battery"
                        else
                            "Auto-pauses playback when listening with engine off",
                        fontSize = 10.sp,
                        color = if (sleepTimerMinutes > 0) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SLEEP_TIMER_OPTIONS.forEach { mins ->
                        val isSelected = (sleepTimerMinutes == mins)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    shape = ChipPillShape
                                )
                                .clickable { viewModel.setSleepTimer(mins) }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (mins == 0) "Off" else "${mins}m",
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Crossfade Between Songs
            val crossfadeSeconds by viewModel.crossfadeSeconds.collectAsState()

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Column {
                    Text(
                        text = "Crossfade Between Songs",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (crossfadeSeconds > 0)
                            "Smoothly blends songs over ${crossfadeSeconds}s (Default: 3s)"
                        else
                            "No crossfade (gapless standard playback)",
                        fontSize = 10.sp,
                        color = if (crossfadeSeconds > 0) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CROSSFADE_OPTIONS.forEach { sec ->
                        val isSelected = (crossfadeSeconds == sec)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    shape = ChipPillShape
                                )
                                .clickable { viewModel.setCrossfadeSeconds(sec) }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = when (sec) {
                                    0 -> "Off"
                                    3 -> "3s (Def)"
                                    else -> "${sec}s"
                                },
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Cache & Storage Maintenance Section.
 */
@Composable
private fun CacheManagementSection(viewModel: MusicViewModel) {
    val cacheSize by viewModel.artworkCacheSize.collectAsState()
    val cardBorder = AppGradients.border(isActive = false)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(width = 0.8.dp, brush = cardBorder, shape = CardSectionShape),
        shape = CardSectionShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.DeleteSweep,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Artwork & Disk Cache Maintenance",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = "Artwork Disk Cache: ${formatCacheSize(cacheSize)}",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "NEON-compressed JPEG thumbnails (64x64 RGB_565) + .noart markers",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Button(
                    onClick = { viewModel.clearArtworkCache() },
                    shape = ActionButtonShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CleaningServices,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Clear Cache",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Acoustic Equalizer & Automotive DSP Navigation Card.
 * Prominently surfaces the active equalizer profile and links to the dedicated EqualizerScreen.
 */
@Composable
private fun EqualizerNavigationCard(
    viewModel: MusicViewModel,
    onClick: () -> Unit
) {
    val isEnabled by viewModel.isEqualizerEnabled.collectAsState()
    val activePreset by viewModel.equalizerPreset.collectAsState()
    val cardBorder = AppGradients.border(isActive = false)
    val presetName = PRESET_NAMES.getOrElse(activePreset) { "Custom" }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardSectionShape)
            .border(width = 0.8.dp, brush = cardBorder, shape = CardSectionShape)
            .clickable(onClick = onClick),
        shape = CardSectionShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f, fill = false),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.GraphicEq,
                        contentDescription = "Equalizer",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Acoustic Equalizer & DSP",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Box(
                            modifier = Modifier
                                .background(
                                    color = if (isEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                            else MaterialTheme.colorScheme.surfaceVariant,
                                    shape = InfoBadgeShape
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isEnabled) "ACTIVE • $presetName" else "BYPASS",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "5-Band Graphic EQ • Subsonic HPF • Seat Time Alignment • Bass Normalizer",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onClick,
                shape = ActionButtonShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.height(34.dp)
            ) {
                Text(
                    text = "Configure",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
} 

/**
 * Hardware, Native Engine & Performance Info Section.
 */
@Composable
private fun SystemInfoSection() {
    val cardBorder = AppGradients.border(isActive = false)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(width = 0.8.dp, brush = cardBorder, shape = CardSectionShape),
        shape = CardSectionShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Hardware & Audio Engine Diagnostics",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = "Platform: Automotive Headunit (ARM64, Mali GPU)",
                fontSize = 9.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Audio Core: C++ OpenSL ES 1.1 + Direct Form II Transposed Biquad DSP",
                fontSize = 9.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Jitter Buffer: 96 KB Lock-Free SPSC RingBuffer (~550ms Headroom)",
                fontSize = 9.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Storage & Cache: SQLite WAL Mode + NEON JPEG Hardware Artwork Cache",
                fontSize = 9.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
