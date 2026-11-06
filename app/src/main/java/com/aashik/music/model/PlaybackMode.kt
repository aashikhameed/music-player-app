package com.aashik.music.model

/**
 * Playback sequencing mode:
 * - SHUFFLE: Plays through queue in randomized order
 * - QUEUE: Plays through queue sequentially in normal order
 * - REPEAT_ONE: Repeats the currently playing song
 */
enum class PlaybackMode {
    SHUFFLE,
    QUEUE,
    REPEAT_ONE;

    fun next(): PlaybackMode = when (this) {
        SHUFFLE -> QUEUE
        QUEUE -> REPEAT_ONE
        REPEAT_ONE -> SHUFFLE
    }
}

