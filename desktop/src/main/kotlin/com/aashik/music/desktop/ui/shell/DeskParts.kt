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
 * Liquid-glass style seek slider: pill thumb that swells under the pointer (spring), accent fill.
 * [progress] is read from a lambda so only this leaf recomposes on playback ticks.
 */
@Composable
internal fun DeskSeekSlider(progress: () -> Float, onSeek: (Float) -> Unit, modifier: Modifier = Modifier, compact: Boolean = false) {
    val c = Desk.c
    var dragging by remember { mutableStateOf<Float?>(null) }
    var widthPx by remember { mutableStateOf(1f) }
    val pressScale by animateFloatAsState(if (dragging != null) 1.35f else 1f, spring(dampingRatio = 0.55f, stiffness = 420f))
    val thumbW = if (compact) 14.dp else 40.dp; val thumbH = if (compact) 14.dp else 24.dp
    val trackH = if (compact) 4.dp else 8.dp
    val density = LocalDensity.current
    val value = (dragging ?: progress()).coerceIn(0f, 1f)
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(if (compact) 28.dp else 44.dp)
            .onSizeChanged { widthPx = it.width.toFloat().coerceAtLeast(1f) }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var v = (down.position.x / widthPx).coerceIn(0f, 1f)
                    dragging = v
                    while (true) {
                        val ev = awaitPointerEventPassMain()
                        val ch = ev.changes.firstOrNull() ?: break
                        v = (ch.position.x / widthPx).coerceIn(0f, 1f)
                        dragging = v
                        if (!ch.pressed) break
                        ch.consume()
                    }
                    onSeek(v)
                    dragging = null
                }
            },
        contentAlignment = Alignment.CenterStart
    ) {
        val trackW = maxWidth
        Box(Modifier.fillMaxWidth().height(trackH).background(c.text.copy(alpha = 0.14f), DeskPill))
        Box(Modifier.width(trackW * value).height(trackH).background(c.accent, DeskPill))
        val x = (trackW - thumbW) * value
        Box(
            Modifier
                .offset(x = x)
                .size(thumbW, thumbH)
                .graphicsLayer { scaleX = pressScale; scaleY = pressScale }
                .background(Color.White, DeskPill)
        )
    }
}

private suspend fun androidx.compose.ui.input.pointer.AwaitPointerEventScope.awaitPointerEventPassMain() = awaitPointerEvent()
