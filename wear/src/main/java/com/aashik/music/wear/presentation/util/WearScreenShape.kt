package com.aashik.music.wear.presentation.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration

/**
 * Returns true if the active Wear OS device has a circular screen,
 * or false if it is a square / rectangular screen.
 */
@Composable
fun isScreenRound(): Boolean {
    val configuration = LocalConfiguration.current
    return remember(configuration) { configuration.isScreenRound }
}
