package com.aashik.music

import com.aashik.music.model.MusicFolder
import com.aashik.music.model.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MusicModelsTest {

    @Test
    fun testSongModelCreation() {
        val song = Song(
            id = "/sdcard/Music/Rock/highway_to_drive.mp3",
            title = "Highway to Drive",
            artist = "AC/DC",
            album = "Rock Classics",
            duration = 210000L,
            path = "/sdcard/Music/Rock/highway_to_drive.mp3"
        )

        assertEquals("Highway to Drive", song.title)
        assertEquals("AC/DC", song.artist)
        assertEquals(210000L, song.duration)
    }

    @Test
    fun testFolderGroupingLogic() {
        val songs = listOf(
            Song(id = "1", title = "Track 1", artist = "Artist A", album = "Album A", duration = 180000L, path = "/sdcard/Music/Rock/track1.mp3"),
            Song(id = "2", title = "Track 2", artist = "Artist A", album = "Album A", duration = 190000L, path = "/sdcard/Music/Rock/track2.mp3"),
            Song(id = "3", title = "Track 3", artist = "Artist B", album = "Album B", duration = 200000L, path = "/sdcard/Music/Jazz/track3.mp3"),
            Song(id = "4", title = "Track 4", artist = "Artist C", album = "Album C", duration = 220000L, path = "/sdcard/Music/Electronic/track4.mp3")
        )

        val folderMap = songs.groupBy { File(it.path).parent ?: "Root" }
        val folders = folderMap.map { (path, songList) ->
            MusicFolder(
                name = File(path).name,
                path = path,
                songCount = songList.size
            )
        }

        assertEquals(3, folders.size)
        val rockFolder = folders.find { it.name == "Rock" }
        assertTrue(rockFolder != null)
        assertEquals(2, rockFolder?.songCount)

        val jazzFolder = folders.find { it.name == "Jazz" }
        assertTrue(jazzFolder != null)
        assertEquals(1, jazzFolder?.songCount)
    }

    @Test
    fun testSearchFilteringLogic() {
        val songs = listOf(
            Song(id = "1", title = "Highway Star", artist = "Deep Purple", album = "Machine Head", duration = 360000L, path = "/sdcard/Music/track1.mp3"),
            Song(id = "2", title = "Stairway to Heaven", artist = "Led Zeppelin", album = "Led Zeppelin IV", duration = 480000L, path = "/sdcard/Music/track2.mp3"),
            Song(id = "3", title = "Hotel California", artist = "Eagles", album = "Hotel California", duration = 390000L, path = "/sdcard/Music/track3.mp3")
        )

        val query = "star"
        val filtered = songs.filter {
            it.title.lowercase().contains(query) || it.artist.lowercase().contains(query) || it.album.lowercase().contains(query)
        }

        assertEquals(1, filtered.size)
        assertEquals("Highway Star", filtered.first().title)
    }
}
