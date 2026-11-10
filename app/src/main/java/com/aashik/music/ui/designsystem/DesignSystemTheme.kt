package com.aashik.music.ui.designsystem

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

@Immutable
data class ColorScheme(
    val primary: Color = Color(0xFF00E5FF),
    val onPrimary: Color = Color.Black,
    val primaryContainer: Color = Color(0xFF004D5A),
    val onPrimaryContainer: Color = Color(0xFF90F1FF),
    val inversePrimary: Color = Color(0xFF006876),
    val secondary: Color = Color(0xFFB0C6FF),
    val onSecondary: Color = Color(0xFF002D6E),
    val secondaryContainer: Color = Color(0xFF1E438F),
    val onSecondaryContainer: Color = Color(0xFFD9E2FF),
    val tertiary: Color = Color(0xFF00E5FF),
    val onTertiary: Color = Color.Black,
    val tertiaryContainer: Color = Color(0xFF004F58),
    val onTertiaryContainer: Color = Color(0xFF88F3FF),
    val background: Color = Color(0xFF0F141C),
    val onBackground: Color = Color(0xFFE2E2E9),
    val surface: Color = Color(0xFF131822),
    val onSurface: Color = Color(0xFFE2E2E9),
    val surfaceVariant: Color = Color(0xFF1E2638),
    val onSurfaceVariant: Color = Color(0xFFC3C7D2),
    val surfaceTint: Color = Color(0xFF00E5FF),
    val inverseSurface: Color = Color(0xFFE2E2E9),
    val inverseOnSurface: Color = Color(0xFF2E3135),
    val outline: Color = Color(0xFF8D9199),
    val outlineVariant: Color = Color(0xFF2B3545),
    val scrim: Color = Color.Black,
    val error: Color = Color(0xFFFFB4AB),
    val onError: Color = Color(0xFF690005),
    val errorContainer: Color = Color(0xFF93000A),
    val onErrorContainer: Color = Color(0xFFFFDAD6),
    val surfaceContainerLowest: Color = Color(0xFF0B0E14),
    val surfaceContainerLow: Color = Color(0xFF111620),
    val surfaceContainer: Color = Color(0xFF161C28),
    val surfaceContainerHigh: Color = Color(0xFF1D2432),
    val surfaceContainerHighest: Color = Color(0xFF242C3C)
)

fun darkColorScheme(
    primary: Color = Color(0xFF00E5FF),
    onPrimary: Color = Color.Black,
    primaryContainer: Color = Color(0xFF004D5A),
    onPrimaryContainer: Color = Color(0xFF90F1FF),
    inversePrimary: Color = Color(0xFF006876),
    secondary: Color = Color(0xFFB0C6FF),
    onSecondary: Color = Color(0xFF002D6E),
    secondaryContainer: Color = Color(0xFF1E438F),
    onSecondaryContainer: Color = Color(0xFFD9E2FF),
    tertiary: Color = Color(0xFF00E5FF),
    onTertiary: Color = Color.Black,
    tertiaryContainer: Color = Color(0xFF004F58),
    onTertiaryContainer: Color = Color(0xFF88F3FF),
    background: Color = Color(0xFF0F141C),
    onBackground: Color = Color(0xFFE2E2E9),
    surface: Color = Color(0xFF131822),
    onSurface: Color = Color(0xFFE2E2E9),
    surfaceVariant: Color = Color(0xFF1E2638),
    onSurfaceVariant: Color = Color(0xFFC3C7D2),
    surfaceTint: Color = Color(0xFF00E5FF),
    inverseSurface: Color = Color(0xFFE2E2E9),
    inverseOnSurface: Color = Color(0xFF2E3135),
    outline: Color = Color(0xFF8D9199),
    outlineVariant: Color = Color(0xFF2B3545),
    scrim: Color = Color.Black,
    error: Color = Color(0xFFFFB4AB),
    onError: Color = Color(0xFF690005),
    errorContainer: Color = Color(0xFF93000A),
    onErrorContainer: Color = Color(0xFFFFDAD6),
    surfaceContainerLowest: Color = Color(0xFF0B0E14),
    surfaceContainerLow: Color = Color(0xFF111620),
    surfaceContainer: Color = Color(0xFF161C28),
    surfaceContainerHigh: Color = Color(0xFF1D2432),
    surfaceContainerHighest: Color = Color(0xFF242C3C)
): ColorScheme = ColorScheme(
    primary = primary,
    onPrimary = onPrimary,
    primaryContainer = primaryContainer,
    onPrimaryContainer = onPrimaryContainer,
    inversePrimary = inversePrimary,
    secondary = secondary,
    onSecondary = onSecondary,
    secondaryContainer = secondaryContainer,
    onSecondaryContainer = onSecondaryContainer,
    tertiary = tertiary,
    onTertiary = onTertiary,
    tertiaryContainer = tertiaryContainer,
    onTertiaryContainer = onTertiaryContainer,
    background = background,
    onBackground = onBackground,
    surface = surface,
    onSurface = onSurface,
    surfaceVariant = surfaceVariant,
    onSurfaceVariant = onSurfaceVariant,
    surfaceTint = surfaceTint,
    inverseSurface = inverseSurface,
    inverseOnSurface = inverseOnSurface,
    outline = outline,
    outlineVariant = outlineVariant,
    scrim = scrim,
    error = error,
    onError = onError,
    errorContainer = errorContainer,
    onErrorContainer = onErrorContainer,
    surfaceContainerLowest = surfaceContainerLowest,
    surfaceContainerLow = surfaceContainerLow,
    surfaceContainer = surfaceContainer,
    surfaceContainerHigh = surfaceContainerHigh,
    surfaceContainerHighest = surfaceContainerHighest
)

fun lightColorScheme(
    primary: Color = Color(0xFF0066FF),
    onPrimary: Color = Color.White,
    primaryContainer: Color = Color(0xFFDBEAFE),
    onPrimaryContainer: Color = Color(0xFF001E4D),
    inversePrimary: Color = Color(0xFF00E5FF),
    secondary: Color = Color(0xFF425E91),
    onSecondary: Color = Color.White,
    secondaryContainer: Color = Color(0xFFD9E2FF),
    onSecondaryContainer: Color = Color(0xFF001945),
    tertiary: Color = Color(0xFF006874),
    onTertiary: Color = Color.White,
    tertiaryContainer: Color = Color(0xFF9EEFFD),
    onTertiaryContainer: Color = Color(0xFF001F24),
    background: Color = Color(0xFFF8FAFC),
    onBackground: Color = Color(0xFF191C20),
    surface: Color = Color(0xFFFFFFFF),
    onSurface: Color = Color(0xFF191C20),
    surfaceVariant: Color = Color(0xFFE2E8F0),
    onSurfaceVariant: Color = Color(0xFF43474E),
    surfaceTint: Color = Color(0xFF0066FF),
    inverseSurface: Color = Color(0xFF2E3135),
    inverseOnSurface: Color = Color(0xFFF0F0F4),
    outline: Color = Color(0xFF74777F),
    outlineVariant: Color = Color(0xFFD2DEE9),
    scrim: Color = Color.Black,
    error: Color = Color(0xFFBA1A1A),
    onError: Color = Color.White,
    errorContainer: Color = Color(0xFFFFDAD6),
    onErrorContainer: Color = Color(0xFF410002),
    surfaceContainerLowest: Color = Color(0xFFFFFFFF),
    surfaceContainerLow: Color = Color(0xFFF1F5F9),
    surfaceContainer: Color = Color(0xFFE2E8F0),
    surfaceContainerHigh: Color = Color(0xFFCBD5E1),
    surfaceContainerHighest: Color = Color(0xFF94A3B8)
): ColorScheme = ColorScheme(
    primary = primary,
    onPrimary = onPrimary,
    primaryContainer = primaryContainer,
    onPrimaryContainer = onPrimaryContainer,
    inversePrimary = inversePrimary,
    secondary = secondary,
    onSecondary = onSecondary,
    secondaryContainer = secondaryContainer,
    onSecondaryContainer = onSecondaryContainer,
    tertiary = tertiary,
    onTertiary = onTertiary,
    tertiaryContainer = tertiaryContainer,
    onTertiaryContainer = onTertiaryContainer,
    background = background,
    onBackground = onBackground,
    surface = surface,
    onSurface = onSurface,
    surfaceVariant = surfaceVariant,
    onSurfaceVariant = onSurfaceVariant,
    surfaceTint = surfaceTint,
    inverseSurface = inverseSurface,
    inverseOnSurface = inverseOnSurface,
    outline = outline,
    outlineVariant = outlineVariant,
    scrim = scrim,
    error = error,
    onError = onError,
    errorContainer = errorContainer,
    onErrorContainer = onErrorContainer,
    surfaceContainerLowest = surfaceContainerLowest,
    surfaceContainerLow = surfaceContainerLow,
    surfaceContainer = surfaceContainer,
    surfaceContainerHigh = surfaceContainerHigh,
    surfaceContainerHighest = surfaceContainerHighest
)

@Immutable
data class Typography(
    val displayLarge: TextStyle = TextStyle.Default,
    val displayMedium: TextStyle = TextStyle.Default,
    val displaySmall: TextStyle = TextStyle.Default,
    val headlineLarge: TextStyle = TextStyle.Default,
    val headlineMedium: TextStyle = TextStyle.Default,
    val headlineSmall: TextStyle = TextStyle.Default,
    val titleLarge: TextStyle = TextStyle.Default,
    val titleMedium: TextStyle = TextStyle.Default,
    val titleSmall: TextStyle = TextStyle.Default,
    val bodyLarge: TextStyle = TextStyle.Default,
    val bodyMedium: TextStyle = TextStyle.Default,
    val bodySmall: TextStyle = TextStyle.Default,
    val labelLarge: TextStyle = TextStyle.Default,
    val labelMedium: TextStyle = TextStyle.Default,
    val labelSmall: TextStyle = TextStyle.Default
)

private val ExtraSmallShape = RoundedCornerShape(4.dp)
private val SmallShape = RoundedCornerShape(8.dp)
private val MediumShape = RoundedCornerShape(12.dp)
private val LargeShape = RoundedCornerShape(16.dp)
private val ExtraLargeShape = RoundedCornerShape(28.dp)

@Immutable
data class Shapes(
    val extraSmall: CornerBasedShape = ExtraSmallShape,
    val small: CornerBasedShape = SmallShape,
    val medium: CornerBasedShape = MediumShape,
    val large: CornerBasedShape = LargeShape,
    val extraLarge: CornerBasedShape = ExtraLargeShape
)

val LocalColorScheme = staticCompositionLocalOf { ColorScheme() }
val LocalTypography = staticCompositionLocalOf { Typography() }
val LocalShapes = staticCompositionLocalOf { Shapes() }
val LocalContentColor = staticCompositionLocalOf { Color.White }

object MaterialTheme {
    val colorScheme: ColorScheme
        @Composable
        @ReadOnlyComposable
        get() = LocalColorScheme.current

    val typography: Typography
        @Composable
        @ReadOnlyComposable
        get() = LocalTypography.current

    val shapes: Shapes
        @Composable
        @ReadOnlyComposable
        get() = LocalShapes.current
}

@Composable
fun MaterialTheme(
    colorScheme: ColorScheme = MaterialTheme.colorScheme,
    typography: Typography = MaterialTheme.typography,
    shapes: Shapes = MaterialTheme.shapes,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalColorScheme provides colorScheme,
        LocalTypography provides typography,
        LocalShapes provides shapes,
        LocalContentColor provides colorScheme.onSurface,
        content = content
    )
}
