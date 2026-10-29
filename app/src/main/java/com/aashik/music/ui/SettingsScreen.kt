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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.ViewColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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

private val AlpineGoldColor = Color(0xFFFFD54F)
private val MudCutWarningColor = Color(0xFFFF8A65)
private val VocalCyanColor = Color(0xFF4DD0E1)
private val ThemeSunColor = Color(0xFFFFD54F)
private val ThemeMoonColor = Color(0xFF7986CB)

private val BAND_LABELS = listOf(
    "60 Hz" to "Sub-Bass",
    "250 Hz" to "Mud Cut (Door Resonance)",
    "1.0 kHz" to "Vocal Body",
    "3.5 kHz" to "Vocal Presence & Clarity",
    "10 kHz" to "Treble Air & Sparkle"
)

private val PRESET_NAMES = listOf(
    "Flat",
    "Alpine Clarity",
    "Vocal Separation",
    "Punchy Bass",
    "Rock Dynamic",
    "Custom"
)

private val HPF_NAMES = listOf(
    "Full Range (Off)",
    "60 Hz Cut",
    "80 Hz Cut (Alpine)"
)

/**
 * Unified Automotive Settings Screen.
 * Consolidates Equalizer & DSP tuning, Theme, Grid Columns, Library Rescan, and Hardware Info.
 * Optimized for Blaupunkt Santa Rosa 985 (1280x720 @ 160 dpi).
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(brush = cardBg, shape = SettingsContainerShape)
            .border(width = 1.dp, brush = cardBorder, shape = SettingsContainerShape)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        // ── Top Header Navigation Bar ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
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

                Column {
                    Text(
                        text = "Settings & Sound Preferences",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Blaupunkt Santa Rosa 985 Automotive Setup",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

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
                // Left Column: Audio DSP & Equalizer Tuning (weight 0.58f)
                Box(
                    modifier = Modifier
                        .weight(0.58f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                ) {
                    AudioDspSection(viewModel = viewModel)
                }

                // Right Column: Display, Library & System Info (weight 0.42f)
                Box(
                    modifier = Modifier
                        .weight(0.42f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        DisplaySection(viewModel = viewModel)
                        LibrarySection(viewModel = viewModel)
                        SystemInfoSection()
                    }
                }
            }
        } else {
            // Portrait Layout: Single vertical scrollable column
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AudioDspSection(viewModel = viewModel)
                DisplaySection(viewModel = viewModel)
                LibrarySection(viewModel = viewModel)
                SystemInfoSection()
                Spacer(modifier = Modifier.height(16.dp))
            }
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
                        text = "Acoustic Equalizer & Alpine DSP",
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

            // Sound Profiles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PRESET_NAMES.forEachIndexed { index, name ->
                    val isSelected = (activePreset == index)
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setEqualizerPreset(index) },
                        enabled = isEnabled,
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (index == 1) { // Alpine Clarity
                                    Icon(
                                        imageVector = Icons.Rounded.Star,
                                        contentDescription = null,
                                        tint = if (isSelected) AlpineGoldColor else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                }
                                Text(
                                    text = name,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        },
                        shape = ChipPillShape,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }

            // Subsonic HPF Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(brush = capsuleBg, shape = BandRowShape)
                    .border(width = 0.6.dp, brush = cardBorder, shape = BandRowShape)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Subsonic HPF:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    HPF_NAMES.forEachIndexed { index, label ->
                        val isSelected = (hpfMode == index)
                        Box(
                            modifier = Modifier
                                .background(
                                    if (isSelected && isEnabled) MaterialTheme.colorScheme.secondary
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    shape = ChipPillShape
                                )
                                .clickable(enabled = isEnabled) {
                                    viewModel.setEqualizerHpfMode(index)
                                }
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = label,
                                fontSize = 9.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected && isEnabled) MaterialTheme.colorScheme.onSecondary
                                else MaterialTheme.colorScheme.onSurfaceVariant
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
                            .padding(horizontal = 8.dp, vertical = 1.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.width(140.dp)) {
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
                                            text = "Mud Cut",
                                            fontSize = 7.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MudCutWarningColor
                                        )
                                    }
                                }
                            }
                            Text(
                                text = roleLabel,
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
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
                                .padding(horizontal = 6.dp),
                            colors = SliderDefaults.colors(
                                thumbColor = if (isMudZone) MudCutWarningColor else MaterialTheme.colorScheme.primary,
                                activeTrackColor = if (isMudZone) MudCutWarningColor else MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            )
                        )

                        Box(
                            modifier = Modifier.width(50.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            val gainText = if (gain > 0f) "+${String.format("%.1f", gain)}" else String.format("%.1f", gain)
                            Text(
                                text = "$gainText dB",
                                fontSize = 10.5.sp,
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
 * Display & Interface Options Section.
 */
@Composable
private fun DisplaySection(viewModel: MusicViewModel) {
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
                    text = "Display & Interface",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Theme Setting Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "App Theme",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isDarkTheme) "Dark Mode (Infotainment Night Mode)" else "Light Mode (Day Driving)",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (isDarkTheme) Icons.Rounded.DarkMode else Icons.Rounded.LightMode,
                        contentDescription = null,
                        tint = if (isDarkTheme) ThemeMoonColor else ThemeSunColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Switch(
                        checked = isDarkTheme,
                        onCheckedChange = { viewModel.toggleTheme() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ThemeMoonColor,
                            checkedTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            // Grid Column Setting Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Library Grid Columns",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (gridColumns == 2) "2 Columns (Optimal for Santa Rosa 985 touch)" else "3 Columns (Compact Layout)",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.GridView,
                                contentDescription = null,
                                tint = if (gridColumns == 2) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "2 Columns",
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
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.ViewColumn,
                                contentDescription = null,
                                tint = if (gridColumns == 3) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "3 Columns",
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
 * Media Library & Storage Management Section.
 */
@Composable
private fun LibrarySection(viewModel: MusicViewModel) {
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
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.MusicNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Library & Storage",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column {
                        Text(
                            text = "${songs.size}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Tracks",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Column {
                        Text(
                            text = "${folders.size}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "Folders",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Button(
                    onClick = { viewModel.loadSongs() },
                    shape = ActionButtonShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Rescan Media",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
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
                    text = "Hardware & Audio Engine",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = "Target Hardware: Blaupunkt Santa Rosa 985 (ARM Cortex-A53, Mali GPU)",
                fontSize = 9.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Audio Core: C++ OpenSL ES 1.1 + Direct Form II Transposed Biquad DSP",
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
