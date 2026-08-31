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
        val songs = ArrayList<Song>(128)
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA
        )

        // Filter out ringtones, notification sounds, and short audio snippets (< 5 seconds) at SQL engine level
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= 5000"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

        val cursor = context.contentResolver.query(uri, projection, selection, null, sortOrder)
        cursor?.use {
            val titleCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val pathCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

            val total = it.count
            var loaded = 0

            while (it.moveToNext()) {
                val path = it.getString(pathCol) ?: continue
                val title = it.getString(titleCol) ?: "Unknown Title"
                val artist = it.getString(artistCol) ?: "Unknown Artist"
                val album = it.getString(albumCol) ?: "Unknown Album"
                val duration = it.getLong(durationCol)

                songs.add(
                    Song(
                        id = path,
                        title = title,
                        artist = artist,
                        album = album,
                        duration = duration,
                        path = path
                    )
                )

                loaded++
                if (loaded % 20 == 0 || loaded == total) {
                    onProgress(loaded, total)
                }
            }
        }

        return songs
    }
}
