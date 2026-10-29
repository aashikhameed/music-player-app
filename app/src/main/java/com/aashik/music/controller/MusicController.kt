// MusicController.kt
package com.aashik.music.controller

import com.aashik.music.model.Song

object MusicController {
    var currentSong: Song? = null
        private set
    var isPlaying: Boolean = false
        private set

    private val listeners = java.util.concurrent.CopyOnWriteArrayList<() -> Unit>()

    fun addListener(listener: () -> Unit) {
        listeners.add(listener)
    }

    fun removeListener(listener: () -> Unit) {
        listeners.remove(listener)
    }

    var onStateChanged: (() -> Unit)?
        get() = null
        set(value) {
            value?.let { addListener(it) }
        }

    private fun notifyStateChanged() {
        for (listener in listeners) {
            listener.invoke()
        }
    }

    fun play(song: Song? = null) {
        song?.let { currentSong = it }
        isPlaying = true
        notifyStateChanged()
    }

    fun pause() {
        isPlaying = false
        notifyStateChanged()
    }

    fun togglePlayPause() {
        isPlaying = !isPlaying
        notifyStateChanged()
    }

    fun next(songs: List<Song>) {
        val curId = currentSong?.id ?: return
        val index = songs.indexOfFirst { it.id == curId }
        if (index != -1 && index < songs.lastIndex) {
            currentSong = songs[index + 1]
            isPlaying = true
            notifyStateChanged()
        }
    }

    fun previous(songs: List<Song>) {
        val curId = currentSong?.id ?: return
        val index = songs.indexOfFirst { it.id == curId }
        if (index > 0) {
            currentSong = songs[index - 1]
            isPlaying = true
            notifyStateChanged()
        }
    }
}
