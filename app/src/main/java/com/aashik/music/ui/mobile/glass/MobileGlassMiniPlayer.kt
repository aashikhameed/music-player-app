package com.aashik.music.ui.mobile.glass

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.expandHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.ui.unit.Dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aashik.music.model.Song
import com.aashik.music.ui.designsystem.Icon
import com.aashik.music.ui.designsystem.Text
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.shapes.Capsule

private val MiniArtShape = RoundedCornerShape(8.dp)

/** Shared with the collapsed tab-bar circles so the whole dock row lines up. */
val MiniPlayerHeight = 60.dp

/** Height of the collapsed dock row (mini-player + round tab buttons), ~75% of the 64dp tab bar. */
val CollapsedDockHeight = 48.dp

/** Horizontal gutter shared by the tab bar, mini-player and collapsed buttons. */
val DockGutter = 24.dp

/**
 * Floating mini-player capsule (designScreen/mobile/Playlist.png):
 * small rounded artwork, title + artist, plain play/pause glyph and next.
 * Same glass as the LiquidBottomTabs panel; text and icons adapt to the luminance of
 * whatever is behind the glass (catalog AdaptiveLuminanceGlassContent).
 * [compact] hides the next button when the capsule shares the row with the collapsed tab bar.
 */
@Composable
fun MobileGlassMiniPlayer(
    currentSong: Song?,
    isPlaying: Boolean,
    albumArt: ImageBitmap?,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onOpenNowPlaying: () -> Unit,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
    compact: Boolean = false,
    height: Dp = MiniPlayerHeight
) {
    val adaptive = rememberAdaptiveLuminance()
    val glass = floatingGlass(elementHeight = height)
    // Artwork is 70% of the capsule height and inset equally on top, bottom and start (concentric).
    val artSize = height * 0.7f
    // Type steps down with the capsule so the two-line stack keeps its padding when collapsed.
    val isSmall = height < 54.dp
    val contentColor = adaptive.contentColor

    val glassModifier = if (backdrop != null) {
        Modifier.drawBackdrop(
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
    } else {
        Modifier.clip(Capsule()).background(MobileTheme.panel)
    }

    Row(
        modifier = modifier
            .height(height)
            .then(glassModifier)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onOpenNowPlaying
            )
            .padding(start = (height - artSize) / 2, end = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(artSize)
                .clip(MiniArtShape)
                .background(contentColor.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center
        ) {
            if (albumArt != null) {
                Image(
                    bitmap = albumArt,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = adaptive.secondaryColor, modifier = Modifier.size(20.dp))
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f).padding(end = 4.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(
                text = currentSong?.title ?: "Not Playing",
                color = contentColor,
                fontSize = if (isSmall) 14.sp else 15.sp,
                lineHeight = if (isSmall) 17.sp else 18.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = currentSong?.artist ?: "",
                color = adaptive.secondaryColor,
                fontSize = if (isSmall) 12.sp else 13.sp,
                lineHeight = if (isSmall) 14.sp else 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .clickable(onClick = onPlayPause),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = contentColor,
                modifier = Modifier.size(28.dp)
            )
        }

        AnimatedVisibility(
            visible = !compact,
            enter = fadeIn() + expandHorizontally(),
            exit = fadeOut() + shrinkHorizontally()
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onNext),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.FastForward,
                    contentDescription = "Next",
                    tint = contentColor,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}
