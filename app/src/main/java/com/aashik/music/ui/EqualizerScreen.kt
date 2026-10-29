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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.SpatialAudio
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.SurroundSound
import androidx.compose.material.icons.rounded.Tune
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aashik.music.theme.AppGradients
import com.aashik.music.viewmodel.MusicViewModel

// Top-level precomputed shapes for Mali GPU fill rate & zero GC allocation
private val EqContainerShape = RoundedCornerShape(16.dp)
private val EqCardShape = RoundedCornerShape(12.dp)
private val EqChipShape = RoundedCornerShape(8.dp)
private val ActionBtnShape = RoundedCornerShape(10.dp)

private val BAND_INFO = listOf(
    "60 Hz" to "Sub-Bass Punch",
    "250 Hz" to "Door Resonance (Mud Cut)",
    "1.0 kHz" to "Vocal Body & Midrange",
    "3.5 kHz" to "Vocal Presence & Clarity",
    "10 kHz" to "Treble Air & Sparkle"
)

private val PRESET_TITLES = listOf(
    "Flat",
    "Acoustic Clarity",
    "Vocal Separation",
    "Punchy Bass",
    "Rock Dynamic",
    "Custom"
)

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

    val cardBg = AppGradients.card(isActive = false)
    val cardBorder = AppGradients.border(isActive = false)

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 10.dp, vertical = 8.dp)
                .background(brush = cardBg, shape = EqContainerShape)
                .border(width = 1.dp, brush = cardBorder, shape = EqContainerShape)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // ── Top Header Navigation Bar ──
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Icon(
                            imageVector = Icons.Rounded.GraphicEq,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )

                        Column {
                            Text(
                                text = "Acoustic Equalizer & DSP Core",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Vehicle Cabin Acoustic Tuning • OpenSL ES Hardware Engine",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Quick Reset / Flat Button
                        Button(
                            onClick = {
                                viewModel.setEqualizerPreset(0) // Flat
                            },
                            shape = ActionBtnShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.RestartAlt,
                                contentDescription = "Reset Flat",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Flat", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }

                        // Master On/Off Switch
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(ActionBtnShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isEnabled) "DSP ACTIVE" else "BYPASS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(
                                checked = isEnabled,
                                onCheckedChange = { viewModel.setEqualizerEnabled(it) },
                                thumbContent = null,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                                ),
                                modifier = Modifier.size(34.dp)
                            )
                        }

                        Button(
                            onClick = onClose,
                            shape = ActionBtnShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text(text = "Done", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // ── Section 1: Presets ──
                DspCard(title = "Acoustic Tuning Presets", icon = Icons.Rounded.Tune) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PRESET_TITLES.forEachIndexed { index, name ->
                            val isSelected = activePreset == index
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setEqualizerPreset(index) },
                                label = {
                                    Text(
                                        text = name,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                shape = EqChipShape,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    labelColor = MaterialTheme.colorScheme.onSurface
                                ),
                                modifier = Modifier.height(32.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // ── Section 2: 5-Band Graphic Equalizer ──
                DspCard(title = "5-Band Graphic Equalizer (-12 dB to +12 dB)", icon = Icons.Rounded.GraphicEq) {
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

                Spacer(modifier = Modifier.height(10.dp))

                // ── Section 3: High-Pass Filter (Door Protection) ──
                DspCard(title = "Door Speaker High-Pass Filter (HPF)", icon = Icons.Rounded.Speed) {
                    Text(
                        text = "Cuts ultra-low frequencies from door woofers to eliminate door panel rattle and prevent voice coil distortion.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
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
                            modifier = Modifier.fillMaxWidth(),
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
                            modifier = Modifier.fillMaxWidth(),
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
                            modifier = Modifier.fillMaxWidth(),
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
}

@Composable
private fun DspCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    val cardBorder = AppGradients.border(isActive = false)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(EqCardShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .border(width = 0.8.dp, brush = cardBorder, shape = EqCardShape)
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        content()
    }
}
