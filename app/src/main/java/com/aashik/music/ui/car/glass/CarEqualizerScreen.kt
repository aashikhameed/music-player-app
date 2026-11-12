package com.aashik.music.ui.car.glass

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.aashik.music.model.DriveSide
import com.aashik.music.model.EqualizerPresetRegistry
import com.aashik.music.ui.designsystem.Text
import com.aashik.music.ui.mobile.glass.MobileTheme
import com.aashik.music.viewmodel.MusicViewModel
import kotlin.math.roundToInt

private val BandFrequencies = listOf("31", "62", "125", "250", "500", "1k", "2k", "4k", "8k", "16k")
private val HpfLabels = listOf("Full range", "60 Hz cut", "80 Hz cut")
private val SurroundLabels = listOf("Stereo", "Studio cabin", "Wide soundstage")
private val TimeAlignmentLabels = listOf("Centre", "Driver (RHD)", "Driver (LHD)")
private val BassLabels = listOf("Off", "Mild punch", "Deep sub-bass")
private const val GainMin = -12f
private const val GainMax = 12f

/**
 * Car equalizer (same controls as the legacy EqualizerScreen): enable, 24 presets,
 * 10-band editor (−12…+12 dB, drag any band; switches to Custom), door-speaker HPF,
 * virtual cabin soundstage, time alignment and bass normaliser. Flat themed surfaces.
 */
@Composable
internal fun CarEqualizerScreen(viewModel: MusicViewModel, onClose: () -> Unit) {
    val isEnabled by viewModel.isEqualizerEnabled.collectAsState()
    val activePreset by viewModel.equalizerPreset.collectAsState()
    val bandGains by viewModel.equalizerBands.collectAsState()
    val hpfMode by viewModel.equalizerHpfMode.collectAsState()
    val surroundMode by viewModel.virtualSurroundMode.collectAsState()
    val timeAlignmentMode by viewModel.timeAlignmentMode.collectAsState()
    val bassMode by viewModel.bassNormalizerMode.collectAsState()

    Column(Modifier.fillMaxSize().background(MobileTheme.background).statusBarsPadding().padding(horizontal = 24.dp)) {
        val driveSide by viewModel.driveSide.collectAsState()
        // Back + title on the driver's side: the header row is laid out RTL for a right-hand drive
        // (children reversed), with the arrow kept pointing left.
        CompositionLocalProvider(
            LocalLayoutDirection provides if (driveSide == DriveSide.RIGHT) LayoutDirection.Rtl else LayoutDirection.Ltr
        ) {
        Row(Modifier.fillMaxWidth().height(88.dp), verticalAlignment = Alignment.CenterVertically) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                CarIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back", onClick = onClose, filled = true, glyph = 26.dp)
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text("Equalizer", color = MobileTheme.text, fontSize = 34.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp)
                Text("Acoustic tuning • OpenSL ES native engine", color = MobileTheme.textSecondary, fontSize = 14.sp)
            }
            Text(if (isEnabled) "Active" else "Bypassed", color = if (isEnabled) MobileTheme.accent else MobileTheme.textSecondary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(14.dp))
            CarSwitch(checked = isEnabled, onCheckedChange = { viewModel.setEqualizerEnabled(it) })
        }
        }

        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                EqCard("PRESET", trailing = EqualizerPresetRegistry.PRESET_NAMES.getOrNull(activePreset)) {
                    val listState = rememberLazyListState()
                    LaunchedEffect(Unit) { listState.scrollToItem((activePreset - 1).coerceAtLeast(0)) }
                    LazyRow(state = listState, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        itemsIndexed(EqualizerPresetRegistry.PRESET_NAMES) { index, name ->
                            EqChip(name, index == activePreset) { viewModel.setEqualizerPreset(index) }
                        }
                    }
                }
            }
            item {
                EqCard("10-BAND EQUALIZER  •  −12 dB TO +12 DB", trailing = if (activePreset == EqualizerPresetRegistry.PRESET_CUSTOM) "Custom" else "Drag a band to customise") {
                    BandEditor(
                        gains = bandGains,
                        enabled = isEnabled,
                        onGainChange = { index, gain -> viewModel.setEqualizerBandGain(index, gain) }
                    )
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    EqCard("DOOR SPEAKER HIGH-PASS FILTER", Modifier.weight(1f)) {
                        ChipRow(HpfLabels, hpfMode) { viewModel.setEqualizerHpfMode(it) }
                    }
                    EqCard("BASS NORMALISER", Modifier.weight(1f)) {
                        ChipRow(BassLabels, bassMode) { viewModel.setBassNormalizer(it) }
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    EqCard("VIRTUAL CABIN SOUNDSTAGE", Modifier.weight(1f)) {
                        ChipRow(SurroundLabels, surroundMode) { viewModel.setVirtualSurroundMode(it) }
                    }
                    EqCard("TIME ALIGNMENT", Modifier.weight(1f)) {
                        ChipRow(TimeAlignmentLabels, timeAlignmentMode) { viewModel.setTimeAlignment(it) }
                    }
                }
            }
        }
    }
}

@Composable
private fun EqCard(title: String, modifier: Modifier = Modifier, trailing: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxWidth().background(MobileTheme.panel, CarCardShape).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = MobileTheme.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp, modifier = Modifier.weight(1f))
            if (trailing != null) Text(trailing, color = MobileTheme.accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
        content()
    }
}

@Composable
private fun EqChip(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val fill by animateColorAsState(if (selected) MobileTheme.accent else carIdleFill, tween(200), label = "eqChip")
    Box(
        modifier
            .height(48.dp)
            .clip(CarPillShape)
            .background(fill)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (selected) MobileTheme.onText else MobileTheme.text, fontSize = 15.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium, maxLines = 1)
    }
}

@Composable
private fun ChipRow(labels: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        labels.forEachIndexed { index, label -> EqChip(label, index == selected, Modifier.weight(1f)) { onSelect(index) } }
    }
}

/** Flat on/off switch (no backdrop sampling). */
@Composable
internal fun CarSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val track by animateColorAsState(if (checked) MobileTheme.accent else MobileTheme.text.copy(alpha = 0.18f), tween(200), label = "swTrack")
    val knobX by animateDpAsState(if (checked) 28.dp else 4.dp, spring(dampingRatio = 0.7f, stiffness = 500f), label = "swKnob")
    Box(
        Modifier
            .size(width = 64.dp, height = 40.dp)
            .background(track, CarPillShape)
            .clickable { onCheckedChange(!checked) }
    ) {
        Box(Modifier.offset(x = knobX, y = 4.dp).size(32.dp).background(Color.White, CarCircle))
    }
}

/**
 * Ten draggable bands drawn in one Canvas (no per-band layout nodes) with the response curve
 * through the band values. Geometry is measured in onSizeChanged, not during draw.
 */
@Composable
private fun BandEditor(gains: List<Float>, enabled: Boolean, onGainChange: (Int, Float) -> Unit) {
    var widthPx by remember { mutableFloatStateOf(1f) }
    var heightPx by remember { mutableFloatStateOf(1f) }
    var activeBand by remember { mutableIntStateOf(-1) }
    val currentGains by rememberUpdatedState(gains)
    val currentOnChange by rememberUpdatedState(onGainChange)
    val bands = BandFrequencies.size
    val accent = if (enabled) MobileTheme.accent else MobileTheme.textTertiary
    val track = MobileTheme.text.copy(alpha = 0.10f)
    val zeroLine = MobileTheme.text.copy(alpha = 0.18f)

    fun bandAt(x: Float) = ((x / widthPx) * bands).toInt().coerceIn(0, bands - 1)
    fun gainAt(y: Float): Float {
        val fraction = 1f - (y / heightPx).coerceIn(0f, 1f)
        val raw = GainMin + fraction * (GainMax - GainMin)
        return ((raw * 2f).roundToInt() / 2f).coerceIn(GainMin, GainMax) // 0.5 dB steps
    }

    Column {
        // Gain readouts above each band
        Row(Modifier.fillMaxWidth()) {
            for (i in 0 until bands) {
                val g = gains.getOrElse(i) { 0f }
                Text(
                    (if (g > 0) "+" else "") + (if (g % 1f == 0f) g.toInt().toString() else "%.1f".format(g)),
                    color = if (i == activeBand) MobileTheme.accent else MobileTheme.textSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(220.dp)
                .onSizeChanged { widthPx = it.width.toFloat(); heightPx = it.height.toFloat() }
                .pointerInput(Unit) {
                    detectTapGestures { o -> currentOnChange(bandAt(o.x), gainAt(o.y)) }
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { o -> activeBand = bandAt(o.x); currentOnChange(activeBand, gainAt(o.y)) },
                        onDragEnd = { activeBand = -1 },
                        onDragCancel = { activeBand = -1 }
                    ) { change, _ ->
                        change.consume()
                        if (activeBand >= 0) {
                            val g = gainAt(change.position.y)
                            if (g != currentGains.getOrNull(activeBand)) currentOnChange(activeBand, g)
                        }
                    }
                }
        ) {
            val colW = size.width / bands
            val trackW = 8.dp.toPx()
            val knobR = 13.dp.toPx()
            fun yFor(g: Float) = size.height * (1f - (g - GainMin) / (GainMax - GainMin))
            val zeroY = yFor(0f)
            drawLine(zeroLine, Offset(0f, zeroY), Offset(size.width, zeroY), strokeWidth = 1.dp.toPx())
            val curve = Path()
            for (i in 0 until bands) {
                val cx = colW * i + colW / 2
                val g = currentGains.getOrElse(i) { 0f }
                val y = yFor(g)
                drawRoundRect(track, Offset(cx - trackW / 2, 0f), Size(trackW, size.height), CornerRadius(trackW / 2))
                val top = minOf(y, zeroY)
                drawRoundRect(accent.copy(alpha = 0.55f), Offset(cx - trackW / 2, top), Size(trackW, kotlin.math.abs(zeroY - y)), CornerRadius(trackW / 2))
                if (i == 0) curve.moveTo(cx, y) else {
                    val px = colW * (i - 1) + colW / 2
                    val py = yFor(currentGains.getOrElse(i - 1) { 0f })
                    curve.cubicTo((px + cx) / 2, py, (px + cx) / 2, y, cx, y)
                }
            }
            drawPath(curve, accent, style = Stroke(width = 2.5.dp.toPx()))
            for (i in 0 until bands) {
                val cx = colW * i + colW / 2
                val y = yFor(currentGains.getOrElse(i) { 0f })
                drawCircle(accent, radius = if (i == activeBand) knobR * 1.25f else knobR, center = Offset(cx, y))
                drawCircle(Color.White, radius = knobR * 0.45f, center = Offset(cx, y))
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth()) {
            BandFrequencies.forEach { f ->
                Text(f, color = MobileTheme.textTertiary, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            }
        }
    }
}
