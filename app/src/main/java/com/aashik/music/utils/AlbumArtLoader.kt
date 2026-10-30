package com.aashik.music.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever

private fun isDirectImageUrl(url: String): Boolean {
    val clean = url.substringBefore('?').lowercase()
    return clean.endsWith(".jpg") || clean.endsWith(".jpeg") || clean.endsWith(".png") || clean.endsWith(".webp")
}

fun loadAlbumArt(path: String): Bitmap? {
    if (path.startsWith("http://") || path.startsWith("https://")) {
        val artUrl = com.aashik.music.cache.AlbumArtCache.getStreamArtwork(path) ?: if (isDirectImageUrl(path)) path else null
        return if (artUrl != null) loadRemoteBitmap(artUrl, targetSize = 64) else null
    }
    var retriever: MediaMetadataRetriever? = null
    return try {
        retriever = MediaMetadataRetriever()
        retriever.setDataSource(path)
        val rawArt = retriever.embeddedPicture ?: return null

        // Decode bounds first to prevent decoding full multi-megabyte pictures into heap
        val boundsOptions = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeByteArray(rawArt, 0, rawArt.size, boundsOptions)

        val targetSize = 64 // 64px is crisp for 56dp @ 160dpi headunit and takes only 8KB in RGB_565
        var sampleSize = 1
        while (boundsOptions.outWidth / sampleSize > targetSize * 2 || boundsOptions.outHeight / sampleSize > targetSize * 2) {
            sampleSize *= 2
        }

        // Low memory allocation (RGB_565 config)
        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        val decoded = BitmapFactory.decodeByteArray(rawArt, 0, rawArt.size, decodeOptions) ?: return null

        if (decoded.width <= targetSize && decoded.height <= targetSize) {
            decoded
        } else {
            val scaled = Bitmap.createScaledBitmap(decoded, targetSize, targetSize, false)
            if (scaled != decoded) {
                decoded.recycle()
            }
            scaled
        }
    } catch (_: Exception) {
        null
    } finally {
        try {
            retriever?.release()
        } catch (_: Exception) {}
    }
}

fun loadAlbumArtHighRes(path: String, targetSize: Int = 256): Bitmap? {
    if (path.startsWith("http://") || path.startsWith("https://")) {
        val artUrl = com.aashik.music.cache.AlbumArtCache.getStreamArtwork(path) ?: if (isDirectImageUrl(path)) path else null
        return if (artUrl != null) loadRemoteBitmap(artUrl, targetSize = targetSize) else null
    }
    var retriever: MediaMetadataRetriever? = null
    return try {
        retriever = MediaMetadataRetriever()
        retriever.setDataSource(path)
        val rawArt = retriever.embeddedPicture ?: return null

        val boundsOptions = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeByteArray(rawArt, 0, rawArt.size, boundsOptions)

        var sampleSize = 1
        while (boundsOptions.outWidth / sampleSize > targetSize * 2 || boundsOptions.outHeight / sampleSize > targetSize * 2) {
            sampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        val decoded = BitmapFactory.decodeByteArray(rawArt, 0, rawArt.size, decodeOptions) ?: return null

        if (decoded.width <= targetSize && decoded.height <= targetSize) {
            decoded
        } else {
            val scaled = Bitmap.createScaledBitmap(decoded, targetSize, targetSize, false)
            if (scaled != decoded) {
                decoded.recycle()
            }
            scaled
        }
    } catch (_: Exception) {
        null
    } finally {
        try {
            retriever?.release()
        } catch (_: Exception) {}
    }
}

private fun loadRemoteBitmap(urlStr: String, targetSize: Int): Bitmap? {
    var connection: java.net.HttpURLConnection? = null
    return try {
        val url = java.net.URL(urlStr)
        connection = (url.openConnection() as java.net.HttpURLConnection).apply {
            connectTimeout = 3000
            readTimeout = 4000
            requestMethod = "GET"
            doInput = true
            setRequestProperty("User-Agent", "Mozilla/5.0")
        }
        if (connection.responseCode != java.net.HttpURLConnection.HTTP_OK) return null
        val bytes = connection.inputStream.use { it.readBytes() }
        if (bytes.isEmpty()) return null

        val boundsOptions = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, boundsOptions)

        var sampleSize = 1
        while (boundsOptions.outWidth / sampleSize > targetSize * 2 || boundsOptions.outHeight / sampleSize > targetSize * 2) {
            sampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions) ?: return null

        if (decoded.width <= targetSize && decoded.height <= targetSize) {
            decoded
        } else {
            val scaled = Bitmap.createScaledBitmap(decoded, targetSize, targetSize, false)
            if (scaled != decoded) {
                decoded.recycle()
            }
            scaled
        }
    } catch (_: Throwable) {
        null
    } finally {
        connection?.disconnect()
    }
}
