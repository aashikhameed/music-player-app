
// SongDao.kt
package com.aashik.music.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aashik.music.model.Song

@Dao
interface SongDao {
    @Query("SELECT * FROM songs LIMIT 1")
    suspend fun getLastPlayed(): Song?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveLastPlayed(song: Song)

    @Query("SELECT * FROM songs")
    suspend fun getAllSongs(): List<Song>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(song: Song)

    @Delete
    suspend fun delete(song: Song)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(songs: List<Song>)

    @Query("SELECT * FROM songs")
    fun getAllSongsBlocking(): List<Song>  // Not suspend



}
