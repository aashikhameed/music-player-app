package com.aashik.music.wear.player

import android.content.ContentUris
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.MediaStore
import com.aashik.music.wear.model.WearSong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class WearPlayerState(
    val currentSong: WearSong? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val volume: Float = 0.8f,
    val playlist: List<WearSong> = emptyList(),
    val currentIndex: Int = -1,
    val isBluetoothConnected: Boolean = true
) {
    val progress: Float
        get() = if (durationMs > 0) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
}

object WearMusicPlayer {

    private val _state = MutableStateFlow(WearPlayerState())
    val state: StateFlow<WearPlayerState> = _state.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var progressJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    private var activeCategory: com.aashik.music.wear.online.WearRadioRepository.RadioCategory? = null
    private var currentCategoryPage: Int = 1
    private var isReplenishingQueue: Boolean = false

    fun initialize(context: Context) {
        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    }

    fun setPlaylist(
        songs: List<WearSong>,
        startIndex: Int = 0,
        category: com.aashik.music.wear.online.WearRadioRepository.RadioCategory? = null
    ) {
        if (songs.isEmpty()) return
        activeCategory = category
        currentCategoryPage = 1
        isReplenishingQueue = false
        val safeIndex = startIndex.coerceIn(0, songs.size - 1)
        _state.value = _state.value.copy(
            playlist = songs,
            currentIndex = safeIndex,
            currentSong = songs[safeIndex]
        )
        // If the station only has a few tracks or user started near the end, prefetch immediately
        if (safeIndex >= songs.size - 3) {
            checkAndReplenishQueue()
        }
        playSong(songs[safeIndex])
    }

    fun playSongAtIndex(index: Int) {
        val playlist = _state.value.playlist
        if (playlist.isEmpty()) return
        val safeIndex = index.coerceIn(0, playlist.size - 1)
        _state.value = _state.value.copy(
            currentIndex = safeIndex,
            currentSong = playlist[safeIndex]
        )
        if (safeIndex >= playlist.size - 3) {
            checkAndReplenishQueue()
        }
        playSong(playlist[safeIndex])
    }

    fun playSong(song: WearSong) {
        try {
            stopProgressTracking()
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null

            val isNetworkStream = song.path.startsWith("http://", ignoreCase = true) ||
                                  song.path.startsWith("https://", ignoreCase = true)

            val songToPlay = if (isNetworkStream) {
                val cleanTitle = song.title.lowercase().replace(Regex("[^a-z0-9]"), " ").trim()
                val localMatch = _state.value.playlist.firstOrNull { local ->
                    !local.path.startsWith("http://", ignoreCase = true) &&
                    !local.path.startsWith("https://", ignoreCase = true) &&
                    local.title.lowercase().replace(Regex("[^a-z0-9]"), " ").trim() == cleanTitle
                }
                localMatch ?: song
            } else {
                song
            }

            val isLocalPlayable = !songToPlay.path.startsWith("http://", ignoreCase = true) &&
                                  !songToPlay.path.startsWith("https://", ignoreCase = true)

            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                try {
                    setWakeMode(null, PowerManager.PARTIAL_WAKE_LOCK)
                } catch (_: Throwable) {}
                setDataSource(songToPlay.path)
                setVolume(_state.value.volume, _state.value.volume)
                setOnCompletionListener {
                    next()
                }
                setOnErrorListener { _, _, _ ->
                    _state.value = _state.value.copy(isPlaying = false, isBuffering = false)
                    false
                }
            }
            mediaPlayer = player

            if (!isLocalPlayable) {
                _state.value = _state.value.copy(
                    currentSong = songToPlay,
                    isPlaying = false,
                    isBuffering = true,
                    durationMs = songToPlay.durationMs,
                    positionMs = 0L
                )
                player.setOnPreparedListener { mp ->
                    if (mediaPlayer == mp) {
                        val duration = if (songToPlay.durationMs > 0) songToPlay.durationMs else mp.duration.toLong().coerceAtLeast(0L)
                        mp.start()
                        _state.value = _state.value.copy(
                            isPlaying = true,
                            isBuffering = false,
                            durationMs = duration
                        )
                        startProgressTracking()
                    }
                }
                player.prepareAsync()
            } else {
                player.prepare()
                val duration = if (songToPlay.durationMs > 0) songToPlay.durationMs else player.duration.toLong()
                player.start()

                _state.value = _state.value.copy(
                    currentSong = songToPlay,
                    isPlaying = true,
                    isBuffering = false,
                    durationMs = duration,
                    positionMs = 0L
                )
                startProgressTracking()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _state.value = _state.value.copy(isPlaying = false, isBuffering = false)
        }
    }

    fun togglePlayPause() {
        val player = mediaPlayer ?: return
        if (player.isPlaying) {
            player.pause()
            _state.value = _state.value.copy(isPlaying = false)
            stopProgressTracking()
        } else {
            player.start()
            _state.value = _state.value.copy(isPlaying = true)
            startProgressTracking()
        }
    }

    fun play() {
        val player = mediaPlayer ?: return
        if (!player.isPlaying) {
            player.start()
            _state.value = _state.value.copy(isPlaying = true)
            startProgressTracking()
        }
    }

    fun pause() {
        val player = mediaPlayer ?: return
        if (player.isPlaying) {
            player.pause()
            _state.value = _state.value.copy(isPlaying = false)
            stopProgressTracking()
        }
    }

    fun next() {
        val playlist = _state.value.playlist
        if (playlist.isEmpty()) return
        val currentIndex = _state.value.currentIndex
        val isNearEnd = currentIndex >= playlist.size - 3
        val isLastSong = currentIndex >= playlist.size - 1

        if (isNearEnd) {
            checkAndReplenishQueue()
        }

        if (isLastSong) {
            val cat = activeCategory
            if (cat != null) {
                // When on the last song of an online radio station, fetch the next page and play immediately
                scope.launch {
                    val newBatch = fetchMoreCategoryTracks(cat)
                    if (newBatch.isNotEmpty()) {
                        val currentList = _state.value.playlist
                        val distinctNew = newBatch.filter { cand ->
                            currentList.none { existing ->
                                existing.id == cand.id || com.aashik.music.wear.online.WearOnlineRepository.isSameRecording(existing.title, existing.artist, cand.title, cand.artist)
                            }
                        }
                        val updated = if (distinctNew.isNotEmpty()) currentList + distinctNew else currentList + newBatch
                        val nextIndex = currentIndex + 1
                        _state.value = _state.value.copy(
                            playlist = updated,
                            currentIndex = nextIndex,
                            currentSong = updated.getOrNull(nextIndex)
                        )
                        updated.getOrNull(nextIndex)?.let { playSong(it) }
                    } else {
                        // Loop around to first song if no new tracks returned from server
                        val nextIndex = 0
                        _state.value = _state.value.copy(
                            currentIndex = nextIndex,
                            currentSong = playlist.getOrNull(nextIndex)
                        )
                        playSong(playlist[nextIndex])
                    }
                }
                return
            } else {
                // If on the last song, query recommendations/related tracks for continuous endless playback
                val currentSong = _state.value.currentSong
                if (currentSong != null) {
                    scope.launch {
                        val relatedTracks = fetchRelatedRecommendations(currentSong)
                        val currentList = _state.value.playlist
                        val distinctNew = relatedTracks.filter { cand ->
                            currentList.none { existing ->
                                existing.id == cand.id || com.aashik.music.wear.online.WearOnlineRepository.isSameRecording(existing.title, existing.artist, cand.title, cand.artist)
                            }
                        }
                        if (distinctNew.isNotEmpty()) {
                            val updated = currentList + distinctNew
                            val nextIndex = currentIndex + 1
                            _state.value = _state.value.copy(
                                playlist = updated,
                                currentIndex = nextIndex,
                                currentSong = updated.getOrNull(nextIndex)
                            )
                            updated.getOrNull(nextIndex)?.let { playSong(it) }
                            return@launch
                        }
                        val nextIndex = (currentIndex + 1) % playlist.size
                        _state.value = _state.value.copy(currentIndex = nextIndex, currentSong = playlist.getOrNull(nextIndex))
                        playSong(playlist[nextIndex])
                    }
                    return
                }
            }
        }

        val nextIndex = (_state.value.currentIndex + 1) % playlist.size
        _state.value = _state.value.copy(
            currentIndex = nextIndex,
            currentSong = playlist.getOrNull(nextIndex)
        )
        playSong(playlist[nextIndex])
    }

    private fun checkAndReplenishQueue() {
        if (isReplenishingQueue) return
        val cat = activeCategory
        if (cat != null) {
            replenishCategoryQueue(cat)
            return
        }
        val currentSong = _state.value.currentSong ?: return
        isReplenishingQueue = true
        scope.launch {
            try {
                val relatedTracks = fetchRelatedRecommendations(currentSong)
                if (relatedTracks.isNotEmpty()) {
                    val currentList = _state.value.playlist
                    val distinctNew = relatedTracks.filter { cand ->
                        currentList.none { existing ->
                            existing.id == cand.id || com.aashik.music.wear.online.WearOnlineRepository.isSameRecording(existing.title, existing.artist, cand.title, cand.artist)
                        }
                    }
                    if (distinctNew.isNotEmpty()) {
                        _state.value = _state.value.copy(playlist = currentList + distinctNew)
                    }
                }
            } finally {
                isReplenishingQueue = false
            }
        }
    }

    private suspend fun fetchRelatedRecommendations(song: WearSong): List<WearSong> {
        val cleanArtist = song.artist.trim()
        val cleanTitle = song.title.replace(Regex("""\([^)]*\)"""), "").replace(Regex("""\[[^\]]*\]"""), "").trim()
        val queries = mutableListOf<String>()

        if (cleanArtist.isNotBlank() && cleanArtist != "Online Artist" && cleanArtist != "Downloaded") {
            queries.add(cleanArtist)
            queries.add("$cleanArtist songs")
        }
        if (cleanTitle.isNotBlank()) {
            queries.add(cleanTitle)
            if (cleanArtist.isNotBlank() && cleanArtist != "Online Artist" && cleanArtist != "Downloaded") {
                queries.add("$cleanTitle $cleanArtist")
            }
        }
        queries.add("Top hits")

        for (q in queries) {
            val results = com.aashik.music.wear.online.WearOnlineRepository.search(q, limit = 10, page = 1)
            if (results.isNotEmpty()) {
                return results
            }
        }
        return emptyList()
    }

    private fun replenishCategoryQueue(cat: com.aashik.music.wear.online.WearRadioRepository.RadioCategory) {
        if (isReplenishingQueue) return
        isReplenishingQueue = true
        scope.launch {
            try {
                val newBatch = fetchMoreCategoryTracks(cat)
                if (newBatch.isNotEmpty()) {
                    val currentList = _state.value.playlist
                    val distinctNew = newBatch.filter { cand ->
                        currentList.none { existing ->
                            existing.id == cand.id || com.aashik.music.wear.online.WearOnlineRepository.isSameRecording(existing.title, existing.artist, cand.title, cand.artist)
                        }
                    }
                    if (distinctNew.isNotEmpty()) {
                        _state.value = _state.value.copy(playlist = currentList + distinctNew)
                    }
                }
            } finally {
                isReplenishingQueue = false
            }
        }
    }

    private suspend fun fetchMoreCategoryTracks(
        cat: com.aashik.music.wear.online.WearRadioRepository.RadioCategory
    ): List<WearSong> {
        val nextPage = currentCategoryPage + 1
        val tracks = com.aashik.music.wear.online.WearRadioRepository.getCategoryTracks(cat, page = nextPage)
        if (tracks.isNotEmpty()) {
            currentCategoryPage = nextPage
            return tracks
        }
        return emptyList()
    }

    fun previous() {
        val playlist = _state.value.playlist
        if (playlist.isEmpty()) return
        val prevIndex = if (_state.value.currentIndex > 0) _state.value.currentIndex - 1 else playlist.size - 1
        _state.value = _state.value.copy(currentIndex = prevIndex)
        playSong(playlist[prevIndex])
    }

    private var seekDebounceJob: Job? = null

    fun seekBy(deltaMs: Long) {
        val duration = _state.value.durationMs
        if (duration <= 0L) return
        val currentPos = _state.value.positionMs
        val targetPos = (currentPos + deltaMs).coerceIn(0L, duration)
        _state.value = _state.value.copy(positionMs = targetPos)

        seekDebounceJob?.cancel()
        seekDebounceJob = scope.launch {
            delay(120L)
            mediaPlayer?.seekTo(targetPos.toInt())
        }
    }

    fun seekTo(positionMs: Long) {
        val player = mediaPlayer ?: return
        val safePos = positionMs.coerceIn(0L, _state.value.durationMs)
        player.seekTo(safePos.toInt())
        _state.value = _state.value.copy(positionMs = safePos)
    }

    fun setVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        _state.value = _state.value.copy(volume = clamped)
        mediaPlayer?.setVolume(clamped, clamped)
    }

    fun adjustVolume(delta: Float) {
        setVolume(_state.value.volume + delta)
    }

    private fun startProgressTracking() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive && _state.value.isPlaying) {
                mediaPlayer?.let { player ->
                    if (player.isPlaying) {
                        _state.value = _state.value.copy(positionMs = player.currentPosition.toLong())
                    }
                }
                delay(250L) // 4 updates/sec is battery-optimal for Wear OS
            }
        }
    }

    private fun stopProgressTracking() {
        progressJob?.cancel()
        progressJob = null
    }

    fun scanLocalMusic(context: Context): List<WearSong> {
        val songs = mutableListOf<WearSong>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DATA
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

        try {
            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol).toString()
                    val title = cursor.getString(titleCol) ?: "Unknown Track"
                    val artist = cursor.getString(artistCol) ?: "Unknown Artist"
                    val duration = cursor.getLong(durationCol)
                    val album = cursor.getString(albumCol) ?: ""
                    val path = cursor.getString(dataCol) ?: ""

                    if (path.isNotEmpty()) {
                        songs.add(
                            WearSong(
                                id = id,
                                title = title,
                                artist = artist,
                                durationMs = duration,
                                path = path,
                                album = album
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return songs
    }
}
