package com.aashik.music.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import com.aashik.music.ui.designsystem.Icon
import com.aashik.music.ui.designsystem.MaterialTheme
import com.aashik.music.ui.designsystem.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aashik.music.viewmodel.MusicViewModel

private val LyricLineShape = RoundedCornerShape(12.dp)
private val FloatingCloseShape = CircleShape

/**
 * Modern, distraction-free synchronized lyrics view embedded seamlessly inside the album art card.
 *
 * Designed with center-aligned typography, generous line spacing, and subtle dismiss controls.
 */
@Composable
fun HudExpandedLyricsView(
    viewModel: MusicViewModel,
    primaryTextColor: Color,
    @Suppress("UNUSED_PARAMETER") secondaryTextColor: Color,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lyrics by viewModel.currentLyrics.collectAsState()
    val positionMs by viewModel.positionFlow.collectAsState()
    val isLoadingLyrics by viewModel.isLoadingLyrics.collectAsState()
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    if (lyrics.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .clickable { onClose() }
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (isLoadingLyrics) "Loading synchronized lyrics..." else "No synchronized lyrics found",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) primaryTextColor else MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (isLoadingLyrics) "Fetching track lines..." else "Tap to return to artwork",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    val currentIndex = remember(lyrics, positionMs) {
        val idx = lyrics.indexOfLast { it.timeMs <= positionMs }
        idx.coerceAtLeast(0)
    }

    val initialIndex = remember {
        val idx = lyrics.indexOfLast { it.timeMs <= positionMs }
        idx.coerceAtLeast(0)
    }

    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = initialIndex,
        initialFirstVisibleItemScrollOffset = 0
    )

    LaunchedEffect(currentIndex) {
        if (currentIndex in lyrics.indices && !listState.isScrollInProgress) {
            listState.animateScrollToItem(
                index = currentIndex,
                scrollOffset = 0
            )
        }
    }

    val lyricsCardBg = when {
        isDark -> Color(0xD906090F)
        else -> MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.95f)
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(lyricsCardBg)
    ) {
        val verticalPadding = (maxHeight / 2 - 20.dp).coerceAtLeast(16.dp)

        // Centered Scrolling Lyrics with Depth-of-Field Position Opacity
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = verticalPadding, bottom = verticalPadding, start = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            itemsIndexed(
                items = lyrics,
                key = { index, line -> "${index}_${line.timeMs}" }
            ) { index, line ->
                val distance = kotlin.math.abs(index - currentIndex)
                val isCurrent = distance == 0

                val targetAlpha = when (distance) {
                    0 -> 1.0f
                    1 -> 0.70f
                    2 -> 0.45f
                    3 -> 0.25f
                    else -> 0.12f
                }

                val animatedAlpha = animateFloatAsState(
                    targetValue = targetAlpha,
                    animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
                    label = "LyricAlpha"
                )

                val targetScale = when (distance) {
                    0 -> 1.10f
                    1 -> 0.90f
                    2 -> 0.83f
                    3 -> 0.75f
                    else -> 0.70f
                }

                val animatedScale = animateFloatAsState(
                    targetValue = targetScale,
                    animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
                    label = "LyricScale"
                )

                val animatedColor by animateColorAsState(
                    targetValue = if (isCurrent) MaterialTheme.colorScheme.primary
                                  else if (isDark) Color.White
                                  else MaterialTheme.colorScheme.onSurface,
                    animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
                    label = "LyricColor"
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            scaleX = animatedScale.value
                            scaleY = animatedScale.value
                            alpha = animatedAlpha.value
                            transformOrigin = TransformOrigin(0.5f, 0.5f)
                        }
                        .clip(LyricLineShape)
                        .clickable {
                            viewModel.seekTo(line.timeMs)
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = line.text.ifBlank { "♪" },
                        fontSize = 17.sp,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                        color = animatedColor,
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Sleek Minimal Floating Close Button in Top-Right Corner
        val closeBtnBg = if (isDark) Color.Black.copy(alpha = 0.45f)
                         else MaterialTheme.colorScheme.surfaceContainerHigh
        val closeBtnTint = if (isDark) Color.White.copy(alpha = 0.85f)
                          else MaterialTheme.colorScheme.onSurface

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp)
                .size(30.dp)
                .background(
                    color = closeBtnBg,
                    shape = FloatingCloseShape
                )
                .clip(FloatingCloseShape)
                .clickable { onClose() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Close Lyrics",
                tint = closeBtnTint,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
