package com.aashik.music.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
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
