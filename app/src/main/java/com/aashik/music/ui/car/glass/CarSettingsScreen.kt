package com.aashik.music.ui.car.glass

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.aashik.music.model.DriveSide
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aashik.music.ui.designsystem.Text
import com.aashik.music.ui.mobile.glass.AppThemeMode
import com.aashik.music.ui.mobile.glass.LiquidGlassConfig
import com.aashik.music.ui.mobile.glass.LocalGlassConfig
import com.aashik.music.ui.mobile.glass.MobileTheme
import com.aashik.music.ui.mobile.screens.MobileAboutSettingsCard
import com.aashik.music.ui.mobile.screens.MobileHeadunitSettingsCard
import com.aashik.music.ui.mobile.screens.MobileLibraryStorageSettingsCard
import com.aashik.music.ui.mobile.screens.MobilePlaybackSettingsCard
import com.aashik.music.ui.mobile.screens.MobileSpeedVolumeSettingsCard
import com.aashik.music.viewmodel.MusicViewModel
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

/** Flat cards on the headunit: no blur / lens on the settings surfaces (AGENTS.md §7). */
private val CarFlatCardGlass = LiquidGlassConfig(
    blurRadius = 0.dp,
    refractionHeight = 0.dp,
    refractionAmount = 0.dp,
    chromaticAberration = false,
    surfaceAlpha = 1f
)

private enum class CarSettingsSection(val label: String, val icon: ImageVector) {
    APPEARANCE("Appearance", Icons.Rounded.Palette),
    PLAYBACK("Playback", Icons.Rounded.PlayCircle),
    DRIVING("Driving & Volume", Icons.Rounded.Speed),
    HEADUNIT("Headunit", Icons.Rounded.DirectionsCar),
    LIBRARY("Library & Storage", Icons.Rounded.LibraryMusic),
    AUDIO("Equalizer & Audio", Icons.Rounded.GraphicEq),
    ABOUT("About", Icons.Rounded.Info)
}

/**
 * Master-detail settings: section list on the left, that section's controls on the right.
 * Every legacy headunit setting is here (the cards are the same ones the mobile UI uses).
 */
@Composable
internal fun CarSettingsScreen(viewModel: MusicViewModel, onClose: () -> Unit) {
    var section by rememberSaveable { mutableStateOf(CarSettingsSection.APPEARANCE) }
    val backdrop = rememberLayerBackdrop()

    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().layerBackdrop(backdrop).background(MobileTheme.background))
        val driveSide by viewModel.driveSide.collectAsState()
        CompositionLocalProvider(
            LocalLayoutDirection provides if (driveSide == DriveSide.RIGHT) LayoutDirection.Rtl else LayoutDirection.Ltr
        ) {
        Row(Modifier.fillMaxSize()) {
            val detailSlot = Modifier.weight(1f).fillMaxHeight()
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            // ── Master: section list on the driver's side (solid panel, like the library sidebar) ──
            Column(
                Modifier
                    .width(300.dp)
                    .fillMaxHeight()
                    .background(MobileTheme.panel)
                    .statusBarsPadding()
                    .padding(12.dp)
            ) {
                Row(Modifier.fillMaxWidth().height(72.dp), verticalAlignment = Alignment.CenterVertically) {
                    CarIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back", onClick = onClose, filled = true, glyph = 26.dp)
                    Spacer(Modifier.width(14.dp))
                    Text("Settings", color = MobileTheme.text, fontSize = 28.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp)
                }
                Spacer(Modifier.height(8.dp))
                CarSettingsSection.entries.forEach { s ->
                    val selected = s == section
                    val fill by animateColorAsState(if (selected) carSelectedFill else Color.Transparent, tween(200), label = "setFill")
                    val tint = if (selected) MobileTheme.accent else MobileTheme.text
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(CarDims.SidebarRowHeight)
                            .clip(CarSidebarItemShape)
                            .background(fill)
                            .clickable { section = s }
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CarGlyph(s.icon, null, tint, 24.dp)
                        Spacer(Modifier.width(14.dp))
                        Text(s.label, color = tint, fontSize = 17.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium)
                    }
                }
            }
            }

            // ── Detail ──
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr, LocalGlassConfig provides CarFlatCardGlass) {
                AnimatedContent(
                    targetState = section,
                    transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(120)) },
                    label = "carSettings",
                    modifier = detailSlot
                ) { s ->
                    LazyColumn(
                        Modifier.fillMaxSize().statusBarsPadding(),
                        contentPadding = PaddingValues(start = 32.dp, end = 48.dp, top = 20.dp, bottom = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Text(s.label, color = MobileTheme.text, fontSize = 34.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp,
                                modifier = Modifier.padding(bottom = 4.dp))
                        }
                        when (s) {
                            CarSettingsSection.APPEARANCE -> item { AppearanceCard(viewModel) }
                            CarSettingsSection.PLAYBACK -> item { CarScaledText { MobilePlaybackSettingsCard(viewModel, backdrop) } }
                            CarSettingsSection.DRIVING -> item { CarScaledText { MobileSpeedVolumeSettingsCard(viewModel, backdrop) } }
                            CarSettingsSection.HEADUNIT -> {
                                item { DriveSideCard(viewModel) }
                                item { CarScaledText { MobileHeadunitSettingsCard(viewModel, backdrop, showDriveSide = false) } }
                            }
                            CarSettingsSection.LIBRARY -> {
                                item { LibraryCard(viewModel) }
                                item { CarScaledText { MobileLibraryStorageSettingsCard(viewModel, backdrop) } }
                            }
                            CarSettingsSection.AUDIO -> item { AudioCard(viewModel) }
                            CarSettingsSection.ABOUT -> {
                                item { CarScaledText { MobileAboutSettingsCard(viewModel, backdrop) } }
                                item { SystemCard() }
                            }
                        }
                    }
                }
            }
        }
        }
    }
}

/** The reused mobile cards use 11–14 sp text; enlarge it 1.2× for the headunit's viewing distance. */
@Composable
private fun CarScaledText(content: @Composable () -> Unit) {
    val density = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(density.density, density.fontScale * 1.2f), content = content)
}

// ─── Car-specific cards (flat panel, same look as the reused mobile cards) ────

@Composable
private fun CarCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(MobileTheme.panel, CarCardShape)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(title, color = MobileTheme.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
        content()
    }
}

@Composable
private fun Label(title: String, subtitle: String?, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(title, color = MobileTheme.text, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        if (subtitle != null) Text(subtitle, color = MobileTheme.textSecondary, fontSize = 13.sp, lineHeight = 17.sp)
    }
}

@Composable
private fun ChoiceChip(label: String, selected: Boolean, modifier: Modifier = Modifier, icon: ImageVector? = null, onClick: () -> Unit) {
    val fill by animateColorAsState(if (selected) carSelectedFill else carIdleFill, tween(200), label = "chipFill")
    val tint = if (selected) MobileTheme.accent else MobileTheme.text
    Row(
        modifier
            .height(48.dp)
            .clip(CarPillShape)
            .background(fill)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            CarGlyph(icon, null, tint, 20.dp)
            Spacer(Modifier.width(8.dp))
        }
        Text(label, color = tint, fontSize = 15.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium, maxLines = 1)
    }
}

/** Driving side picker: each option shows a miniature of the Now Playing layout it produces. */
@Composable
private fun DriveSideCard(viewModel: MusicViewModel) {
    val side by viewModel.driveSide.collectAsState()
    CarCard("DRIVING SIDE") {
        Label(
            "Where does the driver sit?",
            "Navigation and controls move to the driver's side so they stay in easy reach. Artwork and lists go to the passenger side."
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            DriveSide.entries.forEach { option ->
                DriveSideOption(option, selected = side == option, modifier = Modifier.weight(1f)) { viewModel.setDriveSide(option) }
            }
        }
    }
}

@Composable
private fun DriveSideOption(option: DriveSide, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val fill by animateColorAsState(if (selected) carSelectedFill else carIdleFill, tween(200), label = "sideFill")
    val accent = MobileTheme.accent
    val screen = MobileTheme.background
    val ink = MobileTheme.text
    Column(
        modifier
            .clip(CarCardShape)
            .background(fill)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Miniature of the Now Playing stage for this driving side
        Canvas(Modifier.fillMaxWidth().height(116.dp)) {
            val pad = 10.dp.toPx()
            val corner = CornerRadius(12.dp.toPx())
            drawRoundRect(screen, size = size, cornerRadius = corner)
            val driverLeft = option == DriveSide.LEFT
            val controlsW = size.width * 0.40f
            val artSide = minOf(size.height - 2 * pad, size.width - controlsW - 3 * pad)
            val controlsX = if (driverLeft) pad else size.width - pad - controlsW
            val artX = if (driverLeft) size.width - pad - artSide else pad
            // artwork (passenger side)
            drawRoundRect(ink.copy(alpha = 0.16f), Offset(artX, pad), Size(artSide, artSide), CornerRadius(6.dp.toPx()))
            // controls (driver side): title, artist, seek track + progress, transport
            val bar = CornerRadius(3.dp.toPx())
            drawRoundRect(ink.copy(alpha = 0.60f), Offset(controlsX, pad + 2.dp.toPx()), Size(controlsW * 0.85f, 7.dp.toPx()), bar)
            drawRoundRect(ink.copy(alpha = 0.28f), Offset(controlsX, pad + 14.dp.toPx()), Size(controlsW * 0.55f, 5.dp.toPx()), bar)
            val seekY = size.height - pad - 44.dp.toPx()
            drawRoundRect(ink.copy(alpha = 0.18f), Offset(controlsX, seekY), Size(controlsW, 5.dp.toPx()), bar)
            drawRoundRect(accent, Offset(controlsX, seekY), Size(controlsW * 0.4f, 5.dp.toPx()), bar)
            val cy = size.height - pad - 15.dp.toPx()
            drawCircle(ink.copy(alpha = 0.35f), 6.dp.toPx(), Offset(controlsX + controlsW * 0.12f, cy))
            drawCircle(accent, 13.dp.toPx(), Offset(controlsX + controlsW * 0.5f, cy))
            drawCircle(ink.copy(alpha = 0.35f), 6.dp.toPx(), Offset(controlsX + controlsW * 0.88f, cy))
        }
        Column {
            Text(
                option.label,
                color = if (selected) MobileTheme.accent else MobileTheme.text,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(option.summary, color = MobileTheme.textSecondary, fontSize = 13.sp, lineHeight = 17.sp)
        }
    }
}

@Composable
private fun AppearanceCard(viewModel: MusicViewModel) {
    val themeMode by viewModel.themeMode.collectAsState()
    val glassBlur by viewModel.glassBlurRadius.collectAsState()
    val glassHeight by viewModel.glassRefractionHeight.collectAsState()
    val glassAmount by viewModel.glassRefractionAmount.collectAsState()
    val glassChromatic by viewModel.glassChromaticAberration.collectAsState()
    val glassAlpha by viewModel.glassSurfaceAlpha.collectAsState()

    CarCard("THEME") {
        Label(
            "Theme",
            when (themeMode) {
                AppThemeMode.LIGHT -> "Light contrast"
                AppThemeMode.DARK -> "Dark"
                AppThemeMode.ALBUM_ART -> "Tinted from the current song's artwork"
            }
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(
                Triple(AppThemeMode.LIGHT, Icons.Rounded.LightMode, "Light"),
                Triple(AppThemeMode.DARK, Icons.Rounded.DarkMode, "Dark"),
                Triple(AppThemeMode.ALBUM_ART, Icons.Rounded.Palette, "Artwork")
            ).forEach { (mode, icon, label) ->
                ChoiceChip(label, themeMode == mode, Modifier.weight(1f), icon) { viewModel.setAppThemeMode(mode) }
            }
        }
    }
    Spacer(Modifier.height(16.dp))
    // Glass Style presets: on the headunit they style the Now Playing seek slider (the only lens element).
    data class GlassPreset(val name: String, val blur: Float, val height: Float, val amount: Float, val chromatic: Boolean, val alpha: Float)
    val presets = remember {
        listOf(
            GlassPreset("Default", 8f, 24f, 24f, true, 0.4f),
            GlassPreset("Frosted", 24f, 6f, 10f, false, 0.75f),
            GlassPreset("Crystal", 4f, 28f, 40f, true, 0.35f),
            GlassPreset("Minimal", 0f, 0f, 0f, false, 0.85f)
        )
    }
    val active = presets.firstOrNull {
        it.blur == glassBlur && it.height == glassHeight && it.amount == glassAmount &&
            it.chromatic == glassChromatic && kotlin.math.abs(it.alpha - glassAlpha) < 0.01f
    }
    CarCard("LIQUID GLASS") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Label("Glass style", (active?.let { "${it.name} glass" } ?: "Custom glass") + " • used by the seek slider", Modifier.weight(1f))
            CarTextPillButton("Reset", accent = false) { viewModel.resetGlassSettings() }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            presets.forEach { p ->
                ChoiceChip(p.name, p == active, Modifier.weight(1f)) {
                    viewModel.setGlassPreset(p.blur, p.height, p.amount, p.chromatic, p.alpha)
                }
            }
        }
    }
}

@Composable
private fun LibraryCard(viewModel: MusicViewModel) {
    val songs by viewModel.songs.collectAsState()
    CarCard("MUSIC LIBRARY") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Label("Rescan device", "${songs.size} tracks in the current view", Modifier.weight(1f))
            CarTextPillButton("Rescan") { viewModel.loadSongs() }
        }
    }
}

@Composable
private fun AudioCard(viewModel: MusicViewModel) {
    val eqEnabled by viewModel.isEqualizerEnabled.collectAsState()
    CarCard("SOUND") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Label(
                "Equalizer",
                (if (eqEnabled) "On" else "Off") + " • 10-band EQ, door-speaker HPF, cabin soundstage, bass normaliser",
                Modifier.weight(1f)
            )
            CarTextPillButton("Open") { viewModel.openEqualizer() }
        }
    }
}

/** Hardware / engine diagnostics (legacy SystemInfoSection + the HUD latency readout, decision D5). */
@Composable
private fun SystemCard() {
    CarCard("SYSTEM") {
        Label("Platform", "Automotive headunit • ARM64 • Mali GPU")
        Label("Audio core", "C++ OpenSL ES 1.1 + Direct Form II transposed biquad DSP")
        Label("Audio pipeline", "48 kHz • 96 KB lock-free SPSC ring buffer • 5.3 ms output latency")
        Label("Storage & cache", "SQLite WAL • NEON JPEG artwork cache • RGB_565 thumbnails")
    }
}
