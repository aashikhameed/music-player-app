package com.aashik.music.ui.mobile.glass

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.Capsule
import com.kyant.shapes.RoundedRectangle

private val ModalSheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
private val DragHandleShape = Capsule()

/**
 * Liquid Glass Modal Bottom Sheet.
 * 
 * Canonical Reference:
 * /Users/aashikhameed/Documents/aashik/projects/AndroidLiquidGlass/app/src/commonMain/kotlin/com/kyant/backdrop/catalog
 * 
 * Displays a floating frosted liquid glass container with top grab handle,
 * backdrop refraction, and smooth spring physics.
 */
@Composable
fun LiquidModalSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
    scrimColor: Color = Color.Black.copy(alpha = 0.55f),
    surfaceColor: Color = MobileTheme.panel.copy(alpha = 0.72f),
    content: @Composable ColumnScope.() -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)),
        exit = fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            // Dismiss Scrim Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(scrimColor)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss
                    )
            )

            // Sheet Container
            AnimatedVisibility(
                visible = visible,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)
                ),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)
                )
            ) {
                val backdropModifier = if (backdrop != null) {
                    Modifier.drawBackdrop(
                        backdrop = backdrop,
                        shape = { ModalSheetShape },
                        effects = {
                            vibrancy()
                            blur(20f.dp.toPx())
                            lens(16f.dp.toPx(), 28f.dp.toPx(), chromaticAberration = true)
                        },
                        highlight = { Highlight.Default },
                        shadow = { null },
                        innerShadow = { null },
                        onDrawSurface = { drawRect(surfaceColor) }
                    )
                } else {
                    Modifier
                        .clip(ModalSheetShape)
                        .background(surfaceColor)
                }

                Box(
                    modifier = modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .then(backdropModifier)
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Drag Grab Handle
                        Box(
                            modifier = Modifier
                                .size(36.dp, 5.dp)
                                .clip(DragHandleShape)
                                .background(MobileTheme.text.copy(alpha = 0.35f))
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        content()
                    }
                }
            }
        }
    }
}

