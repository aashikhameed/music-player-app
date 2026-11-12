package com.aashik.music.ui.car.glass

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aashik.music.model.DriveSide
import com.aashik.music.model.Song
import com.aashik.music.ui.designsystem.CircularProgressIndicator
import com.aashik.music.ui.mobile.glass.MobileTheme
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.vibrancy

// ─────────────────────────────────────────────────────────────────────────────
// Car / landscape liquid glass design tokens (1280×720 @ 160 dpi → 1 dp = 1 px).
// Flat MobileTheme colours only: no gradients, borders or shadows (AGENTS.md §1, §7).
// ─────────────────────────────────────────────────────────────────────────────

/** Driver's side, provided by CarGlassApp; mirrors navigation, controls, sheets and settings toward the driver. */
internal val LocalDriveSide = compositionLocalOf { DriveSide.RIGHT }

/** Library layer recorded for the glass surfaces (capsule, sheets); null outside CarGlassApp. */
internal val LocalCarBackdrop = compositionLocalOf<com.kyant.backdrop.Backdrop?> { null }

/**
 * Frosted liquid-glass surface (vibrancy + blur, no lens: the lens stays on the seek slider only).
 * Needs RenderEffect blur (API 31+); older headunits (Android 9) keep the flat solid panel and skip
 * the backdrop capture entirely.
 */
@Composable
internal fun Modifier.carGlass(radius: Dp, flatShape: androidx.compose.ui.graphics.Shape, flatAlpha: Float = 1f): Modifier {
    val backdrop = LocalCarBackdrop.current
    val cfg = com.aashik.music.ui.mobile.glass.LocalGlassConfig.current
    val glassShape = remember(radius) { com.kyant.shapes.RoundedRectangle(radius) }
    if (backdrop == null || android.os.Build.VERSION.SDK_INT < 31) {
        return this.background(MobileTheme.panel.copy(alpha = flatAlpha), flatShape)
    }
    val surface = MobileTheme.panel.copy(alpha = cfg.surfaceAlpha.coerceAtLeast(0.55f))
    val blurPx = cfg.blurRadius
    val base = MobileTheme.background // regions outside the recorded library layer stay opaque
    return this.drawBackdrop(
        backdrop = backdrop,
        shape = { glassShape },
        effects = {
            vibrancy()
            if (blurPx.value > 0f) blur(blurPx.toPx())
        },
        highlight = { null },
        shadow = { null },
        innerShadow = { null },
        onDrawBackdrop = { drawBackdrop -> drawRect(base); drawBackdrop() },
        onDrawSurface = { drawRect(surface) }
    )
}

internal object CarDims {
    val SidebarWidth = 240.dp
    val SidebarRowHeight = 56.dp
    val ContentGutter = 24.dp
    val RowHeight = 64.dp
    val RowArt = 48.dp
    val CapsuleHeight = 68.dp
    val CapsuleBottomGap = 12.dp
    val Target = 56.dp
}

// Pre-allocated shapes (zero GC churn on recomposition)
internal val CarPillShape = RoundedCornerShape(percent = 50)
internal val CarCardShape = RoundedCornerShape(16.dp)
internal val CarSheetShape = RoundedCornerShape(24.dp)
internal val CarSidebarItemShape = RoundedCornerShape(14.dp)
internal val CarSearchShape = RoundedCornerShape(22.dp)
internal val CarArtShape8 = RoundedCornerShape(8.dp)
internal val CarHeroArtShape = RoundedCornerShape(16.dp)
internal val CarCapsuleShape = RoundedCornerShape(34.dp)
internal val CarCircle = CircleShape

/** Accent fill used for selected pills / active toggles. */
internal val carSelectedFill: Color get() = MobileTheme.accent.copy(alpha = 0.16f)

/** Neutral translucent fill for idle controls (adapts to Light / Dark / Artwork). */
internal val carIdleFill: Color get() = MobileTheme.text.copy(alpha = 0.08f)

/** The hairline divider colour (0.5 dp, 10% text). */
internal val carHairline: Color get() = MobileTheme.text.copy(alpha = 0.10f)

/** Vector glyph that can scale past the 24 dp intrinsic size of the design-system Icon. */
@Composable
internal fun CarGlyph(icon: ImageVector, contentDescription: String?, tint: Color, size: Dp, modifier: Modifier = Modifier) {
    Image(
        painter = rememberVectorPainter(icon),
        contentDescription = contentDescription,
        contentScale = ContentScale.Fit,
        colorFilter = ColorFilter.tint(tint),
        modifier = modifier.size(size)
    )
}

/** Glyph that springs between icons (play ⇄ pause, shuffle ⇄ repeat). */
@Composable
internal fun CarAnimatedGlyph(icon: ImageVector, contentDescription: String?, tint: Color, size: Dp) {
    AnimatedContent(
        targetState = icon,
        transitionSpec = {
            (fadeIn(tween(160)) + scaleIn(initialScale = 0.6f, animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f))) togetherWith
                (fadeOut(tween(120)) + scaleOut(targetScale = 0.6f, animationSpec = tween(120)))
        },
        label = "carGlyph"
    ) { ic -> CarGlyph(ic, contentDescription, tint, size) }
}

/**
 * Round car control: ≥ 56 dp touch target, spring press scale, optional flat fill.
 * [active] paints an accent disc; [progress]/[loading] replace the glyph with a ring.
 */
@Composable
internal fun CarIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = CarDims.Target,
    glyph: Dp = 26.dp,
    tint: Color = MobileTheme.text,
    filled: Boolean = false,
    active: Boolean = false,
    loading: Boolean = false,
    progress: Float? = null,
    enabled: Boolean = true
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.88f else 1f, spring(dampingRatio = 0.6f, stiffness = 400f), label = "carBtnScale")
    val fill by animateColorAsState(
        when {
            active -> MobileTheme.accent
            filled -> carIdleFill
            pressed -> MobileTheme.text.copy(alpha = 0.10f)
            else -> Color.Transparent
        },
        tween(200), label = "carBtnFill"
    )
    val glyphTint = if (active) MobileTheme.onText else tint
    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer { scaleX = scale; scaleY = scale; alpha = if (enabled) 1f else 0.4f }
            .background(fill, CarCircle)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        when {
            progress != null -> CircularProgressIndicator(
                progress = progress.coerceIn(0.04f, 1f),
                color = glyphTint,
                trackColor = glyphTint.copy(alpha = 0.25f),
                strokeWidth = 2.5.dp,
                modifier = Modifier.size(glyph)
            )
            loading -> CircularProgressIndicator(color = glyphTint, strokeWidth = 2.5.dp, modifier = Modifier.size(glyph))
            else -> CarAnimatedGlyph(icon, contentDescription, glyphTint, glyph)
        }
    }
}

/** Codec / source badge shown beside the time counters ("FLAC", "Streaming"…). */
internal fun carFormatBadge(song: Song?): String? {
    song ?: return null
    if (song.isStream()) return "Streaming"
    val ext = song.path.substringAfterLast('.', "").lowercase()
    return when (ext) {
        "flac" -> "FLAC"
        "alac", "wav", "aiff", "aif" -> "Lossless"
        "m4a", "aac" -> "AAC"
        "" -> null
        else -> ext.uppercase()
    }
}

internal fun carFormatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
