package com.aashik.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aashik.music.theme.AppGradients
import com.aashik.music.ui.designsystem.*
import com.aashik.music.viewmodel.MusicViewModel
import kotlin.math.roundToInt

// Precomputed static shapes hoisted to top-level constants for zero GC churn on automotive hardware
private val CardSectionShape = RoundedCornerShape(12.dp)
private val ChipPillShape = RoundedCornerShape(8.dp)
private val PresetBadgeShape = RoundedCornerShape(6.dp)

private val CyanGlassAccent = Color(0xFF00E5FF)

/**
 * Liquid Glass Optics & Shader Preferences Section for Settings.
 * Allows interactive tuning of backdrop blur, optical lens refraction,
 * chromatic aberration, and obsidian surface opacity.
 * Based on Kyant0/AndroidLiquidGlass GlassPlaygroundContent reference.
 */
@Composable
fun LiquidGlassSettingsSection(
    viewModel: MusicViewModel,
    modifier: Modifier = Modifier
) {
    val blurRadius by viewModel.glassBlurRadius.collectAsState()
    val refractionHeight by viewModel.glassRefractionHeight.collectAsState()
    val refractionAmount by viewModel.glassRefractionAmount.collectAsState()
    val chromaticAberration by viewModel.glassChromaticAberration.collectAsState()
    val surfaceAlpha by viewModel.glassSurfaceAlpha.collectAsState()

    val cardBorder = AppGradients.border(isActive = false)

    Surface(
        modifier = modifier
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
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Tune,
                        contentDescription = null,
                        tint = CyanGlassAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Liquid Glass Effect Optics",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Reset Button
                Box(
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = PresetBadgeShape
                        )
                        .clickable { viewModel.resetGlassSettings() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Reset Glass Defaults",
                            tint = CyanGlassAccent,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "Reset",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CyanGlassAccent
                        )
                    }
                }
            }

            Text(
                text = "Shader blur, optical lens refraction, and prism dispersion",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Preset Pills Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val presets = listOf(
                    "Default" to { viewModel.setGlassPreset(12f, 16f, 24f, true, 0.65f) },
                    "Frosted" to { viewModel.setGlassPreset(24f, 6f, 10f, false, 0.75f) },
                    "Crystal" to { viewModel.setGlassPreset(4f, 28f, 40f, true, 0.35f) },
                    "Minimal" to { viewModel.setGlassPreset(0f, 0f, 0f, false, 0.85f) }
                )

                presets.forEach { (name, action) ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                shape = ChipPillShape
                            )
                            .clickable { action() }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = name,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                thickness = 0.5.dp
            )

            // 1. Blur Radius Slider
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Blur Radius",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${blurRadius.roundToInt()} dp",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanGlassAccent
                    )
                }
                Slider(
                    value = blurRadius,
                    onValueChange = { viewModel.updateGlassBlur(it) },
                    valueRange = 0f..32f,
                    modifier = Modifier.fillMaxWidth().height(28.dp)
                )
            }

            // 2. Refraction Height Slider
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Refraction Lens Height",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${refractionHeight.roundToInt()} dp",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanGlassAccent
                    )
                }
                Slider(
                    value = refractionHeight,
                    onValueChange = { viewModel.updateGlassRefractionHeight(it) },
                    valueRange = 0f..32f,
                    modifier = Modifier.fillMaxWidth().height(28.dp)
                )
            }

            // 3. Refraction Amount Slider
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Refraction Lens Amount",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${refractionAmount.roundToInt()} dp",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanGlassAccent
                    )
                }
                Slider(
                    value = refractionAmount,
                    onValueChange = { viewModel.updateGlassRefractionAmount(it) },
                    valueRange = 0f..48f,
                    modifier = Modifier.fillMaxWidth().height(28.dp)
                )
            }

            // 4. Surface Alpha Slider
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Surface Glass Opacity",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${(surfaceAlpha * 100).roundToInt()}%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanGlassAccent
                    )
                }
                Slider(
                    value = surfaceAlpha,
                    onValueChange = { viewModel.updateGlassSurfaceAlpha(it) },
                    valueRange = 0.1f..0.95f,
                    modifier = Modifier.fillMaxWidth().height(28.dp)
                )
            }

            // 5. Chromatic Aberration Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = "Chromatic Aberration",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Prism color fringe along curved optical lens edges",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = chromaticAberration,
                    onCheckedChange = { viewModel.updateGlassChromaticAberration(it) }
                )
            }
        }
    }
}

