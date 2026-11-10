package com.aashik.music.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import com.aashik.music.ui.designsystem.MaterialTheme
import com.aashik.music.ui.designsystem.Shapes
import com.aashik.music.ui.designsystem.Typography
import com.aashik.music.ui.designsystem.darkColorScheme
import com.aashik.music.ui.designsystem.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aashik.music.R

val AppFontFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold)
)

// Pre-allocated static shapes to eliminate GC churn on low-spec hardware (M3 Shape Scale)
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

private val AutoDarkColorScheme = darkColorScheme(
    primary = M3DarkPrimary,
    onPrimary = M3DarkOnPrimary,
    primaryContainer = M3DarkPrimaryContainer,
    onPrimaryContainer = M3DarkOnPrimaryContainer,
    inversePrimary = M3DarkInversePrimary,
    secondary = M3DarkSecondary,
    onSecondary = M3DarkOnSecondary,
    secondaryContainer = M3DarkSecondaryContainer,
    onSecondaryContainer = M3DarkOnSecondaryContainer,
    tertiary = M3DarkTertiary,
    onTertiary = M3DarkOnTertiary,
    tertiaryContainer = M3DarkTertiaryContainer,
    onTertiaryContainer = M3DarkOnTertiaryContainer,
    background = M3DarkBackground,
    onBackground = M3DarkOnBackground,
    surface = M3DarkSurface,
    onSurface = M3DarkOnSurface,
    surfaceVariant = M3DarkSurfaceVariant,
    onSurfaceVariant = M3DarkOnSurfaceVariant,
    surfaceTint = M3DarkSurfaceTint,
    inverseSurface = M3DarkInverseSurface,
    inverseOnSurface = M3DarkInverseOnSurface,
    outline = M3DarkOutline,
    outlineVariant = M3DarkOutlineVariant,
    scrim = M3DarkScrim,
    error = M3DarkError,
    onError = M3DarkOnError,
    errorContainer = M3DarkErrorContainer,
    onErrorContainer = M3DarkOnErrorContainer,
    surfaceContainerLowest = M3DarkSurfaceContainerLowest,
    surfaceContainerLow = M3DarkSurfaceContainerLow,
    surfaceContainer = M3DarkSurfaceContainer,
    surfaceContainerHigh = M3DarkSurfaceContainerHigh,
    surfaceContainerHighest = M3DarkSurfaceContainerHighest
)

private val AutoLightColorScheme = lightColorScheme(
    primary = M3LightPrimary,
    onPrimary = M3LightOnPrimary,
    primaryContainer = M3LightPrimaryContainer,
    onPrimaryContainer = M3LightOnPrimaryContainer,
    inversePrimary = M3LightInversePrimary,
    secondary = M3LightSecondary,
    onSecondary = M3LightOnSecondary,
    secondaryContainer = M3LightSecondaryContainer,
    onSecondaryContainer = M3LightOnSecondaryContainer,
    tertiary = M3LightTertiary,
    onTertiary = M3LightOnTertiary,
    tertiaryContainer = M3LightTertiaryContainer,
    onTertiaryContainer = M3LightOnTertiaryContainer,
    background = M3LightBackground,
    onBackground = M3LightOnBackground,
    surface = M3LightSurface,
    onSurface = M3LightOnSurface,
    surfaceVariant = M3LightSurfaceVariant,
    onSurfaceVariant = M3LightOnSurfaceVariant,
    surfaceTint = M3LightSurfaceTint,
    inverseSurface = M3LightInverseSurface,
    inverseOnSurface = M3LightInverseOnSurface,
    outline = M3LightOutline,
    outlineVariant = M3LightOutlineVariant,
    scrim = M3LightScrim,
    error = M3LightError,
    onError = M3LightOnError,
    errorContainer = M3LightErrorContainer,
    onErrorContainer = M3LightOnErrorContainer,
    surfaceContainerLowest = M3LightSurfaceContainerLowest,
    surfaceContainerLow = M3LightSurfaceContainerLow,
    surfaceContainer = M3LightSurfaceContainer,
    surfaceContainerHigh = M3LightSurfaceContainerHigh,
    surfaceContainerHighest = M3LightSurfaceContainerHighest
)

// Material 3 Official 15-tier Typography Scale (m3.material.io/styles/typography/tokens)
private val AutoTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = 0.sp
    ),
    displaySmall = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    labelLarge = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

/**
 * Universal Theme-Responsive Linear Gradients.
 * Pre-allocated static brushes — Zero fragment shader overhead for weak automotive Mali GPUs.
 */
object AppGradients {
    private val DarkCardActive = SolidColor(Color(0xFF0F2D44))
    private val LightCardActive = SolidColor(Color(0xFFE3F2FD))
    private val DarkCardInactive = SolidColor(Color(0xFF161C28))
    private val LightCardInactive = SolidColor(Color(0xFFFFFFFF))

    private val DarkDock = SolidColor(Color(0xFF131824))
    private val LightDock = SolidColor(Color(0xFFFFFFFF))

    private val DarkCapsuleActive = SolidColor(Color(0xFF10334C))
    private val LightCapsuleActive = SolidColor(Color(0xFFDBEAFE))
    private val DarkCapsuleInactive = SolidColor(Color(0xFF19202E))
    private val LightCapsuleInactive = SolidColor(Color(0xFFFFFFFF))

    private val DarkPrimaryBtn = SolidColor(Color(0xFF00E5FF))
    private val LightPrimaryBtn = SolidColor(Color(0xFF0066FF))

    private val DarkBorderActive = SolidColor(Color(0xFF00E5FF))
    private val LightBorderActive = SolidColor(Color(0xFF0066FF))
    private val DarkBorderInactive = SolidColor(Color(0xFF283448))
    private val LightBorderInactive = SolidColor(Color(0xFFD2DEE9))

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
        shapes = AppShapes,
        content = content
    )
}
