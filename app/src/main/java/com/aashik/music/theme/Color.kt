package com.aashik.music.theme

import androidx.compose.ui.graphics.Color

// =========================================================================
// MATERIAL 3 COLOR TOKENS — AUTOMOTIVE CYBER OBSIDIAN (Dark Mode)
// Matches https://m3.material.io/styles/color/the-color-system/tokens
// Engineered for deep OLED contrast & automotive cockpit legibility
// =========================================================================
val M3DarkPrimary = Color(0xFF00E5FF)               // High-luma electric cyber cyan
val M3DarkOnPrimary = Color(0xFF00363F)
val M3DarkPrimaryContainer = Color(0xFF004E5B)
val M3DarkOnPrimaryContainer = Color(0xFFB5F4FF)
val M3DarkInversePrimary = Color(0xFF006877)

val M3DarkSecondary = Color(0xFFFFB300)             // Vibrant automotive amber
val M3DarkOnSecondary = Color(0xFF422C00)
val M3DarkSecondaryContainer = Color(0xFF5F4100)
val M3DarkOnSecondaryContainer = Color(0xFFFFDF9E)

val M3DarkTertiary = Color(0xFF00E676)              // Vivid status emerald
val M3DarkOnTertiary = Color(0xFF00391A)
val M3DarkTertiaryContainer = Color(0xFF005328)
val M3DarkOnTertiaryContainer = Color(0xFF72FBA3)

val M3DarkBackground = Color(0xFF090C12)            // Deep obsidian midnight base
val M3DarkOnBackground = Color(0xFFF1F5F9)          // Slate 100

val M3DarkSurface = Color(0xFF10141D)               // Elevated dock & panel surface
val M3DarkOnSurface = Color(0xFFF1F5F9)
val M3DarkSurfaceVariant = Color(0xFF171D29)        // Inactive card background
val M3DarkOnSurfaceVariant = Color(0xFF94A3B8)      // Slate 400

val M3DarkSurfaceTint = Color(0xFF00E5FF)
val M3DarkInverseSurface = Color(0xFFF1F5F9)
val M3DarkInverseOnSurface = Color(0xFF090C12)

val M3DarkOutline = Color(0xFF252F42)               // Structural border
val M3DarkOutlineVariant = Color(0xFF1C2433)        // Subtle separation border
val M3DarkScrim = Color(0xFF000000)

val M3DarkError = Color(0xFFFF5252)
val M3DarkOnError = Color(0xFF690005)
val M3DarkErrorContainer = Color(0xFF93000A)
val M3DarkOnErrorContainer = Color(0xFFFFDAD6)

val M3DarkSurfaceContainerLowest = Color(0xFF06090E)
val M3DarkSurfaceContainerLow = Color(0xFF0D1118)
val M3DarkSurfaceContainer = Color(0xFF10141D)
val M3DarkSurfaceContainerHigh = Color(0xFF151B25)
val M3DarkSurfaceContainerHighest = Color(0xFF1B2230)

// Backward-compatible aliases for existing automotive references
val AutoDarkBg = M3DarkBackground
val AutoDarkSurface = M3DarkSurface
val AutoDarkCard = M3DarkSurfaceVariant
val AutoDarkBorder = M3DarkOutline
val AutoPrimaryCyan = M3DarkPrimary
val AutoAccentAmber = M3DarkSecondary
val AutoAccentGreen = M3DarkTertiary
val AutoError = M3DarkError
val AutoTextPrimary = M3DarkOnSurface
val AutoTextSecondary = M3DarkOnSurfaceVariant

// =========================================================================
// MATERIAL 3 COLOR TOKENS — AUTOMOTIVE CERAMIC AZURE (Light Mode)
// High-visibility crisp ceramic & daytime azure cobalt (7:1+ AAA contrast)
// =========================================================================
val M3LightPrimary = Color(0xFF0066FF)              // Daytime high-contrast cobalt azure
val M3LightOnPrimary = Color(0xFFFFFFFF)
val M3LightPrimaryContainer = Color(0xFFD6E4FF)
val M3LightOnPrimaryContainer = Color(0xFF001B3F)
val M3LightInversePrimary = Color(0xFFA8C8FF)

val M3LightSecondary = Color(0xFFD97706)            // Rich daylight amber
val M3LightOnSecondary = Color(0xFFFFFFFF)
val M3LightSecondaryContainer = Color(0xFFFFDDB3)
val M3LightOnSecondaryContainer = Color(0xFF2B1700)

val M3LightTertiary = Color(0xFF059669)             // Daylight emerald status
val M3LightOnTertiary = Color(0xFFFFFFFF)
val M3LightTertiaryContainer = Color(0xFFA3F2D4)
val M3LightOnTertiaryContainer = Color(0xFF002114)

val M3LightBackground = Color(0xFFEEF2F6)           // Cool ceramic pearl background
val M3LightOnBackground = Color(0xFF0F172A)         // Slate 900

val M3LightSurface = Color(0xFFFFFFFF)              // Pure bright surface
val M3LightOnSurface = Color(0xFF0F172A)
val M3LightSurfaceVariant = Color(0xFFE2E8F0)       // Slate 200
val M3LightOnSurfaceVariant = Color(0xFF475569)     // Slate 600

val M3LightSurfaceTint = Color(0xFF0066FF)
val M3LightInverseSurface = Color(0xFF1E293B)
val M3LightInverseOnSurface = Color(0xFFF8FAFC)

val M3LightOutline = Color(0xFFCBD5E1)              // Slate 300
val M3LightOutlineVariant = Color(0xFFE2E8F0)
val M3LightScrim = Color(0xFF000000)

val M3LightError = Color(0xFFBA1A1A)
val M3LightOnError = Color(0xFFFFFFFF)
val M3LightErrorContainer = Color(0xFFFFDAD6)
val M3LightOnErrorContainer = Color(0xFF410002)

val M3LightSurfaceContainerLowest = Color(0xFFFFFFFF)
val M3LightSurfaceContainerLow = Color(0xFFF8FAFC)
val M3LightSurfaceContainer = Color(0xFFF1F5F9)
val M3LightSurfaceContainerHigh = Color(0xFFE2E8F0)
val M3LightSurfaceContainerHighest = Color(0xFFCBD5E1)

// Backward-compatible aliases for existing automotive references
val AutoLightBg = M3LightBackground
val AutoLightSurface = M3LightSurface
val AutoLightCard = M3LightSurface
val AutoLightBorderActive = M3LightPrimary
val AutoPrimaryAzure = M3LightPrimary
val AutoLightAccentAmber = M3LightSecondary
val AutoLightAccentGreen = M3LightTertiary
val AutoLightTextPrimary = M3LightOnSurface
val AutoLightTextSecondary = M3LightOnSurfaceVariant
