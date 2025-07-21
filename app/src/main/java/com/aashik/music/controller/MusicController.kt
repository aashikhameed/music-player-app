// MusicController.kt
package com.aashik.music.controller

import com.aashik.music.model.Song

object MusicController {
    var currentSong: Song? = null
        private set
    var isPlaying: Boolean = false
        private set

    var onStateChanged: (() -> Unit)? = null

    fun play(song: Song? = null) {
        song?.let { currentSong = it }
        isPlaying = true
        onStateChanged?.invoke()
    }

    fun pause() {
        isPlaying = false
        onStateChanged?.invoke()
    }

    fun togglePlayPause() {
        isPlaying = !isPlaying
        onStateChanged?.invoke()
    }

    fun next(songs: List<Song>) {
        val index = songs.indexOf(currentSong)
        if (index != -1 && index < songs.lastIndex) {
            currentSong = songs[index + 1]
            isPlaying = true
            onStateChanged?.invoke()
        }
    }

    fun previous(songs: List<Song>) {
        val index = songs.indexOf(currentSong)
        if (index > 0) {
            currentSong = songs[index - 1]
            isPlaying = true
            onStateChanged?.invoke()
        }
    }
}
