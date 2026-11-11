package com.aashik.music.ui.mobile.glass

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/** The three user-selectable app themes. */
enum class AppThemeMode(val label: String) {
    LIGHT("Light"),
    DARK("Dark"),
    ALBUM_ART("Artwork")
}

/** Semantic colour roles for the mobile (portrait) UI. */
data class MobilePalette(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceDeep: Color,
    val panel: Color,
    val text: Color,
    val onText: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val accent: Color
)

object MobilePalettes {
    val Dark = MobilePalette(
        isDark = true,
        background = Color(0xFF0D0F12),
        surface = Color(0xFF1E293B),
        surfaceDeep = Color(0xFF0F172A),
        panel = Color(0xFF141923),
        text = Color.White,
        onText = Color.Black,
        textSecondary = Color(0xFF94A3B8),
        textTertiary = Color(0xFF64748B),
        accent = Color(0xFF00E5FF)
    )

    val Light = MobilePalette(
        isDark = false,
        background = Color(0xFFF2F4F7),
        surface = Color(0xFFE2E8F0),
        surfaceDeep = Color(0xFFCBD5E1),
        panel = Color(0xFFF8FAFC),
        text = Color(0xFF0F172A),
        onText = Color.White,
        textSecondary = Color(0xFF475569),
        textTertiary = Color(0xFF94A3B8),
        accent = Color(0xFF0891B2)
    )

    /** Dark palette tinted by the dominant colour of the current album artwork. */
    fun fromArt(art: Color): MobilePalette {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(android.graphics.Color.argb(255, (art.red * 255).toInt(), (art.green * 255).toInt(), (art.blue * 255).toInt()), hsv)
        fun hsvColor(h: Float, s: Float, v: Float) =
            Color(android.graphics.Color.HSVToColor(floatArrayOf(h, s.coerceIn(0f, 1f), v.coerceIn(0f, 1f))))
        val sat = hsv[1].coerceIn(0.25f, 0.9f)
        val bg = hsvColor(hsv[0], sat * 0.75f, 0.16f)
        return MobilePalette(
            isDark = true,
            background = bg,
            surface = hsvColor(hsv[0], sat * 0.6f, 0.30f),
            surfaceDeep = hsvColor(hsv[0], sat * 0.7f, 0.10f),
            panel = hsvColor(hsv[0], sat * 0.65f, 0.20f),
            text = Color.White,
            onText = Color.Black,
            textSecondary = lerp(Color.White, hsvColor(hsv[0], 0.5f, 0.9f), 0.35f).copy(alpha = 0.78f),
            textTertiary = lerp(Color.White, hsvColor(hsv[0], 0.5f, 0.9f), 0.35f).copy(alpha = 0.55f),
            accent = hsvColor(hsv[0], sat.coerceAtLeast(0.55f), 1f)
        )
    }

    /** Saturation/brightness-weighted average colour of an artwork thumbnail, or null if it is grey/empty. */
    fun dominantColor(bitmap: Bitmap): Color? {
        var r = 0f; var g = 0f; var b = 0f; var wSum = 0f
        val hsv = FloatArray(3)
        val step = (minOf(bitmap.width, bitmap.height) / 16).coerceAtLeast(1)
        var y = 0
        while (y < bitmap.height) {
            var x = 0
            while (x < bitmap.width) {
                val px = bitmap.getPixel(x, y)
                android.graphics.Color.colorToHSV(px, hsv)
                val w = hsv[1] * hsv[2] + 0.02f
                r += android.graphics.Color.red(px) * w
                g += android.graphics.Color.green(px) * w
                b += android.graphics.Color.blue(px) * w
                wSum += w
                x += step
            }
            y += step
        }
        if (wSum <= 0f) return null
        return Color((r / wSum) / 255f, (g / wSum) / 255f, (b / wSum) / 255f)
    }
}

/**
 * Global theme accessor. Getters read Compose snapshot state, so they are safe to use from
 * composables, draw lambdas and modifiers alike and trigger recomposition/redraw on change.
 */
object MobileTheme {
    var palette: MobilePalette by mutableStateOf(MobilePalettes.Dark)
    val isDark: Boolean get() = palette.isDark
    val background: Color get() = palette.background
    val surface: Color get() = palette.surface
    val surfaceDeep: Color get() = palette.surfaceDeep
    val panel: Color get() = palette.panel
    val text: Color get() = palette.text
    val onText: Color get() = palette.onText
    val textSecondary: Color get() = palette.textSecondary
    val textTertiary: Color get() = palette.textTertiary
    val accent: Color get() = palette.accent
}
