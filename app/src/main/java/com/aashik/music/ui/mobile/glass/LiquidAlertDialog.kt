package com.aashik.music.ui.mobile.glass

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import com.aashik.music.ui.designsystem.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.RoundedRectangle

private val AlertDialogShape = RoundedCornerShape(24.dp)

/**
 * Liquid Glass Alert Dialog.
 * 
 * Canonical Reference:
 * /Users/aashikhameed/Documents/aashik/projects/AndroidLiquidGlass/app/src/commonMain/kotlin/com/kyant/backdrop/catalog
 * 
 * Floating popup with refraction, specular highlight, and LiquidButton actions.
 */
@Composable
fun LiquidAlertDialog(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    title: String,
    message: String,
    confirmButtonText: String = "OK",
    onConfirm: () -> Unit,
    dismissButtonText: String? = null,
    onDismissButton: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
    confirmTint: Color = MobileTheme.accent, // Ice-Cyan
    surfaceColor: Color = MobileTheme.panel.copy(alpha = 0.82f)
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)),
        exit = fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Dismiss Scrim
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.60f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismissRequest
                    )
            )

            // Dialog Card
            AnimatedVisibility(
                visible = visible,
                enter = scaleIn(initialScale = 0.90f, animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
                exit = scaleOut(targetScale = 0.90f, animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut()
            ) {
                val backdropModifier = if (backdrop != null) {
                    Modifier.drawBackdrop(
                        backdrop = backdrop,
                        shape = { AlertDialogShape },
                        effects = {
                            vibrancy()
                            blur(24f.dp.toPx())
                            lens(16f.dp.toPx(), 28f.dp.toPx(), chromaticAberration = true)
                        },
                        highlight = { Highlight.Default },
                        shadow = { null },
                        innerShadow = { null },
                        onDrawSurface = { drawRect(surfaceColor) }
                    )
                } else {
                    Modifier
                        .clip(AlertDialogShape)
                        .background(surfaceColor)
                }

                Box(
                    modifier = modifier
                        .widthIn(max = 340.dp)
                        .padding(horizontal = 24.dp)
                        .then(backdropModifier)
                        .padding(24.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = title,
                            color = MobileTheme.text,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Text(
                            text = message,
                            color = MobileTheme.textSecondary,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (dismissButtonText != null && onDismissButton != null) {
                                LiquidButton(
                                    onClick = onDismissButton,
                                    backdrop = backdrop,
                                    surfaceColor = MobileTheme.text.copy(alpha = 0.08f),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = dismissButtonText,
                                        color = MobileTheme.text.copy(alpha = 0.85f),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            LiquidButton(
                                onClick = onConfirm,
                                backdrop = backdrop,
                                tint = confirmTint,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = confirmButtonText,
                                    color = MobileTheme.text,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

