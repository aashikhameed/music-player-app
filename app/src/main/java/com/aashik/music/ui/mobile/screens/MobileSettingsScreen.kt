package com.aashik.music.ui.mobile.screens

import com.aashik.music.ui.mobile.glass.MobileTheme
import com.aashik.music.ui.mobile.glass.AppThemeMode

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aashik.music.ui.designsystem.Icon
import com.aashik.music.ui.designsystem.Switch
import com.aashik.music.ui.designsystem.Text
import com.aashik.music.ui.mobile.glass.LiquidButton
import com.aashik.music.ui.mobile.glass.LiquidGlassCard
import com.aashik.music.ui.mobile.glass.LiquidIconButton
import com.aashik.music.ui.mobile.glass.LiquidSlider
import com.aashik.music.viewmodel.MusicViewModel
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.Capsule
import com.kyant.shapes.RoundedRectangle
import kotlin.math.roundToInt

// Pre-allocated static shapes hoisted to top-level constants for zero GC churn
private val PreviewCardShape = RoundedRectangle(20f.dp)
private val PresetButtonShape = Capsule()
private val ChipPillShape = RoundedRectangle(12f.dp)

private val CyanGlassAccent: androidx.compose.ui.graphics.Color get() = MobileTheme.accent

/**
 * Mobile Settings Screen with full Kyant0/AndroidLiquidGlass aesthetic.
 * Allows interactive real-time tuning of optical lens refraction, shader blur,
 * chromatic aberration, and obsidian surface opacity with live preview.
 */
@Composable
fun MobileSettingsScreen(
    viewModel: MusicViewModel,
    backdrop: Backdrop,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val songs by viewModel.songs.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()

    val glassBlur by viewModel.glassBlurRadius.collectAsState()
    val glassRefractionHeight by viewModel.glassRefractionHeight.collectAsState()
    val glassRefractionAmount by viewModel.glassRefractionAmount.collectAsState()
    val glassChromatic by viewModel.glassChromaticAberration.collectAsState()
    val glassSurfaceAlpha by viewModel.glassSurfaceAlpha.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 175.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ─── TOP BAR WITH BACK BUTTON & TITLE ─────────────────────────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    LiquidIconButton(
                        onClick = onBack,
                        backdrop = backdrop,
                        size = 38.dp,
                        surfaceColor = MobileTheme.text.copy(alpha = 0.12f)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = MobileTheme.text,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = "Settings",
                        color = MobileTheme.text,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    )
                }

                // Reset Defaults Button
                LiquidButton(
                    onClick = { viewModel.resetGlassSettings() },
                    backdrop = backdrop,
                    surfaceColor = MobileTheme.text.copy(alpha = 0.10f),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text(
                        text = "Reset",
                        color = CyanGlassAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // ─── AUDIO & THEME CARD ───────────────────────────────────────────────
        item {
            LiquidGlassCard(
                backdrop = backdrop,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "APP PREFERENCES",
                        color = MobileTheme.textSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    )

                    // Theme Row (Light / Dark / Album Art)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Theme",
                            color = MobileTheme.text,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = when (themeMode) {
                                AppThemeMode.LIGHT -> "Light contrast"
                                AppThemeMode.DARK -> "Dark AMOLED"
                                AppThemeMode.ALBUM_ART -> "Follows album art colour"
                            },
                            color = MobileTheme.textSecondary,
                            fontSize = 11.sp
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                Triple(AppThemeMode.LIGHT, Icons.Rounded.LightMode, Color(0xFFFFD54F)),
                                Triple(AppThemeMode.DARK, Icons.Rounded.DarkMode, Color(0xFF7986CB)),
                                Triple(AppThemeMode.ALBUM_ART, Icons.Rounded.Palette, Color(0xFFEC4899))
                            ).forEach { (mode, icon, tone) ->
                                val selected = themeMode == mode
                                LiquidButton(
                                    onClick = { viewModel.setAppThemeMode(mode) },
                                    backdrop = backdrop,
                                    surfaceColor = if (selected) tone.copy(alpha = 0.25f) else MobileTheme.text.copy(alpha = 0.08f),
                                    modifier = Modifier.weight(1f).height(34.dp)
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = if (selected) tone else MobileTheme.text,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(mode.label, fontSize = 11.sp, color = MobileTheme.text)
                                }
                            }
                        }
                    }

                    // Glass Style: shader presets (replace the old per-value sliders)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Glass Style",
                            color = MobileTheme.text,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        data class GlassPreset(
                            val name: String, val blur: Float, val height: Float,
                            val amount: Float, val chromatic: Boolean, val alpha: Float
                        )
                        val glassPresets = remember {
                            listOf(
                                GlassPreset("Default", 8f, 24f, 24f, true, 0.4f),
                                GlassPreset("Frosted", 24f, 6f, 10f, false, 0.75f),
                                GlassPreset("Crystal", 4f, 28f, 40f, true, 0.35f),
                                GlassPreset("Minimal", 0f, 0f, 0f, false, 0.85f)
                            )
                        }
                        val activePreset = glassPresets.firstOrNull {
                            it.blur == glassBlur && it.height == glassRefractionHeight &&
                                it.amount == glassRefractionAmount && it.chromatic == glassChromatic &&
                                kotlin.math.abs(it.alpha - glassSurfaceAlpha) < 0.01f
                        }
                        Text(
                            text = activePreset?.let { "${it.name} glass" } ?: "Custom glass",
                            color = MobileTheme.textSecondary,
                            fontSize = 11.sp
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            glassPresets.forEach { preset ->
                                val selected = preset == activePreset
                                LiquidButton(
                                    onClick = {
                                        viewModel.setGlassPreset(
                                            preset.blur, preset.height, preset.amount, preset.chromatic, preset.alpha
                                        )
                                    },
                                    backdrop = backdrop,
                                    surfaceColor = if (selected) MobileTheme.accent.copy(alpha = 0.22f)
                                        else MobileTheme.text.copy(alpha = 0.08f),
                                    shape = PresetButtonShape,
                                    modifier = Modifier.weight(1f).height(34.dp)
                                ) {
                                    Text(
                                        text = preset.name,
                                        color = if (selected) MobileTheme.accent else MobileTheme.text,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    // Rescan Library Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Music Library",
                                color = MobileTheme.text,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${songs.size} local tracks indexed",
                                color = MobileTheme.textSecondary,
                                fontSize = 11.sp
                            )
                        }

                        LiquidButton(
                            onClick = { viewModel.loadSongs() },
                            backdrop = backdrop,
                            surfaceColor = MobileTheme.text.copy(alpha = 0.10f),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = null,
                                tint = CyanGlassAccent,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Rescan", fontSize = 11.sp, color = CyanGlassAccent)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "Equalizer", color = MobileTheme.text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text(text = "10-band audio DSP & sound enhancement", color = MobileTheme.textSecondary, fontSize = 11.sp)
                        }
                        LiquidButton(
                            onClick = { viewModel.openEqualizer() },
                            backdrop = backdrop,
                            surfaceColor = MobileTheme.text.copy(alpha = 0.10f),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text("Open", fontSize = 11.sp, color = CyanGlassAccent)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "Drive Mode", color = MobileTheme.text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text(text = "High-contrast automotive HUD controls", color = MobileTheme.textSecondary, fontSize = 11.sp)
                        }
                        LiquidButton(
                            onClick = { viewModel.openDriveMode() },
                            backdrop = backdrop,
                            surfaceColor = MobileTheme.text.copy(alpha = 0.10f),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text("Open", fontSize = 11.sp, color = CyanGlassAccent)
                        }
                    }
                }
            }
        }

        // ─── SECTIONS CARRIED OVER FROM THE HEADUNIT SETTINGS ─────────────────
        item { MobilePlaybackSettingsCard(viewModel, backdrop) }
        item { MobileSpeedVolumeSettingsCard(viewModel, backdrop) }
        item { MobileLibraryStorageSettingsCard(viewModel, backdrop) }
        item { MobileHeadunitSettingsCard(viewModel, backdrop) }
        item { MobileAboutSettingsCard(viewModel, backdrop) }
    }
}
