package com.aashik.music.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aashik.music.cache.AlbumArtCache
import com.aashik.music.utils.loadAlbumArt

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
    
    // Instant synchronous check for memory cache hit to prevent blank flicker
    var bitmap by remember(path) { 
        mutableStateOf<Bitmap?>(AlbumArtCache.get(path)) 
    }

    if (bitmap == null) {
        LaunchedEffect(path) {
            bitmap = AlbumArtCache.getOrLoad(context, path) { loadAlbumArt(it) }
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
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
            Icon(
                imageVector = Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(size * 0.25f)
            )
        }
    }
}
