package com.aashik.music.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aashik.music.model.Song
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Precomputed shapes hoisted to top-level constants for Mali GPU & zero GC
private val ScrubberStripShape = RoundedCornerShape(12.dp)
private val LetterBubbleShape = RoundedCornerShape(10.dp)

private val ALPHABET_LIST = listOf(
    '#', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
    'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'
)

/**
 * Automotive Fast Alphabet Scrubber / Fast-Scroll Indexer.
 * Allows quick jumping through large audio libraries (500+ songs) without distracting the driver.
 * Optimized for touch targets on 1280x720 automotive displays.
 */
@Composable
fun AlphabetScrubber(
    songs: List<Song>,
    gridState: LazyGridState,
    modifier: Modifier = Modifier
) {
    if (songs.size <= 15) return // Scrubber not needed for small lists

    val coroutineScope = rememberCoroutineScope()
    var activeBubbleLetter by remember { mutableStateOf<Char?>(null) }

    // Precompute letter-to-index mapping (zero allocation on recomposition)
    val letterIndexMap = remember(songs) {
        val map = HashMap<Char, Int>(28)
        for (i in songs.indices) {
            val title = songs[i].title
            val c = title.firstOrNull()?.uppercaseChar() ?: '#'
            val key = if (c in 'A'..'Z') c else '#'
            if (!map.containsKey(key)) {
                map[key] = i
            }
        }
        map
    }

    LaunchedEffect(activeBubbleLetter) {
        if (activeBubbleLetter != null) {
            delay(800)
            activeBubbleLetter = null
        }
    }

    fun jumpToLetter(letter: Char) {
        activeBubbleLetter = letter
        val targetIdx = letterIndexMap[letter] ?: run {
            // Find nearest available letter
            val targetChar = if (letter == '#') 'A' else letter
            val firstAfter = songs.indexOfFirst {
                val c = it.title.firstOrNull()?.uppercaseChar() ?: ' '
                c >= targetChar
            }
            if (firstAfter != -1) firstAfter else 0
        }
        coroutineScope.launch {
            gridState.scrollToItem(targetIdx)
        }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.CenterEnd
    ) {
        // Floating Large Preview Bubble
        AnimatedVisibility(
            visible = activeBubbleLetter != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(end = 28.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = LetterBubbleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = activeBubbleLetter?.toString().orEmpty(),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }

        // Vertical Scrubber Strip
        Column(
            modifier = Modifier
                .width(22.dp)
                .fillMaxHeight()
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                    shape = ScrubberStripShape
                )
                .padding(vertical = 4.dp),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ALPHABET_LIST.forEach { letter ->
                val hasSongs = letterIndexMap.containsKey(letter)
                Text(
                    text = letter.toString(),
                    fontSize = 9.sp,
                    fontWeight = if (hasSongs) FontWeight.Bold else FontWeight.Normal,
                    color = if (hasSongs) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .clickable(enabled = hasSongs) {
                            jumpToLetter(letter)
                        }
                        .padding(vertical = 0.5.dp)
                )
            }
        }
    }
}
