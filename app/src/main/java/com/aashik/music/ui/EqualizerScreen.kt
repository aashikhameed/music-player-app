package com.aashik.music.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.ArrowDropUp
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.SpatialAudio
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Tune
import com.aashik.music.ui.designsystem.DropdownMenu
import com.aashik.music.ui.designsystem.DropdownMenuItem
import com.aashik.music.ui.designsystem.FilterChip
import com.aashik.music.ui.designsystem.FilterChipDefaults
import com.aashik.music.ui.designsystem.Icon
import com.aashik.music.ui.designsystem.MaterialTheme
import com.aashik.music.ui.designsystem.Slider
import com.aashik.music.ui.designsystem.SliderDefaults
import com.aashik.music.ui.designsystem.Surface
import com.aashik.music.ui.designsystem.Switch
import com.aashik.music.ui.designsystem.SwitchDefaults
import com.aashik.music.ui.designsystem.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.heightIn
import com.aashik.music.model.EqualizerPresetRegistry
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aashik.music.ui.components.BalanceFaderCard
import com.aashik.music.ui.components.DspCard
import com.aashik.music.ui.components.EqualizerCurveCanvas
import com.aashik.music.viewmodel.MusicViewModel

import androidx.compose.ui.geometry.Offset

// Top-level precomputed shapes matching Material 3 specifications
private val EqCardShape = RoundedCornerShape(12.dp)
private val EqChipShape = RoundedCornerShape(8.dp)
private val GridBorderShape = RoundedCornerShape(12.dp)
private val EqCurveContainerShape = RoundedCornerShape(8.dp)
private val EqCurveBg = Color(0xFF10141D)
private val EqCurveBorder = Color(0xFF1E293B)
private val EqCurveGridColor = Color(0x3394A3B8)
private val EqCurveLineColor = Color(0xFF00E5FF)
private val EqCurveBypassColor = Color(0x6694A3B8)

private val BAND_INFO = listOf(
    "31 Hz" to "Sub-Bass Depth",
    "62 Hz" to "Kick & Bass Punch",
    "125 Hz" to "Upper Bass Warmth",
    "250 Hz" to "Door Resonance (Mud Cut)",
    "500 Hz" to "Low Midrange Body",
    "1.0 kHz" to "Vocal Body & Presence",
    "2.0 kHz" to "Vocal Clarity & Definition",
    "4.0 kHz" to "Attack & Percussion",
    "8.0 kHz" to "Treble Sparkle",
    "16 kHz" to "Air & Shimmer"
)

private val EqDropdownShape = RoundedCornerShape(12.dp)

private val HPF_LABELS = listOf(
    "Full Range (Off)",
    "60 Hz Cut (Door Protection)",
    "80 Hz Cut (Subwoofer Hand-off)"
)

private val SURROUND_LABELS = listOf(
    "Off (Stereo)",
    "Studio Cabin",
    "Wide Soundstage"
)

private val TIME_ALIGNMENT_LABELS = listOf(
    "Center (Balanced)",
    "Driver Seat Focus (RHD)",
    "Driver Seat Focus (LHD)"
)

private val BASS_BOOST_LABELS = listOf(
    "Off",
    "Mild Punch",
    "Deep Sub-Bass"
)

/**
 * Dedicated Automotive Equalizer & Acoustic DSP Screen.
 * Hardware-tuned for vehicle audio acoustics, speaker resonance compensation,
 * and high-fidelity native OpenSL ES audio processing.
 */
@Composable
fun EqualizerScreen(
    viewModel: MusicViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isEnabled by viewModel.isEqualizerEnabled.collectAsState()
    val activePreset by viewModel.equalizerPreset.collectAsState()
    val bandGains by viewModel.equalizerBands.collectAsState()
    val hpfMode by viewModel.equalizerHpfMode.collectAsState()
    val surroundMode by viewModel.virtualSurroundMode.collectAsState()
    val timeAlignmentMode by viewModel.timeAlignmentMode.collectAsState()
    val bassMode by viewModel.bassNormalizerMode.collectAsState()

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
                // ── Top Header Navigation Bar (Material 3 Standardized) ──
                AppTopBar(
                    title = "Equalizer",
                    onBack = onClose,
                    actions = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                text = if (isEnabled) "ACTIVE" else "BYPASS",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Switch(
                                checked = isEnabled,
                                onCheckedChange = { viewModel.setEqualizerEnabled(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                                )
                            )
                        }
                    }
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.GraphicEq,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Acoustic Tuning • OpenSL ES Native Engine",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // ── Section 1: Presets Dropdown ──
                DspCard(title = "Acoustic Tuning Presets", icon = Icons.Rounded.Tune) {
                    var isDropdownExpanded by remember { mutableStateOf(false) }
                    val currentPresetName = EqualizerPresetRegistry.getPresetName(activePreset)

                    Box(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(EqDropdownShape)
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, EqDropdownShape)
                                .clickable(enabled = isEnabled) { isDropdownExpanded = true }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.GraphicEq,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = currentPresetName,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (activePreset == EqualizerPresetRegistry.PRESET_CUSTOM) "Custom manual 10-band profile"
                                               else "Hardware DSP profile (${activePreset + 1} of ${EqualizerPresetRegistry.PRESET_NAMES.size})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Icon(
                                imageVector = if (isDropdownExpanded) Icons.Rounded.ArrowDropUp else Icons.Rounded.ArrowDropDown,
                                contentDescription = "Select Preset",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = isDropdownExpanded,
                            onDismissRequest = { isDropdownExpanded = false },
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .heightIn(max = 380.dp)
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        ) {
                            EqualizerPresetRegistry.PRESET_NAMES.forEachIndexed { index, name ->
                                val isSelected = activePreset == index
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = name,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    trailingIcon = {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Rounded.Check,
                                                contentDescription = "Selected",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    },
                                    onClick = {
                                        viewModel.setEqualizerPreset(index)
                                        isDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    EqualizerCurveCanvas(
                        bandGains = bandGains,
                        isEnabled = isEnabled
                    )
                }

                // ── Section 2: 10-Band Graphic Equalizer (Custom only) ──
                if (activePreset == EqualizerPresetRegistry.PRESET_CUSTOM) {
                    Spacer(modifier = Modifier.height(10.dp))

                    DspCard(title = "10-Band Custom Equalizer (-12 dB to +12 dB)", icon = Icons.Rounded.GraphicEq) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            BAND_INFO.forEachIndexed { index, (freq, description) ->
                                val gainDb = bandGains.getOrElse(index) { 0f }
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(EqCardShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = freq,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "• $description",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Text(
                                            text = String.format("%+.1f dB", gainDb),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = if (gainDb > 0.05f) MaterialTheme.colorScheme.primary
                                                    else if (gainDb < -0.05f) Color(0xFFFF8A65)
                                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Slider(
                                        value = gainDb,
                                        onValueChange = { newGain ->
                                            viewModel.setEqualizerBandGain(index, newGain)
                                        },
                                        valueRange = -12f..12f,
                                        steps = 23,
                                        enabled = isEnabled,
                                        colors = SliderDefaults.colors(
                                            thumbColor = MaterialTheme.colorScheme.primary,
                                            activeTrackColor = MaterialTheme.colorScheme.primary,
                                            inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                                        ),
                                        modifier = Modifier.height(26.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // ── Section 3: 4-Way Balance & Fader Acoustic Grid ──
                BalanceFaderCard(viewModel = viewModel)

                Spacer(modifier = Modifier.height(10.dp))

                // ── Section 4: High-Pass Filter (Door Protection) ──
                DspCard(title = "Door Speaker High-Pass Filter (HPF)", icon = Icons.Rounded.Speed) {
                    Text(
                        text = "Cuts ultra-low frequencies from door woofers to eliminate door panel rattle and prevent voice coil distortion.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HPF_LABELS.forEachIndexed { index, label ->
                            val isSelected = hpfMode == index
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setEqualizerHpfMode(index) },
                                label = { Text(text = label, fontSize = 11.sp) },
                                shape = EqChipShape,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                ),
                                modifier = Modifier.height(32.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // ── Section 4: Vehicle Cabin Soundstage & DSP ──
                DspCard(title = "Virtual Cabin Soundstage & Time Alignment", icon = Icons.Rounded.SpatialAudio) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Virtual Surround
                        Text(
                            text = "Virtual Cabin Soundstage:",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SURROUND_LABELS.forEachIndexed { index, label ->
                                val isSelected = surroundMode == index
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.setVirtualSurroundMode(index) },
                                    label = { Text(text = label, fontSize = 11.sp) },
                                    shape = EqChipShape,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    ),
                                    modifier = Modifier.height(32.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Time Alignment
                        Text(
                            text = "Acoustic Time Alignment (Seat Delay Offset):",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TIME_ALIGNMENT_LABELS.forEachIndexed { index, label ->
                                val isSelected = timeAlignmentMode == index
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.setTimeAlignment(index) },
                                    label = { Text(text = label, fontSize = 11.sp) },
                                    shape = EqChipShape,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    ),
                                    modifier = Modifier.height(32.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Bass Normalizer
                        Text(
                            text = "Dynamic Sub-Bass Normalizer:",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            BASS_BOOST_LABELS.forEachIndexed { index, label ->
                                val isSelected = bassMode == index
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.setBassNormalizer(index) },
                                    label = { Text(text = label, fontSize = 11.sp) },
                                    shape = EqChipShape,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    ),
                                    modifier = Modifier.height(32.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
