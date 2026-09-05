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

// Automotive curated gradient pairings for generative vinyl artwork
private val AutomotiveGradients = listOf(
    listOf(Color(0xFF00E5FF), Color(0xFF0052D4)), // Cyber Cyan -> Deep Cobalt
    listOf(Color(0xFFFF2A6D), Color(0xFF05D9E8)), // Neon Cyberpunk
    listOf(Color(0xFFFF9A8B), Color(0xFFFF6A88)), // Sunset Peach
    listOf(Color(0xFFF7971E), Color(0xFFFFD200)), // Warm Amber Gold
    listOf(Color(0xFF00E676), Color(0xFF1DE9B6)), // Emerald Mint
    listOf(Color(0xFF7F00FF), Color(0xFFE100FF))  // Electric Violet
)

private val AutomotiveGradientBrushes = AutomotiveGradients.map { colors ->
    Brush.linearGradient(colors = colors, start = Offset.Zero, end = Offset.Infinite)
}

@Composable
fun AlbumArtImage(
    path: String,
    modifier: Modifier = Modifier,
    size: Dp = 54.dp,
    borderRadius: Dp = 10.dp,
    isPlaying: Boolean = false
) {
    val shape = remember(borderRadius) { RoundedCornerShape(borderRadius) }
    val context = LocalContext.current

    var bitmap by remember(path) {
        mutableStateOf<Bitmap?>(AlbumArtCache.get(path))
    }

    if (bitmap == null && path.isNotEmpty()) {
        LaunchedEffect(path) {
            bitmap = AlbumArtCache.getOrLoad(context, path) { loadAlbumArt(it) }
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
            // High-End Generative Automotive Vinyl Record Art
            val gradientIndex = (path.hashCode().absoluteValue) % AutomotiveGradientBrushes.size
            val brush = AutomotiveGradientBrushes[gradientIndex]

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(brush),
                contentAlignment = Alignment.Center
            ) {
                // Vinyl Groove Rings
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val radius = size.toPx() / 2f
                    drawCircle(
                        color = Color.Black.copy(alpha = 0.25f),
                        radius = radius * 0.85f,
                        style = Stroke(width = 1.2f)
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.15f),
                        radius = radius * 0.65f,
                        style = Stroke(width = 1.2f)
                    )
                    drawCircle(
                        color = Color.Black.copy(alpha = 0.35f),
                        radius = radius * 0.45f,
                        style = Stroke(width = 1.2f)
                    )
                }

                // Center Vinyl Label Core
                Box(
                    modifier = Modifier
                        .size(size * 0.46f)
                        .clip(CircleShape)
                        .background(Color(0xFF0B0E14).copy(alpha = 0.88f))
                        .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.95f),
                        modifier = Modifier
                            .size(size * 0.26f)
                    )
                }
            }
        }
    }
}
