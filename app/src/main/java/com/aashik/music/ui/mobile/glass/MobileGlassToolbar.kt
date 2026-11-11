package com.aashik.music.ui.mobile.glass

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aashik.music.ui.designsystem.Icon
import com.aashik.music.ui.designsystem.Text
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.shapes.Capsule

data class ToolbarAction(val icon: ImageVector, val contentDescription: String, val onClick: () -> Unit)

/** Height of the toolbar row below the status bar; screens pad their content by this + the status bar. */
val MobileToolbarHeight = 60.dp

/**
 * Floating glass toolbar for detail screens (designScreen/mobile/Playlist.png, Album.png):
 * a round glass back button on the left, a glass capsule of actions on the right, and the
 * screen title fading in once the header has scrolled away. Sits inside the status-bar safe area.
 */
@Composable
fun MobileGlassToolbar(
    title: String,
    titleVisibility: () -> Float,
    onBack: () -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    actions: List<ToolbarAction> = emptyList()
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(MobileToolbarHeight)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GlassCircleButton(
            icon = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = "Back",
            backdrop = backdrop,
            onClick = onBack,
            size = 44.dp
        )

        Text(
            text = title,
            color = MobileTheme.text,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
                .graphicsLayer { alpha = titleVisibility() }
        )

        if (actions.isNotEmpty()) {
            GlassActionCapsule(actions = actions, backdrop = backdrop)
        } else {
            Spacer(Modifier.size(44.dp))
        }
    }
}

@Composable
private fun GlassActionCapsule(actions: List<ToolbarAction>, backdrop: Backdrop) {
    val adaptive = rememberAdaptiveLuminance()
    val glass = floatingGlass(elementHeight = 44.dp)
    Row(
        modifier = Modifier
            .height(44.dp)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { Capsule() },
                effects = {
                    vibrancy()
                    if (glass.blur.value > 0f) blur(glass.blur.toPx())
                    if (glass.lensAmount.value > 0f) lens(glass.lensHeight.toPx(), glass.lensAmount.toPx())
                },
                shadow = { null },
                innerShadow = { null },
                onDrawBackdrop = { drawBackdrop ->
                    drawBackdrop()
                    adaptive.layer.record { drawBackdrop() }
                },
                onDrawSurface = { drawRect(MobileTheme.panel.copy(alpha = glass.surfaceAlpha)) }
            )
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        actions.forEach { action ->
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable(onClick = action.onClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = action.icon,
                    contentDescription = action.contentDescription,
                    tint = adaptive.contentColor,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
