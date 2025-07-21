
// AudioScanner.kt
package com.aashik.music.repository

import android.content.Context
import android.provider.MediaStore
import com.aashik.music.model.Song

object AudioScanner {
    fun scan(
        context: Context,
        onProgress: (loaded: Int, total: Int) -> Unit = { _, _ -> }
    ): List<Song> {
        val songs = mutableListOf<Song>()
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA
        )

        val cursor = context.contentResolver.query(uri, projection, null, null, null)
        cursor?.use {
            val titleCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val pathCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

            val total = it.count
            var loaded = 0

            while (it.moveToNext()) {
                val path = it.getString(pathCol)
//                val albumArt: ByteArray? = try {
//                    val retriever = MediaMetadataRetriever()
//                    retriever.setDataSource(path)
//                    val raw = retriever.embeddedPicture
//                    val resized = raw?.let { bytes ->
//                        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
//                        val scaled = Bitmap.createScaledBitmap(bitmap, 50, 50, true)
//                        val outputStream = ByteArrayOutputStream()
//                        scaled.compress(Bitmap.CompressFormat.JPEG, 50, outputStream)
//                        outputStream.toByteArray()
//                    }
//                    retriever.release()
//                    resized
//                } catch (_: Exception) {
//                    null
//                }

                songs.add(
                    Song(
                        title = it.getString(titleCol),
                        artist = it.getString(artistCol),
                        album = it.getString(albumCol),
                        duration = it.getLong(durationCol),
                        path = path,
//                        albumArt = albumArt,
                        id = path
                    )
                )

                loaded++
                onProgress(loaded, total)
            }
        }

        return songs
    }

}
