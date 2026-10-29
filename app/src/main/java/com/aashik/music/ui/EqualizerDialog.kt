package com.aashik.music.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Star
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.aashik.music.theme.AppGradients
import com.aashik.music.viewmodel.MusicViewModel

// Precomputed shapes hoisted to top-level constants for Mali GPU fill rate & zero GC allocation
private val DialogContainerShape = RoundedCornerShape(18.dp)
private val ChipPillShape = RoundedCornerShape(10.dp)
private val BandRowShape = RoundedCornerShape(12.dp)
private val InfoBadgeShape = RoundedCornerShape(8.dp)

private val AccentGoldColor = Color(0xFFFFD54F)
private val MudCutWarningColor = Color(0xFFFF8A65)
private val VocalCyanColor = Color(0xFF4DD0E1)

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

/**
 * Automotive Equalizer & Acoustic Separation DSP Dialog.
 * Specifically tuned for automotive headunit & door speakers.
 */
@Composable
fun EqualizerDialog(
    viewModel: MusicViewModel,
    onDismiss: () -> Unit
) {
    val isEnabled by viewModel.isEqualizerEnabled.collectAsState()
    val activePreset by viewModel.equalizerPreset.collectAsState()
    val bandGains by viewModel.equalizerBands.collectAsState()
    val hpfMode by viewModel.equalizerHpfMode.collectAsState()

    val cardBg = AppGradients.card(isActive = false)
    val cardBorder = AppGradients.border(isActive = false)
    val capsuleBg = AppGradients.capsule(isActive = false)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .widthIn(min = 600.dp, max = 920.dp)
                .fillMaxWidth(0.92f)
                .background(brush = cardBg, shape = DialogContainerShape)
                .border(width = 1.dp, brush = cardBorder, shape = DialogContainerShape)
                .padding(16.dp),
            shape = DialogContainerShape,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // ── Header Row: Title, Master Switch & Close Button ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.GraphicEq,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Automotive Acoustic Equalizer",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .background(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                            shape = InfoBadgeShape
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Automotive DSP",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Text(
                                text = "Acoustic separation for vehicle cabin & door speakers",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (isEnabled) "DSP ACTIVE" else "BYPASS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Switch(
                            checked = isEnabled,
                            onCheckedChange = { viewModel.setEqualizerEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                // ── Presets Selection Chips ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Profiles:",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 4.dp)
                    )

                    PRESET_NAMES.forEachIndexed { index, name ->
                        val isSelected = (activePreset == index)
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setEqualizerPreset(index) },
                            enabled = isEnabled,
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (index == 1) { // Recommended profile
                                        Icon(
                                            imageVector = Icons.Rounded.Star,
                                            contentDescription = null,
                                            tint = if (isSelected) AccentGoldColor else MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                    }
                                    Text(
                                        text = name,
                                        fontSize = 11.sp,
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

                // ── Subsonic High-Pass Filter (HPF) Protection Row ──
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(brush = capsuleBg, shape = BandRowShape)
                        .border(width = 0.8.dp, brush = cardBorder, shape = BandRowShape)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "High-Pass Filter (HPF):",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Prevents woofer excursion distortion on door speakers",
                            fontSize = 10.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected && isEnabled) MaterialTheme.colorScheme.onSecondary
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // ── 5 Equalizer Sliders ──
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
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
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                                    shape = BandRowShape
                                )
                                .padding(horizontal = 10.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Band Frequency and Role Label
                            Column(modifier = Modifier.width(160.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = freqLabel,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.5.sp,
                                        color = if (isMudZone) MudCutWarningColor
                                                else if (isVocalZone) VocalCyanColor
                                                else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (isMudZone) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .background(MudCutWarningColor.copy(alpha = 0.2f), shape = InfoBadgeShape)
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        ) {
                                            Text(
                                                text = "Car Resonance",
                                                fontSize = 8.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MudCutWarningColor
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = roleLabel,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }

                            // Horizontal Slider
                            Slider(
                                value = gain,
                                onValueChange = { newGain ->
                                    viewModel.setEqualizerBandGain(bandIndex, (newGain * 2).toInt() / 2f)
                                },
                                valueRange = -10f..10f,
                                steps = 39, // 0.5 dB step intervals
                                enabled = isEnabled,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp),
                                colors = SliderDefaults.colors(
                                    thumbColor = if (isMudZone) MudCutWarningColor else MaterialTheme.colorScheme.primary,
                                    activeTrackColor = if (isMudZone) MudCutWarningColor else MaterialTheme.colorScheme.primary,
                                    inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                )
                            )

                            // Formatted Gain Text: e.g. "-4.0 dB", "+2.5 dB"
                            Box(
                                modifier = Modifier.width(55.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                val gainText = if (gain > 0f) "+${String.format("%.1f", gain)}" else "${String.format("%.1f", gain)}"
                                Text(
                                    text = "$gainText dB",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (gain < 0f && isMudZone) MudCutWarningColor
                                            else if (gain > 0f) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // ── Footer: DAC Headroom & Tuning Guide Info ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "• -2.5 dB digital headroom active (eliminates DAC clipping into external processor)",
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                    Text(
                        text = "Keep headunit volume at ~75-80%",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
