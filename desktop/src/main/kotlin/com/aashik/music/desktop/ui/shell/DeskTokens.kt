package com.aashik.music.desktop.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import java.awt.image.BufferedImage

/**
 * Desktop liquid-glass design tokens, ported from the headunit (landscape) design:
 * flat colours, no borders or shadows, accent taken from the artwork.
 */
internal data class DeskPalette(
    val background: Color,
    val panel: Color,
    val text: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val accent: Color,
    val onAccent: Color
) {
    val idleFill: Color get() = text.copy(alpha = 0.08f)
    val selectedFill: Color get() = accent.copy(alpha = 0.16f)
    val hairline: Color get() = text.copy(alpha = 0.10f)
}

internal val DeskDark = DeskPalette(
    background = Color(0xFF0D0F12),
    panel = Color(0xFF141923),
    text = Color.White,
    textSecondary = Color(0xFF94A3B8),
    textTertiary = Color(0xFF64748B),
    accent = Color(0xFF00E5FF),
    onAccent = Color.Black
)

internal val LocalDesk = compositionLocalOf { DeskDark }

internal object Desk {
    val c: DeskPalette @Composable get() = LocalDesk.current
}

internal val DeskPill = RoundedCornerShape(percent = 50)
internal val DeskCircle: Shape = CircleShape
internal val DeskCard = RoundedCornerShape(16.dp)
internal val DeskSheet = RoundedCornerShape(24.dp)
internal val DeskItem = RoundedCornerShape(14.dp)
internal val DeskArt8 = RoundedCornerShape(8.dp)
internal val DeskHeroArt = RoundedCornerShape(16.dp)
internal val DeskCapsule = RoundedCornerShape(34.dp)

/** Translucent frosted panel (flat fill; the desktop has no backdrop lens, see layout notes). */
@Composable
internal fun Modifier.deskGlass(shape: Shape, alpha: Float = 0.82f): Modifier = background(Desk.c.panel.copy(alpha = alpha), shape)

/** Accent colour from artwork: saturation-weighted average, lifted so it reads on the dark background. */
internal fun deskPaletteFromArt(art: ImageBitmap?): DeskPalette {
    art ?: return DeskDark
    return try {
        val src = art.toAwtImage()
        val small = BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB)
        val g = small.createGraphics()
        g.drawImage(src.getScaledInstance(16, 16, java.awt.Image.SCALE_AREA_AVERAGING), 0, 0, null)
        g.dispose()
        var r = 0.0; var gr = 0.0; var b = 0.0; var wSum = 0.0
        for (y in 0 until 16) for (x in 0 until 16) {
            val p = small.getRGB(x, y)
            val rr = (p shr 16 and 0xFF) / 255.0; val gg = (p shr 8 and 0xFF) / 255.0; val bb = (p and 0xFF) / 255.0
            val mx = maxOf(rr, gg, bb); val mn = minOf(rr, gg, bb)
            val w = (mx - mn) * mx + 0.02
            r += rr * w; gr += gg * w; b += bb * w; wSum += w
        }
        val hsv = FloatArray(3)
        java.awt.Color.RGBtoHSB((r / wSum * 255).toInt(), (gr / wSum * 255).toInt(), (b / wSum * 255).toInt(), hsv)
        val accent = java.awt.Color.HSBtoRGB(hsv[0], (hsv[1] * 1.1f).coerceIn(0.35f, 0.85f), 0.95f)
        val bg = java.awt.Color.HSBtoRGB(hsv[0], (hsv[1] * 0.35f).coerceIn(0.05f, 0.3f), 0.09f)
        val panel = java.awt.Color.HSBtoRGB(hsv[0], (hsv[1] * 0.3f).coerceIn(0.05f, 0.28f), 0.15f)
        DeskDark.copy(background = Color(bg), panel = Color(panel), accent = Color(accent), onAccent = Color.Black)
    } catch (_: Throwable) {
        DeskDark
    }
}

internal fun deskFormatTime(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val h = total / 3600; val m = (total % 3600) / 60; val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
