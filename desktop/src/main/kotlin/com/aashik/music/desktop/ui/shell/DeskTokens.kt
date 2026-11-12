package com.aashik.music.desktop.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
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
    background = Color(0xFF1E1E22),
    panel = Color(0xFF26262E),
    text = Color.White,
    textSecondary = Color(0xFF98989F),
    textTertiary = Color(0xFF6C6C74),
    accent = Color(0xFFFA2D48),
    onAccent = Color.White
)

internal val DeskLight = DeskPalette(
    background = Color(0xFFF5F5F7),
    panel = Color(0xFFE9E9EE),
    text = Color(0xFF1D1D1F),
    textSecondary = Color(0xFF6E6E73),
    textTertiary = Color(0xFF9A9AA0),
    accent = Color(0xFFFA2D48),
    onAccent = Color.White
)

internal enum class DeskThemeMode(val label: String) { LIGHT("Light"), DARK("Dark"), ARTWORK("Album Art") }

internal object DeskPrefs {
    private val node = java.util.prefs.Preferences.userRoot().node("com/aashik/music/desktop")
    fun theme(): DeskThemeMode = runCatching { DeskThemeMode.valueOf(node.get("theme", "ARTWORK")) }.getOrDefault(DeskThemeMode.ARTWORK)
    fun setTheme(m: DeskThemeMode) { node.put("theme", m.name) }
    fun plays(id: String): Int = node.getInt("p" + (id.hashCode().toLong() and 0xffffffffL), 0)
    fun addPlay(id: String) { node.putInt("p" + (id.hashCode().toLong() and 0xffffffffL), plays(id) + 1) }
    private val favFile get() = java.io.File(System.getProperty("user.home"), ".musicplayer/favorites.txt")
    fun loadFavorites(): List<String> = runCatching { favFile.readLines().filter { it.isNotBlank() } }.getOrDefault(emptyList())
    fun saveFavorites(ids: List<String>) { runCatching { favFile.parentFile.mkdirs(); favFile.writeText(ids.joinToString("\n")) } }
}

internal fun deskPalette(mode: DeskThemeMode, art: ImageBitmap?): DeskPalette = when (mode) {
    DeskThemeMode.LIGHT -> DeskLight
    DeskThemeMode.DARK -> DeskDark
    DeskThemeMode.ARTWORK -> deskPaletteFromArt(art)
}

/** Whole page (wallpaper + sidebar + content + panel): sampled by floating glass (mini player, suggestions, sheets). */
internal val LocalDeskBackdrop = compositionLocalOf<com.kyant.backdrop.Backdrop?> { null }

/** Wallpaper only: sampled by glass that lives on the page itself (sidebar, panels, cards, pills, buttons). */
internal val LocalDeskWallpaper = compositionLocalOf<com.kyant.backdrop.Backdrop?> { null }

/** Real Kyant liquid glass: vibrancy + blur + (optional) lens over the page behind. Flat panel when no backdrop. */
@Composable
internal fun Modifier.deskLiquid(radius: androidx.compose.ui.unit.Dp, lens: Boolean = true, surfaceAlpha: Float = 0.35f, floating: Boolean = false, blurDp: Float = 2f): Modifier {
    // Glass never samples a layer it is drawn into: page items use the wallpaper, floating items the whole page.
    val backdrop = if (floating) LocalDeskBackdrop.current else LocalDeskWallpaper.current
    val c = Desk.c
    val shape = remember(radius) { com.kyant.shapes.RoundedRectangle(radius) }
    if (backdrop == null) return background(c.panel.copy(alpha = 0.92f), RoundedCornerShape(radius))
    val surface = c.panel.copy(alpha = surfaceAlpha)
    return drawBackdrop(
        backdrop = backdrop,
        shape = { shape },
        effects = {
            if (!floating) vibrancy()
            if (blurDp > 0f) blur(blurDp.dp.toPx())
            if (lens) lens(if (floating) 24.dp.toPx() else 12.dp.toPx(), 24.dp.toPx())
        },
        highlight = { null },
        shadow = { null },
        innerShadow = { null },
                onDrawSurface = { drawRect(surface) }
    )
}

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
        // Background colour picked from the artwork (dark enough for white text); accent and text stay fixed.
        val bg = java.awt.Color.HSBtoRGB(hsv[0], hsv[1].coerceIn(0.35f, 0.75f), 0.30f)
        val panel = java.awt.Color.HSBtoRGB(hsv[0], hsv[1].coerceIn(0.3f, 0.7f), 0.38f)
        DeskDark.copy(background = Color(bg), panel = Color(panel))
    } catch (_: Throwable) {
        DeskDark
    }
}

internal fun deskFormatTime(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val h = total / 3600; val m = (total % 3600) / 60; val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

/** Plain (non-glass) base surface: sidebar, cards, tiles and side panels. Glass is reserved for controls and floating UI. */
@Composable
internal fun Modifier.deskSurface(radius: androidx.compose.ui.unit.Dp): Modifier {
    val c = Desk.c
    return background(c.idleFill, RoundedCornerShape(radius))
}
