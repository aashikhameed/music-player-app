
// RoomDatabase.kt
package com.aashik.music.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.aashik.music.model.Song

@Database(entities = [Song::class], version = 1)
abstract class MusicDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    @Volatile
    private var INSTANCE: MusicDatabase? = null

    companion object {
        @Volatile private var INSTANCE: MusicDatabase? = null

        fun getDatabase(context: Context): MusicDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MusicDatabase::class.java,
                    "music_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }

}