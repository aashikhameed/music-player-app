package com.aashik.music.desktop.ui.shell

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Composable
internal fun DeskGlyph(icon: ImageVector, tint: Color, size: Dp, modifier: Modifier = Modifier) {
    Image(
        painter = rememberVectorPainter(icon),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        colorFilter = ColorFilter.tint(tint),
        modifier = modifier.size(size)
    )
}

/** Round control: spring press scale, hover tint, optional flat fill / accent disc. */
@Composable
internal fun DeskIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    glyph: Dp = 24.dp,
    tint: Color = Desk.c.text,
    filled: Boolean = false,
    active: Boolean = false,
    enabled: Boolean = true
) {
    val c = Desk.c
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val hovered by interaction.collectIsHoveredAsState()
    val scale by animateFloatAsState(if (pressed) 0.9f else 1f, spring(dampingRatio = 0.6f, stiffness = 400f))
    val fill by animateColorAsState(
        when {
            active -> c.accent
            filled -> c.idleFill
            hovered || pressed -> c.text.copy(alpha = 0.10f)
            else -> Color.Transparent
        },
        tween(160)
    )
    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer { scaleX = scale; scaleY = scale; alpha = if (enabled) 1f else 0.4f }
            .background(fill, DeskCircle)
            .hoverable(interaction)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) { DeskGlyph(icon, if (active) c.onAccent else tint, glyph) }
}

@Composable
internal fun DeskPillButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, accent: Boolean = true) {
    val c = Desk.c
    Row(
        modifier = modifier
            .height(48.dp)
            .background(if (accent) c.accent else c.idleFill, DeskPill)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) { DeskGlyph(icon, if (accent) c.onAccent else c.text, 20.dp); androidx.compose.foundation.layout.Spacer(Modifier.width(8.dp)) }
        Text(text, color = if (accent) c.onAccent else c.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

/**
 * Kyant liquid glass slider (squash-and-stretch glass thumb with lens), shared with the Android app.
 * [progress] is read from a lambda so only this leaf recomposes on playback ticks.
 */
@Composable
internal fun DeskSeekSlider(progress: () -> Float, onSeek: (Float) -> Unit, modifier: Modifier = Modifier, compact: Boolean = false) {
    val c = Desk.c
    var scrub by remember { mutableStateOf<Float?>(null) }
    val backdrop = LocalDeskBackdrop.current ?: com.kyant.backdrop.backdrops.rememberLayerBackdrop()
    com.aashik.music.desktop.ui.glass.LiquidSlider(
        value = { scrub ?: progress() },
        onValueChange = { scrub = it },
        valueRange = 0f..1f,
        visibilityThreshold = 0.001f,
        backdrop = backdrop,
        modifier = modifier,
        accentColor = c.accent,
        trackColorOverride = c.text.copy(alpha = 0.14f),
        trackHeight = if (compact) 4.dp else 8.dp,
        thumbWidth = if (compact) 28.dp else 40.dp,
        thumbHeight = if (compact) 18.dp else 24.dp,
        onValueChangeFinished = { scrub?.let(onSeek); scrub = null }
    )
}
