package com.aashik.music.desktop.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import javax.imageio.ImageIO

object DesktopImageCache {
    private val memory = ConcurrentHashMap<String, ImageBitmap>()

    suspend fun load(url: String): ImageBitmap? = withContext(Dispatchers.IO) {
        if (url.isBlank()) return@withContext null
        memory[url]?.let { return@withContext it }
        try {
            val conn = URL(url).openConnection()
            conn.connectTimeout = 4000
            conn.readTimeout = 5000
            val buf = conn.getInputStream().use { ImageIO.read(it) }
            if (buf != null) {
                val bmp = buf.toComposeImageBitmap()
                memory[url] = bmp
                return@withContext bmp
            }
        } catch (_: Exception) {}
        null
    }
}

@Composable
fun DesktopArtwork(
    url: String?,
    modifier: Modifier = Modifier,
    size: Dp = 180.dp,
    shape: RoundedCornerShape = DesktopArtShape
) {
    var bitmap by remember(url) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(url) {
        if (!url.isNullOrBlank()) {
            bitmap = DesktopImageCache.load(url)
        } else {
            bitmap = null
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(DesktopSurfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        val bmp = bitmap
        if (bmp != null) {
            Image(
                bitmap = bmp,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = DesktopPrimary.copy(alpha = 0.6f),
                modifier = Modifier.size(size / 3)
            )
        }
    }
}

