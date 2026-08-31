package com.aashik.music.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever

fun loadAlbumArt(path: String): Bitmap? {
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

        val targetSize = 128
        var sampleSize = 1
        while (boundsOptions.outWidth / sampleSize > targetSize * 2 || boundsOptions.outHeight / sampleSize > targetSize * 2) {
            sampleSize *= 2
        }

        // Low memory allocation (RGB_565 config)
        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.RGB_565
            inDither = false
        }
        val decoded = BitmapFactory.decodeByteArray(rawArt, 0, rawArt.size, decodeOptions) ?: return null

        if (decoded.width <= targetSize && decoded.height <= targetSize) {
            decoded
        } else {
            val scaled = Bitmap.createScaledBitmap(decoded, targetSize, targetSize, true)
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
