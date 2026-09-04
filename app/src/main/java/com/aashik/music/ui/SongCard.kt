package com.aashik.music.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aashik.music.model.Song
import com.aashik.music.theme.AppGradients
import java.util.Locale

@Composable
fun LiveAnimatedEqualizer(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary
) {
    val infiniteTransition = rememberInfiniteTransition(label = "Equalizer")

    val h1 by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = if (isPlaying) 0.95f else 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar1"
    )

    val h2 by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = if (isPlaying) 0.2f else 0.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(350, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar2"
    )

    val h3 by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = if (isPlaying) 1.0f else 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar3"
    )

    val h4 by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = if (isPlaying) 0.15f else 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(380, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar4"
    )

    Row(
        modifier = modifier.height(18.dp).width(18.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        val barShape = RoundedCornerShape(1.dp)
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight(h1)
                .clip(barShape)
                .background(barColor)
        )
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight(h2)
                .clip(barShape)
                .background(barColor)
        )
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight(h3)
                .clip(barShape)
                .background(barColor)
        )
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight(h4)
                .clip(barShape)
                .background(barColor)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SongCard(
    song: Song,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
    cardWidth: Dp? = null
) {
    val widthModifier = if (cardWidth != null) Modifier.width(cardWidth) else Modifier.fillMaxWidth()
    val shape = RoundedCornerShape(14.dp)
    val cardGradient = AppGradients.card(isActive = isPlaying)
    val borderBrush = AppGradients.border(isActive = isPlaying)
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    // Codec extension and duration format
    val ext = song.path.substringAfterLast('.', "").uppercase().take(4)
    val formattedDuration = formatSongDuration(song.duration)

    Box(
        modifier = modifier
            .then(widthModifier)
            .height(76.dp) // 1280×720 @ 160dpi: 76dp card = clear automotive touch target
            .clip(shape)
            .background(brush = cardGradient, shape = shape)
            .border(
                border = BorderStroke(if (isPlaying) 1.6.dp else 1.dp, borderBrush),
                shape = shape
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album Artwork with active indicator
            Box(contentAlignment = Alignment.Center) {
                AlbumArtImage(
                    path = song.path,
                    size = 56.dp,  // 56dp — fills the 76dp card nicely at 160dpi
                    borderRadius = 10.dp,
                    isPlaying = isPlaying
                )

                if (isPlaying) {
                    // Semi-translucent dark vignette over art with live equalizer overlay
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.Black.copy(alpha = 0.45f)),
                        contentAlignment = Alignment.Center
                    ) {
                        LiveAnimatedEqualizer(
                            isPlaying = true,
                            barColor = if (isDark) MaterialTheme.colorScheme.primary else Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Metadata Info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = song.title.ifEmpty { "Unknown Title" },
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.SemiBold,
                        fontSize = 15.sp  // 15sp for glanceable at automotive distance on 160dpi
                    ),
                    color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = if (isPlaying) Modifier.basicMarquee() else Modifier
                )

                Spacer(modifier = Modifier.height(3.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = song.artist.ifEmpty { "Unknown Artist" },
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp), // bumped for 160dpi
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    // Codec & Duration Micro-Pill
                    val badgeShape = RoundedCornerShape(6.dp)
                    val badgeBg = if (isPlaying) {
                        MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.22f else 0.14f)
                    } else {
                        if (isDark) Color(0xFF1E2638) else Color(0xFFE2E8F0)
                    }
                    val badgeTextColor = if (isPlaying) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
                    }

                    Box(
                        modifier = Modifier
                            .clip(badgeShape)
                            .background(badgeBg)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (ext.isNotEmpty()) "$ext • $formattedDuration" else formattedDuration,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeTextColor,
                            letterSpacing = 0.4.sp
                        )
                    }
                }
            }
        }
    }
}

private fun formatSongDuration(durationMs: Long): String {
    if (durationMs <= 0) return "00:00"
    val totalSeconds = (durationMs / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}
