package com.aashik.music.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever

private fun isDirectImageUrl(url: String): Boolean {
    val clean = url.substringBefore('?').lowercase()
    return clean.endsWith(".jpg") || clean.endsWith(".jpeg") || clean.endsWith(".png") || clean.endsWith(".webp")
}

fun loadAlbumArt(path: String, artworkUrl: String? = null): Bitmap? {
    if (path.startsWith("http://") || path.startsWith("https://")) {
        val artUrl = com.aashik.music.cache.AlbumArtCache.getStreamArtwork(path) ?: if (isDirectImageUrl(path)) path else null
        return if (artUrl != null) loadRemoteBitmap(artUrl, targetSize = 64) else null
    }
    var retriever: MediaMetadataRetriever? = null
    val embedded: Bitmap? = try {
        retriever = MediaMetadataRetriever()
        retriever.setDataSource(path)
        val rawArt = retriever.embeddedPicture

        if (rawArt != null) {
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
            val decoded = BitmapFactory.decodeByteArray(rawArt, 0, rawArt.size, decodeOptions)
            if (decoded != null) {
                if (decoded.width <= targetSize && decoded.height <= targetSize) {
                    decoded
                } else {
                    val scaled = Bitmap.createScaledBitmap(decoded, targetSize, targetSize, false)
                    if (scaled != decoded) decoded.recycle()
                    scaled
                }
            } else null
        } else null
    } catch (_: Exception) {
        null
    } finally {
        try { retriever?.release() } catch (_: Exception) {}
    }

    if (embedded != null) return embedded

    val companion = loadCompanionArt(path, targetSize = 64)
    if (companion != null) return companion

    // Fallback: try registered stream artwork URL or explicit artworkUrl for local/downloaded songs
    val fallbackUrl = com.aashik.music.cache.AlbumArtCache.getStreamArtwork(path)
        ?: artworkUrl?.takeIf { it.isNotBlank() && (it.startsWith("http://") || it.startsWith("https://")) }
    return if (fallbackUrl != null) loadRemoteBitmap(fallbackUrl, targetSize = 64) else null
}

private fun upgradeToFullRes(url: String): String {
    return when {
        url.contains("hqdefault.jpg") -> url.replace("hqdefault.jpg", "maxresdefault.jpg")
        url.contains("mqdefault.jpg") -> url.replace("mqdefault.jpg", "maxresdefault.jpg")
        url.contains("default.jpg") && !url.contains("maxresdefault.jpg") -> url.replace("default.jpg", "maxresdefault.jpg")
        url.contains("150x150.jpg") -> url.replace("150x150.jpg", "500x500.jpg")
        url.contains("50x50.jpg") -> url.replace("50x50.jpg", "500x500.jpg")
        url.contains("=w120-h120") -> url.replace("=w120-h120", "=w1000-h1000")
        url.contains("=w226-h226") -> url.replace("=w226-h226", "=w1000-h1000")
        url.contains("=s120") -> url.replace("=s120", "=s1000")
        else -> url
    }
}

fun loadAlbumArtHighRes(path: String, artworkUrl: String? = null, targetSize: Int = 1024): Bitmap? {
    if (path.startsWith("http://") || path.startsWith("https://")) {
        val artUrl = com.aashik.music.cache.AlbumArtCache.getStreamArtwork(path) ?: artworkUrl ?: if (isDirectImageUrl(path)) path else null
        val fullResUrl = artUrl?.let { upgradeToFullRes(it) } ?: artUrl
        val loaded = if (fullResUrl != null) loadRemoteBitmap(fullResUrl, targetSize = targetSize) else null
        if (loaded != null) return loaded
        // Fallback to original url if maxres didn't exist
        return if (artUrl != null && artUrl != fullResUrl) loadRemoteBitmap(artUrl, targetSize = targetSize) else null
    }
    var retriever: MediaMetadataRetriever? = null
    val embedded: Bitmap? = try {
        retriever = MediaMetadataRetriever()
        retriever.setDataSource(path)
        val rawArt = retriever.embeddedPicture

        if (rawArt != null) {
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
            val decoded = BitmapFactory.decodeByteArray(rawArt, 0, rawArt.size, decodeOptions)
            if (decoded != null) {
                if (decoded.width <= targetSize && decoded.height <= targetSize) {
                    decoded
                } else {
                    val scaled = Bitmap.createScaledBitmap(decoded, targetSize, targetSize, false)
                    if (scaled != decoded) decoded.recycle()
                    scaled
                }
            } else null
        } else null
    } catch (_: Exception) {
        null
    } finally {
        try { retriever?.release() } catch (_: Exception) {}
    }

    if (embedded != null) return embedded

    val companion = loadCompanionArt(path, targetSize = targetSize)
    if (companion != null) return companion

    // Fallback: try registered stream artwork URL or explicit artworkUrl for local/downloaded songs
    val fallbackUrl = com.aashik.music.cache.AlbumArtCache.getStreamArtwork(path)
        ?: artworkUrl?.takeIf { it.isNotBlank() && (it.startsWith("http://") || it.startsWith("https://")) }
    return if (fallbackUrl != null) loadRemoteBitmap(fallbackUrl, targetSize = targetSize) else null
}

private fun loadCompanionArt(path: String, targetSize: Int): Bitmap? {
    return try {
        val file = java.io.File(path)
        val parent = file.parentFile ?: return null
        val baseName = file.nameWithoutExtension
        val candidate = java.io.File(parent, "$baseName.jpg").takeIf { it.exists() && it.length() > 0 }
            ?: java.io.File(parent, "$baseName.png").takeIf { it.exists() && it.length() > 0 }
            ?: java.io.File(parent, "cover.jpg").takeIf { it.exists() && it.length() > 0 }
            ?: java.io.File(parent, "folder.jpg").takeIf { it.exists() && it.length() > 0 }

        if (candidate != null) {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(candidate.absolutePath, bounds)
            var sampleSize = 1
            while (bounds.outWidth / sampleSize > targetSize * 2 || bounds.outHeight / sampleSize > targetSize * 2) {
                sampleSize *= 2
            }
            val opts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            BitmapFactory.decodeFile(candidate.absolutePath, opts)
        } else null
    } catch (_: Exception) {
        null
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
