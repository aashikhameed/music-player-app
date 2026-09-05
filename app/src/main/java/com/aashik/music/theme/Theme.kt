package com.aashik.music.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.aashik.music.R

val AppFontFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold)
)

private val AutoDarkColorScheme = darkColorScheme(
    primary = AutoPrimaryCyan,
    onPrimary = Color(0xFF001E2B),
    primaryContainer = AutoDarkCardActive,
    onPrimaryContainer = AutoPrimaryCyan,
    secondary = AutoAccentAmber,
    onSecondary = Color(0xFF1F1200),
    background = AutoDarkBg,
    onBackground = AutoTextPrimary,
    surface = AutoDarkSurface,
    onSurface = AutoTextPrimary,
    surfaceVariant = AutoDarkCard,
    onSurfaceVariant = AutoTextSecondary,
    outline = AutoDarkBorder,
    outlineVariant = Color(0xFF1C2433)
)

private val AutoLightColorScheme = lightColorScheme(
    primary = AutoPrimaryAzure,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = AutoLightCardActive,
    onPrimaryContainer = AutoPrimaryAzure,
    secondary = AutoLightAccentAmber,
    onSecondary = Color(0xFFFFFFFF),
    background = AutoLightBg,
    onBackground = AutoLightTextPrimary,
    surface = AutoLightSurface,
    onSurface = AutoLightTextPrimary,
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = AutoLightTextSecondary,
    outline = AutoLightBorder,
    outlineVariant = Color(0xFFE2E8F0)
)

private val AutoTypography = Typography(
    titleLarge = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    titleMedium = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp
    ),
    titleSmall = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 13.5.sp,
        lineHeight = 18.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.5.sp,
        lineHeight = 16.sp
    ),
    bodySmall = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 15.sp
    ),
    labelLarge = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.5.sp,
        lineHeight = 18.sp
    ),
    labelSmall = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 10.sp,
        lineHeight = 13.sp
    )
)

/**
 * Universal Theme-Responsive Linear Gradients.
 * Eliminates drop shadows / elevations in favor of modern, high-contrast linear gradients.
 */
object AppGradients {
    // Pre-allocated static brushes — Zero heap allocation on recompositions
    private val DarkCardActive = Brush.linearGradient(listOf(Color(0xFF0F2D44), Color(0xFF091C2B)))
    private val LightCardActive = Brush.linearGradient(listOf(Color(0xFFE3F2FD), Color(0xFFCFE6FB)))
    private val DarkCardInactive = Brush.linearGradient(listOf(Color(0xFF161C28), Color(0xFF10141E)))
    private val LightCardInactive = Brush.linearGradient(listOf(Color(0xFFFFFFFF), Color(0xFFF7FAFD)))

    private val DarkDock = Brush.verticalGradient(listOf(Color(0xFF131824), Color(0xFF090C12)))
    private val LightDock = Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFEFF4F9)))

    private val DarkCapsuleActive = Brush.linearGradient(listOf(Color(0xFF10334C), Color(0xFF0C2233)))
    private val LightCapsuleActive = Brush.linearGradient(listOf(Color(0xFFDBEAFE), Color(0xFFBFDBFE)))
    private val DarkCapsuleInactive = Brush.linearGradient(listOf(Color(0xFF19202E), Color(0xFF131824)))
    private val LightCapsuleInactive = Brush.linearGradient(listOf(Color(0xFFFFFFFF), Color(0xFFEDF2F7)))

    private val DarkPrimaryBtn = Brush.linearGradient(listOf(Color(0xFF00E5FF), Color(0xFF0091EA)))
    private val LightPrimaryBtn = Brush.linearGradient(listOf(Color(0xFF0066FF), Color(0xFF0052D4)))

    private val DarkBorderActive = Brush.linearGradient(listOf(Color(0xFF00E5FF).copy(alpha = 0.9f), Color(0xFF0091EA).copy(alpha = 0.45f)))
    private val LightBorderActive = Brush.linearGradient(listOf(Color(0xFF0066FF).copy(alpha = 0.9f), Color(0xFF0052D4).copy(alpha = 0.45f)))
    private val DarkBorderInactive = Brush.linearGradient(listOf(Color(0xFF283448).copy(alpha = 0.85f), Color(0xFF1B2332).copy(alpha = 0.5f)))
    private val LightBorderInactive = Brush.linearGradient(listOf(Color(0xFFD2DEE9), Color(0xFFE2EAF1)))

    @Composable
    fun card(isActive: Boolean = false): Brush {
        val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
        return if (isActive) {
            if (isDark) DarkCardActive else LightCardActive
        } else {
            if (isDark) DarkCardInactive else LightCardInactive
        }
    }

    @Composable
    fun dock(): Brush {
        val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
        return if (isDark) DarkDock else LightDock
    }

    @Composable
    fun capsule(isActive: Boolean = false): Brush {
        val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
        return if (isActive) {
            if (isDark) DarkCapsuleActive else LightCapsuleActive
        } else {
            if (isDark) DarkCapsuleInactive else LightCapsuleInactive
        }
    }

    @Composable
    fun primaryButton(): Brush {
        val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
        return if (isDark) DarkPrimaryBtn else LightPrimaryBtn
    }

    @Composable
    fun border(isActive: Boolean = false): Brush {
        val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
        return if (isActive) {
            if (isDark) DarkBorderActive else LightBorderActive
        } else {
            if (isDark) DarkBorderInactive else LightBorderInactive
        }
    }
}

@Composable
fun CarMusicTheme(
    isDarkMode: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (isDarkMode) AutoDarkColorScheme else AutoLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AutoTypography,
        content = content
    )
}
