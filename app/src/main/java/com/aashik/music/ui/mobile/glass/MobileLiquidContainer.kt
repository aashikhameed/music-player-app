package com.aashik.music.ui.mobile.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop

/**
 * Controller configuration for the mobile liquid glass backing canvas.
 * Implements the architecture defined in aHR0cHM6Ly9reWFudC5naXRib29rLmlvL2JhY2tkcm9wLg==
 */
class MobileLiquidBackdropController(
    val backgroundColor: Color = MobileTheme.background
)

val LocalMobileBackdrop = compositionLocalOf<MobileLiquidBackdropController?> { null }

val LocalBackdrop = compositionLocalOf<Backdrop?> { null }

/**
 * Standalone Static Color Canvas Backdrop.
 * Provides a clean solid static color background without continuous animation loops.
 * Can be directly tagged with Modifier.layerBackdrop(backdrop) as a background layer
 * without wrapping foreground UI composables, preventing HWUI render node loops.
 */
@Composable
fun MobileLiquidCanvas(
    modifier: Modifier = Modifier,
    backgroundColor: Color = MobileTheme.background
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
    )
}

/**
 * Root Container for Android Mobile portrait views.
 * Tags the background layer with layerBackdrop for Kyant0/AndroidLiquidGlass
 * sampling across liquid sliders, buttons, sheets, and dialogs.
 */
@Composable
fun MobileLiquidContainer(
    modifier: Modifier = Modifier,
    backdrop: LayerBackdrop? = null,
    backgroundColor: Color = MobileTheme.background,
    content: @Composable BoxScope.() -> Unit
) {
    val controller = remember { MobileLiquidBackdropController(backgroundColor) }

    CompositionLocalProvider(
        LocalMobileBackdrop provides controller,
        LocalBackdrop provides backdrop
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(backgroundColor)
        ) {
            content()
        }
    }
}
