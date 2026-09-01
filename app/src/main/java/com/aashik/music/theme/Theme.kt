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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val AutoDarkColorScheme = darkColorScheme(
    primary = AutoPrimaryCyan,
    onPrimary = AutoDarkBg,
    primaryContainer = AutoDarkCardActive,
    onPrimaryContainer = AutoPrimaryCyan,
    secondary = AutoAccentAmber,
    onSecondary = AutoDarkBg,
    background = AutoDarkBg,
    onBackground = AutoTextPrimary,
    surface = AutoDarkSurface,
    onSurface = AutoTextPrimary,
    surfaceVariant = AutoDarkCard,
    onSurfaceVariant = AutoTextSecondary,
    outline = AutoDarkBorder,
    outlineVariant = AutoDarkBorder
)

private val AutoLightColorScheme = lightColorScheme(
    primary = AutoPrimaryCyanDim,
    onPrimary = AutoLightSurface,
    primaryContainer = AutoLightCardActive,
    onPrimaryContainer = AutoLightTextPrimary,
    secondary = AutoAccentAmber,
    onSecondary = AutoLightSurface,
    background = AutoLightBg,
    onBackground = AutoLightTextPrimary,
    surface = AutoLightSurface,
    onSurface = AutoLightTextPrimary,
    surfaceVariant = AutoLightCard,
    onSurfaceVariant = AutoLightTextSecondary,
    outline = AutoLightBorder,
    outlineVariant = AutoLightBorder
)

private val AutoTypography = Typography(
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 18.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 17.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 15.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 18.sp
    )
)

/**
 * Universal Theme-Responsive Linear Gradients.
 * Eliminates drop shadows / elevations in favor of modern, high-contrast linear gradients.
 */
object AppGradients {
    @Composable
    fun card(isActive: Boolean = false): Brush {
        val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
        return if (isActive) {
            if (isDark) {
                Brush.linearGradient(
                    listOf(
                        Color(0xFF0C2C3D),
                        Color(0xFF131F2E)
                    )
                )
            } else {
                Brush.linearGradient(
                    listOf(
                        Color(0xFFD6F2F8),
                        Color(0xFFCCE4EE)
                    )
                )
            }
        } else {
            if (isDark) {
                Brush.linearGradient(
                    listOf(
                        Color(0xFF1E222B),
                        Color(0xFF15181F)
                    )
                )
            } else {
                Brush.linearGradient(
                    listOf(
                        Color(0xFFFAFCFE),
                        Color(0xFFEDF2F7)
                    )
                )
            }
        }
    }

    @Composable
    fun dock(): Brush {
        val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
        return if (isDark) {
            Brush.verticalGradient(
                listOf(
                    Color(0xFF181B24),
                    Color(0xFF0F1117)
                )
            )
        } else {
            Brush.verticalGradient(
                listOf(
                    Color(0xFFFFFFFF),
                    Color(0xFFEFF3F8)
                )
            )
        }
    }

    @Composable
    fun capsule(isActive: Boolean = false): Brush {
        val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
        return if (isActive) {
            if (isDark) {
                Brush.linearGradient(
                    listOf(
                        Color(0xFF133648),
                        Color(0xFF182433)
                    )
                )
            } else {
                Brush.linearGradient(
                    listOf(
                        Color(0xFFD8F2F8),
                        Color(0xFFC7E2EC)
                    )
                )
            }
        } else {
            if (isDark) {
                Brush.linearGradient(
                    listOf(
                        Color(0xFF242835),
                        Color(0xFF1B1E28)
                    )
                )
            } else {
                Brush.linearGradient(
                    listOf(
                        Color(0xFFFFFFFF),
                        Color(0xFFE9EFF6)
                    )
                )
            }
        }
    }

    @Composable
    fun primaryButton(): Brush {
        return Brush.linearGradient(
            listOf(
                AutoPrimaryCyan,
                Color(0xFF00B0FF)
            )
        )
    }

    @Composable
    fun border(isActive: Boolean = false): Brush {
        val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
        return if (isActive) {
            Brush.linearGradient(
                listOf(
                    AutoPrimaryCyan.copy(alpha = 0.85f),
                    AutoPrimaryCyan.copy(alpha = 0.35f)
                )
            )
        } else {
            if (isDark) {
                Brush.linearGradient(
                    listOf(
                        Color(0xFF353B4B).copy(alpha = 0.8f),
                        Color(0xFF232733).copy(alpha = 0.5f)
                    )
                )
            } else {
                Brush.linearGradient(
                    listOf(
                        Color(0xFFD2DCE6),
                        Color(0xFFE5ECF2)
                    )
                )
            }
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
