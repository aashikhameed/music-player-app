package com.aashik.music.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
    secondaryTextColor: Color,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lyrics by viewModel.currentLyrics.collectAsState()
    val positionMs by viewModel.positionFlow.collectAsState()
    val isLoadingLyrics by viewModel.isLoadingLyrics.collectAsState()

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
                    color = primaryTextColor,
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
        initialFirstVisibleItemIndex = (initialIndex - 1).coerceAtLeast(0)
    )

    LaunchedEffect(currentIndex) {
        if (currentIndex in lyrics.indices) {
            val target = (currentIndex - 1).coerceAtLeast(0)
            listState.animateScrollToItem(
                index = target,
                scrollOffset = 0
            )
        }
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // Centered Scrolling Lyrics
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 48.dp, bottom = 48.dp, start = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            itemsIndexed(
                items = lyrics,
                key = { index, line -> "${index}_${line.timeMs}" }
            ) { index, line ->
                val isCurrent = index == currentIndex
                val isPast = index < currentIndex

                val animatedColor by animateColorAsState(
                    targetValue = when {
                        isCurrent -> MaterialTheme.colorScheme.primary
                        isPast -> secondaryTextColor.copy(alpha = 0.38f)
                        else -> primaryTextColor.copy(alpha = 0.75f)
                    },
                    animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
                    label = "LyricColor"
                )

                val animatedScale by animateFloatAsState(
                    targetValue = if (isCurrent) 1.06f else 1.0f,
                    animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
                    label = "LyricScale"
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            scaleX = animatedScale
                            scaleY = animatedScale
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
                        fontSize = if (isCurrent) 18.sp else 14.sp,
                        fontWeight = if (isCurrent) FontWeight.Black else FontWeight.SemiBold,
                        color = animatedColor,
                        textAlign = TextAlign.Center,
                        lineHeight = if (isCurrent) 24.sp else 20.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Sleek Minimal Floating Close Button in Top-Right Corner
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp)
                .size(30.dp)
                .background(
                    color = Color.Black.copy(alpha = 0.45f),
                    shape = FloatingCloseShape
                )
                .clip(FloatingCloseShape)
                .clickable { onClose() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Close Lyrics",
                tint = primaryTextColor.copy(alpha = 0.85f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
