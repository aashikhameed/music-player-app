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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Tune
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
private val BandRowShape = RoundedCornerShape(10.dp)
private val InfoBadgeShape = RoundedCornerShape(8.dp)
private val ActionButtonShape = RoundedCornerShape(10.dp)

private val AccentGoldColor = Color(0xFFFFD54F)
private val MudCutWarningColor = Color(0xFFFF8A65)
private val VocalCyanColor = Color(0xFF4DD0E1)
private val ThemeSunColor = Color(0xFFFFD54F)
private val ThemeMoonColor = Color(0xFF7986CB)
private val RefetchGreenColor = Color(0xFF00E676)

private val BAND_LABELS = listOf(
    "60 Hz" to "Sub-Bass",
    "250 Hz" to "Mud Cut (Door Resonance)",
    "1.0 kHz" to "Vocal Body",
    "3.5 kHz" to "Vocal Presence & Clarity",
    "10 kHz" to "Treble Air & Sparkle"
)

private val PRESET_NAMES = listOf(
    "Flat",
    "Acoustic Clarity",
    "Vocal Separation",
    "Punchy Bass",
    "Rock Dynamic",
    "Custom"
)

private val HPF_NAMES = listOf(
    "Full Range (Off)",
    "60 Hz Cut",
    "80 Hz Cut (Door Speakers)"
)

private val TIME_ALIGN_NAMES = listOf(
    "Center Stage",
    "RHD Driver (India/UK)",
    "LHD Driver (US/EU)"
)

private val BASS_NORM_NAMES = listOf(
    "Natural Flat",
    "Mild Dynamic (+2dB)",
    "Deep Dynamic (+4dB)"
)

private val VIRTUAL_SURROUND_NAMES = listOf(
    "Stereo Standard (Off)",
    "Cabin Stage (+25%)",
    "Concert Wide (+50%)"
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
                                text = "Settings & Sound Preferences",
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

                // ── Content Layout: 2-Column in Landscape, Scrollable Column in Portrait ──
                if (isLandscape) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Left Column: Audio DSP & Equalizer Tuning (weight 0.52f)
                        Box(
                            modifier = Modifier
                                .weight(0.52f)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState())
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                AudioDspSection(viewModel = viewModel)
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }

                        // Right Column: Song Refetch, Theme, Playback, Cache & Diagnostics (weight 0.48f)
                        Box(
                            modifier = Modifier
                                .weight(0.48f)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState())
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                RefetchSection(viewModel = viewModel)
                                ThemeDisplaySection(viewModel = viewModel)
                                PlaybackPreferencesSection(viewModel = viewModel)
                                CacheManagementSection(viewModel = viewModel)
                                SystemInfoSection()
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                    }
                } else {
                    // Portrait Layout: Single vertical scrollable column
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        RefetchSection(viewModel = viewModel)
                        ThemeDisplaySection(viewModel = viewModel)
                        AudioDspSection(viewModel = viewModel)
                        PlaybackPreferencesSection(viewModel = viewModel)
                        CacheManagementSection(viewModel = viewModel)
                        SystemInfoSection()
                        Spacer(modifier = Modifier.height(24.dp))
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
 * Audio DSP & 5-Band Equalizer Settings Section.
 */
/**
 * Reusable Sound Preset Chip with custom pill background and star for recommended preset.
 */
@Composable
private fun PresetChip(
    name: String,
    isSelected: Boolean,
    isEnabled: Boolean,
    isRecommended: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                if (isSelected && isEnabled) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                shape = ChipPillShape
            )
            .clickable(enabled = isEnabled, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (isRecommended) {
                Icon(
                    imageVector = Icons.Rounded.Star,
                    contentDescription = null,
                    tint = if (isSelected) AccentGoldColor else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(11.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
            }
            Text(
                text = name,
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected && isEnabled) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Audio DSP & 5-Band Equalizer Settings Section.
 */
@Composable
private fun AudioDspSection(viewModel: MusicViewModel) {
    val isEnabled by viewModel.isEqualizerEnabled.collectAsState()
    val activePreset by viewModel.equalizerPreset.collectAsState()
    val bandGains by viewModel.equalizerBands.collectAsState()
    val hpfMode by viewModel.equalizerHpfMode.collectAsState()

    val capsuleBg = AppGradients.capsule(isActive = false)
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
            // Section Title + Master Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.GraphicEq,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Acoustic Equalizer & Automotive DSP",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isEnabled) "ACTIVE" else "BYPASS",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { viewModel.setEqualizerEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            // Sound Profiles — 2 rows of 3 equal-width chips (never overflows horizontally!)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Row 1: Flat, Acoustic Clarity, Vocal Separation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (index in 0..2) {
                        PresetChip(
                            name = PRESET_NAMES[index],
                            isSelected = (activePreset == index),
                            isEnabled = isEnabled,
                            isRecommended = (index == 1),
                            onClick = { viewModel.setEqualizerPreset(index) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Row 2: Punchy Bass, Rock Dynamic, Custom
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (index in 3..5) {
                        PresetChip(
                            name = PRESET_NAMES[index],
                            isSelected = (activePreset == index),
                            isEnabled = isEnabled,
                            isRecommended = false,
                            onClick = { viewModel.setEqualizerPreset(index) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Subsonic HPF Selector
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(brush = capsuleBg, shape = BandRowShape)
                    .border(width = 0.6.dp, brush = cardBorder, shape = BandRowShape)
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "Subsonic HPF (Door Speaker & Sub Protection):",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    HPF_NAMES.forEachIndexed { index, label ->
                        val isSelected = (hpfMode == index)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    if (isSelected && isEnabled) MaterialTheme.colorScheme.secondary
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                    shape = ChipPillShape
                                )
                                .clickable(enabled = isEnabled) {
                                    viewModel.setEqualizerHpfMode(index)
                                }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 9.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected && isEnabled) MaterialTheme.colorScheme.onSecondary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Driver Soundstage Time Alignment (Automotive DSP Micro-Delay)
            val timeAlign by viewModel.timeAlignmentMode.collectAsState()
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = "Driver Soundstage Time Alignment (Cabin Micro-Delay):",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TIME_ALIGN_NAMES.forEachIndexed { index, name ->
                        val isSelected = (timeAlign == index)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    if (isSelected && isEnabled) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                    shape = ChipPillShape
                                )
                                .clickable(enabled = isEnabled) { viewModel.setTimeAlignment(index) }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = name,
                                fontSize = 9.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected && isEnabled) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Bass Normalizer & Loudness Contour
            val bassNorm by viewModel.bassNormalizerMode.collectAsState()
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = "Acoustic Bass Normalizer & Dynamic Loudness Contour:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    BASS_NORM_NAMES.forEachIndexed { index, name ->
                        val isSelected = (bassNorm == index)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    if (isSelected && isEnabled) MaterialTheme.colorScheme.secondary
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                    shape = ChipPillShape
                                )
                                .clickable(enabled = isEnabled) { viewModel.setBassNormalizer(index) }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = name,
                                fontSize = 9.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected && isEnabled) MaterialTheme.colorScheme.onSecondary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Virtual Surround / Cabin Soundstage Widener Row
            val surroundMode by viewModel.virtualSurroundMode.collectAsState()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Virtual Surround & Cabin Soundstage Widener:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    VIRTUAL_SURROUND_NAMES.forEachIndexed { index, name ->
                        val isSelected = (surroundMode == index)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    if (isSelected && isEnabled) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                    shape = ChipPillShape
                                )
                                .clickable(enabled = isEnabled) { viewModel.setVirtualSurroundMode(index) }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = name,
                                fontSize = 9.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected && isEnabled) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // 5 EQ Band Sliders
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                BAND_LABELS.forEachIndexed { bandIndex, (freqLabel, roleLabel) ->
                    val gain = bandGains.getOrElse(bandIndex) { 0.0f }
                    val isMudZone = (bandIndex == 1) // 250 Hz Mud Cut
                    val isVocalZone = (bandIndex == 2 || bandIndex == 3)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (isMudZone) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.12f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                                shape = BandRowShape
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.width(96.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = freqLabel,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp,
                                    color = if (isMudZone) MudCutWarningColor
                                            else if (isVocalZone) VocalCyanColor
                                            else MaterialTheme.colorScheme.onSurface
                                )
                                if (isMudZone) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .background(MudCutWarningColor.copy(alpha = 0.2f), shape = InfoBadgeShape)
                                            .padding(horizontal = 3.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = "Cut",
                                            fontSize = 7.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MudCutWarningColor
                                        )
                                    }
                                }
                            }
                            Text(
                                text = roleLabel,
                                fontSize = 8.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Slider(
                            value = gain,
                            onValueChange = { newGain ->
                                viewModel.setEqualizerBandGain(bandIndex, (newGain * 2).toInt() / 2f)
                            },
                            valueRange = -10f..10f,
                            steps = 39,
                            enabled = isEnabled,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp),
                            colors = SliderDefaults.colors(
                                thumbColor = if (isMudZone) MudCutWarningColor else MaterialTheme.colorScheme.primary,
                                activeTrackColor = if (isMudZone) MudCutWarningColor else MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            )
                        )

                        Box(
                            modifier = Modifier.width(48.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            val gainText = if (gain > 0f) "+${String.format("%.1f", gain)}" else String.format("%.1f", gain)
                            Text(
                                text = "$gainText dB",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (gain < 0f && isMudZone) MudCutWarningColor
                                        else if (gain > 0f) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Headroom status text
            Text(
                text = "• -2.5 dB digital headroom active (eliminates DAC clipping into external processor)",
                fontSize = 9.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
            )
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
