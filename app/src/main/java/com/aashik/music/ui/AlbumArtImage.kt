package com.aashik.music.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aashik.music.cache.AlbumArtCache
import com.aashik.music.utils.loadAlbumArt
import kotlin.math.absoluteValue

private val PlaceholderBg = Color(0xFF161D2B)
private val Shape20 = RoundedCornerShape(20.dp)
private val Shape16 = RoundedCornerShape(16.dp)
private val Shape14 = RoundedCornerShape(14.dp)
private val Shape12 = RoundedCornerShape(12.dp)
private val Shape10 = RoundedCornerShape(10.dp)
private val Shape8 = RoundedCornerShape(8.dp)
private val Shape6 = RoundedCornerShape(6.dp)
private val Shape4 = RoundedCornerShape(4.dp)

@Composable
fun AlbumArtImage(
    path: String,
    modifier: Modifier = Modifier,
    artworkUrl: String? = null,
    size: Dp = 54.dp,
    borderRadius: Dp = 10.dp,
    isPlaying: Boolean = false,
    isHighRes: Boolean = false
) {
    val shape = when (borderRadius) {
        20.dp -> Shape20
        16.dp -> Shape16
        14.dp -> Shape14
        12.dp -> Shape12
        10.dp -> Shape10
        8.dp -> Shape8
        6.dp -> Shape6
        4.dp -> Shape4
        else -> remember(borderRadius) { RoundedCornerShape(borderRadius) }
    }
    val context = LocalContext.current

    if (!artworkUrl.isNullOrBlank()) {
        AlbumArtCache.registerStreamArtwork(path, artworkUrl, context)
    }

    var bitmap by remember(path, isHighRes) {
        mutableStateOf<Bitmap?>(
            if (isHighRes) AlbumArtCache.getHighRes(path) ?: AlbumArtCache.get(path)
            else AlbumArtCache.get(path)
        )
    }

    if (path.isNotEmpty() && !AlbumArtCache.hasNoArt(path)) {
        if (isHighRes && AlbumArtCache.getHighRes(path) == null) {
            LaunchedEffect(path, artworkUrl) {
                val loaded = AlbumArtCache.getOrLoadHighRes(path) {
                    com.aashik.music.utils.loadAlbumArtHighRes(it, artworkUrl = artworkUrl)
                }
                if (loaded != null) {
                    bitmap = loaded
                }
            }
        } else if (bitmap == null) {
            LaunchedEffect(path, artworkUrl) {
                bitmap = AlbumArtCache.getOrLoad(context, path) { loadAlbumArt(it, artworkUrl = artworkUrl) }
            }
        }
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val glowModifier = if (isPlaying) {
        Modifier.border(
            width = 1.5.dp,
            color = primaryColor.copy(alpha = 0.85f),
            shape = shape
        )
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .size(size)
            .then(glowModifier)
            .clip(shape),
        contentAlignment = Alignment.Center
    ) {
        val currentBitmap = bitmap
        if (currentBitmap != null) {
            Image(
                bitmap = currentBitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // High-Performance Automotive Placeholder (zero canvas/circle overhead for weak GPUs)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(PlaceholderBg),
                contentAlignment = Alignment.Center
            ) {
                val isStream = path.startsWith("http://", ignoreCase = true) || path.startsWith("https://", ignoreCase = true)
                Icon(
                    imageVector = if (isStream) Icons.Rounded.Radio else Icons.Rounded.MusicNote,
                    contentDescription = null,
                    tint = primaryColor.copy(alpha = 0.6f),
                    modifier = Modifier.size(size * 0.45f)
                )
            }
        }
    }
}
