package com.aashik.music.ui.mobile.glass

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import com.aashik.music.ui.designsystem.Icon
import com.aashik.music.ui.designsystem.Text
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Search
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.Capsule
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign

val LocalLiquidBottomTabScale =
    staticCompositionLocalOf { { 1f } }

val LocalLiquidBottomTabAlpha =
    staticCompositionLocalOf<(Int) -> Float> { { 1f } }

/**
 * 1:1 Implementation of LiquidBottomTab from Kyant0/AndroidLiquidGlass.
 * Source: /Users/aashikhameed/Documents/aashik/projects/AndroidLiquidGlass/app/src/commonMain/kotlin/com/kyant/backdrop/catalog/components/LiquidBottomTab.kt
 */
@Composable
fun RowScope.LiquidBottomTab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    index: Int = -1,
    content: @Composable ColumnScope.() -> Unit
) {
    val scale = LocalLiquidBottomTabScale.current
    val alphaProvider = LocalLiquidBottomTabAlpha.current
    Column(
        modifier
            .clip(Capsule())
            .clickable(
                interactionSource = null,
                indication = null,
                role = Role.Tab,
                onClick = onClick
            )
            .fillMaxHeight()
            .weight(1f)
            .graphicsLayer {
                val scaleValue = scale()
                scaleX = scaleValue
                scaleY = scaleValue
                if (index >= 0) {
                    alpha = alphaProvider(index)
                }
            },
        verticalArrangement = Arrangement.spacedBy(2f.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content
    )
}

/**
 * 1:1 Implementation of LiquidBottomTabs from Kyant0/AndroidLiquidGlass.
 * Source: /Users/aashikhameed/Documents/aashik/projects/AndroidLiquidGlass/app/src/commonMain/kotlin/com/kyant/backdrop/catalog/components/LiquidBottomTabs.kt
 */
@Composable
fun LiquidBottomTabs(
    selectedTabIndex: () -> Int,
    onTabSelected: (index: Int) -> Unit,
    backdrop: Backdrop,
    tabsCount: Int,
    modifier: Modifier = Modifier,
    accentColor: Color = MobileTheme.accent,
    containerColor: Color = MobileTheme.panel.copy(LocalGlassConfig.current.surfaceAlpha),
    luminanceLayer: androidx.compose.ui.graphics.layer.GraphicsLayer? = null,
    content: @Composable RowScope.() -> Unit
) {
    val tabsBackdrop = rememberLayerBackdrop()
    val glass = floatingGlass(elementHeight = 64.dp)
    val currentOnTabSelected by rememberUpdatedState(onTabSelected)

    BoxWithConstraints(
        modifier,
        contentAlignment = Alignment.CenterStart
    ) {
        val density = LocalDensity.current
        val tabWidth = with(density) {
            (constraints.maxWidth.toFloat() - 8f.dp.toPx()) / tabsCount
        }

        val offsetAnimation = remember { Animatable(0f) }
        val panelOffset by remember(density) {
            derivedStateOf {
                val fraction = (offsetAnimation.value / constraints.maxWidth).fastCoerceIn(-1f, 1f)
                with(density) {
                    4f.dp.toPx() * fraction.sign * EaseOut.transform(abs(fraction))
                }
            }
        }

        val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
        val animationScope = rememberCoroutineScope()
        // Used to detect the finger leaving the screen: the lens then drops onto the nearest tab.
        var indicatorCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
        val windowInfo = LocalWindowInfo.current
        val edgeSlopPx = with(density) { 6.dp.toPx() }
        val dampedDragAnimation = remember(animationScope) {
            DampedDragAnimation(
                animationScope = animationScope,
                initialValue = selectedTabIndex().toFloat().coerceIn(0f, (tabsCount - 1).toFloat()),
                valueRange = 0f..(tabsCount - 1).toFloat(),
                visibilityThreshold = 0.001f,
                initialScale = 1f,
                pressedScale = 78f / 56f,
                onDragStarted = {},
                shouldRelease = { change ->
                    val coords = indicatorCoords
                    if (coords == null || !coords.isAttached) false
                    else {
                        val p = coords.localToWindow(change.position)
                        val w = windowInfo.containerSize.width
                        val h = windowInfo.containerSize.height
                        p.x <= edgeSlopPx || p.x >= w - edgeSlopPx || p.y <= edgeSlopPx || p.y >= h - edgeSlopPx
                    }
                },
                onDragStopped = {
                    val targetIndex = targetValue.fastRoundToInt().fastCoerceIn(0, tabsCount - 1)
                    animateToValue(targetIndex.toFloat())
                    animationScope.launch {
                        offsetAnimation.animateTo(
                            0f,
                            spring(1f, 300f, 0.5f)
                        )
                    }
                    currentOnTabSelected(targetIndex)
                },
                onDrag = { _, dragAmount ->
                    updateValue(
                        (targetValue + dragAmount.x / tabWidth * if (isLtr) 1f else -1f)
                            .fastCoerceIn(0f, (tabsCount - 1).toFloat())
                    )
                    animationScope.launch {
                        offsetAnimation.snapTo(offsetAnimation.value + dragAmount.x)
                    }
                }
            )
        }

        val targetIndex = selectedTabIndex()
        LaunchedEffect(targetIndex) {
            if (targetIndex in 0 until tabsCount && dampedDragAnimation.targetValue.fastRoundToInt() != targetIndex) {
                dampedDragAnimation.animateToValue(targetIndex.toFloat())
            }
        }

        val interactiveHighlight = remember(animationScope) {
            InteractiveHighlight(
                animationScope = animationScope,
                position = { size, _ ->
                    Offset(
                        if (isLtr) (dampedDragAnimation.value + 0.5f) * tabWidth + panelOffset
                        else size.width - (dampedDragAnimation.value + 0.5f) * tabWidth + panelOffset,
                        size.height / 2f
                    )
                }
            )
        }

        CompositionLocalProvider(
            LocalLiquidBottomTabAlpha provides { tabIndex: Int ->
                if (targetIndex < 0) 1f
                else (abs(dampedDragAnimation.value - tabIndex.toFloat()) / 0.5f).fastCoerceIn(0f, 1f)
            }
        ) {
            Row(
                Modifier
                    .graphicsLayer {
                        translationX = panelOffset
                    }
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { Capsule() },
                        // 1:1 catalog LiquidBottomTabs panel: vibrancy + light blur + resting lens.
                        effects = {
                            vibrancy()
                            if (glass.blur.value > 0f) blur(glass.blur.toPx())
                            if (glass.lensAmount.value > 0f) lens(glass.lensHeight.toPx(), glass.lensAmount.toPx())
                        },
                        layerBlock = {
                            val progress = dampedDragAnimation.pressProgress
                            val scale = lerp(1f, 1f + 16f.dp.toPx() / size.width, progress)
                            scaleX = scale
                            scaleY = scale
                        },
                        shadow = { null },
                        onDrawBackdrop = { drawBackdrop ->
                            drawBackdrop()
                            luminanceLayer?.record { drawBackdrop() }
                        },
                        onDrawSurface = { drawRect(containerColor) }
                    )
                    .then(interactiveHighlight.modifier)
                    .height(64f.dp)
                    .fillMaxWidth()
                    .padding(4f.dp),
                verticalAlignment = Alignment.CenterVertically,
                content = content
            )
        }

        if (targetIndex in 0 until tabsCount) {
            CompositionLocalProvider(
                LocalLiquidBottomTabAlpha provides { _: Int -> 1f },
                LocalLiquidBottomTabScale provides {
                    lerp(1f, 1.2f, dampedDragAnimation.pressProgress)
                }
            ) {
                Row(
                    Modifier
                        .clearAndSetSemantics {}
                        .alpha(0f)
                        .layerBackdrop(tabsBackdrop)
                        .graphicsLayer {
                            translationX = panelOffset
                        }
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { Capsule() },
                            effects = {
                                val progress = dampedDragAnimation.pressProgress
                                vibrancy()
                                if (glass.blur.value > 0f) blur(glass.blur.toPx())
                                if (glass.lensAmount.value > 0f) lens(
                                    glass.lensHeight.toPx() * progress,
                                    glass.lensAmount.toPx() * progress
                                )
                            },
                            highlight = {
                                val progress = dampedDragAnimation.pressProgress
                                Highlight.Default.copy(alpha = progress)
                            },
                            shadow = { null },
                            onDrawSurface = { drawRect(containerColor) }
                        )
                        .then(interactiveHighlight.modifier)
                        .height(56f.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 4f.dp)
                        .graphicsLayer(colorFilter = ColorFilter.tint(accentColor)),
                    verticalAlignment = Alignment.CenterVertically,
                    content = content
                )
            }

            Box(
                Modifier
                    .padding(horizontal = 4f.dp)
                    .graphicsLayer {
                        translationX =
                            if (isLtr) dampedDragAnimation.value * tabWidth + panelOffset
                            else size.width - (dampedDragAnimation.value + 1f) * tabWidth + panelOffset
                    }
                    .then(interactiveHighlight.gestureModifier)
                    .onGloballyPositioned { indicatorCoords = it }
                    .then(dampedDragAnimation.modifier)
                    .drawBackdrop(
                        backdrop = rememberCombinedBackdrop(backdrop, tabsBackdrop),
                        shape = { Capsule() },
                        effects = {
                            val progress = dampedDragAnimation.pressProgress
                            lens(
                                10f.dp.toPx() * progress,
                                14f.dp.toPx() * progress,
                                chromaticAberration = glass.chromatic
                            )
                        },
                        highlight = {
                            val progress = dampedDragAnimation.pressProgress
                            Highlight.Default.copy(alpha = progress)
                        },
                        shadow = { null },
                        innerShadow = { null },
                        layerBlock = {
                            scaleX = dampedDragAnimation.scaleX
                            scaleY = dampedDragAnimation.scaleY
                            val velocity = dampedDragAnimation.velocity / 10f
                            scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                            scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                        },
                        onDrawSurface = {
                            val progress = dampedDragAnimation.pressProgress
                            drawRect(
                                if (MobileTheme.isDark) Color.White.copy(0.1f) else Color.Black.copy(0.1f),
                                alpha = 1f - progress
                            )
                            drawRect(Color.Black.copy(alpha = 0.03f * progress))
                        }
                    )
                    .height(56f.dp)
                    .fillMaxWidth(1f / tabsCount)
            )
        }
    }
}

/**
 * Mobile application bottom navigation bar wrapper for LiquidBottomTabs.
 */
@Composable
fun LiquidBottomBar(
    tabs: List<MobileNavTab>,
    selectedTab: MobileNavTab,
    onTabSelected: (MobileNavTab) -> Unit,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
    accentColor: Color = MobileTheme.accent,
    inactiveColor: Color = MobileTheme.textSecondary
) {
    val nonNullBackdrop = backdrop ?: rememberLayerBackdrop()
    val selectedIndex = tabs.indexOf(selectedTab)
    // Tab icons/labels adapt to the luminance of the content behind the glass.
    val adaptive = rememberAdaptiveLuminance()

    LiquidBottomTabs(
        selectedTabIndex = { selectedIndex },
        onTabSelected = { index ->
            if (index in tabs.indices) {
                onTabSelected(tabs[index])
            }
        },
        backdrop = nonNullBackdrop,
        tabsCount = tabs.size,
        modifier = modifier.fillMaxWidth(),
        accentColor = accentColor,
        containerColor = MobileTheme.panel.copy(LocalGlassConfig.current.surfaceAlpha),
        luminanceLayer = adaptive.layer
    ) {
        tabs.forEachIndexed { index, tab ->
            LiquidBottomTab(
                index = index,
                onClick = {
                    if (index in tabs.indices) {
                        onTabSelected(tabs[index])
                    }
                }
            ) {
                Icon(
                    imageVector = tab.icon,
                    contentDescription = tab.title,
                    tint = adaptive.contentColor,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = tab.title,
                    color = adaptive.contentColor,
                    fontSize = 11.sp
                )
            }
        }
    }
}
