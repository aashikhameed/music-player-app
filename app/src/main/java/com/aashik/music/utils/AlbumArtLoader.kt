package com.aashik.music.utils

// utils/AlbumArtLoader.kt
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever

fun loadAlbumArt(path: String): Bitmap? {
    return try {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(path)
        val rawArt = retriever.embeddedPicture
        retriever.release()

        rawArt?.let {
            val bitmap = BitmapFactory.decodeByteArray(it, 0, it.size)
            Bitmap.createScaledBitmap(bitmap, 50, 50, true)
        }
    } catch (_: Exception) {
        null
    }
}
