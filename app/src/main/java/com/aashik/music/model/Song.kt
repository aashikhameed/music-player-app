// Song.kt
package com.aashik.music.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class Song(
    @PrimaryKey(autoGenerate = false)
    val id: String, // could be file path or hash
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val path: String,
//    val albumArt: ByteArray? = null
) {
//    override fun equals(other: Any?): Boolean {
//        return other is Song && this.path == other.path
//    }
//
//    override fun hashCode(): Int {
//        return path.hashCode()
//    }
}
