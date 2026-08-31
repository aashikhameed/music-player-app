package com.aashik.music.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aashik.music.controller.MusicController
import com.aashik.music.data.MusicDatabase
import com.aashik.music.model.MusicFolder
import com.aashik.music.model.Song
import com.aashik.music.pref.ThemePreference
import com.aashik.music.repository.AudioScanner
import com.aashik.music.service.MusicPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class LibraryTab {
    ALL_SONGS,
    FOLDERS,
    BLUETOOTH
}

class MusicViewModel(application: Application) : AndroidViewModel(application) {
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val musicPlayer = MusicPlayer(application)
    private val songDao = MusicDatabase.getDatabase(application).songDao()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _loadedCount = MutableStateFlow(0)
    val loadedCount: StateFlow<Int> = _loadedCount

    private val _totalCount = MutableStateFlow(0)
    val totalCount: StateFlow<Int> = _totalCount

    private val _scrollToIndex = MutableStateFlow<Int?>(null)
    val scrollToIndex: StateFlow<Int?> = _scrollToIndex

    private var originalSongs: List<Song> = emptyList()
    private var shuffledSongs: MutableList<Song> = mutableListOf()
    private val _isShuffleOn = MutableStateFlow(true)
    val isShuffleOn: StateFlow<Boolean> = _isShuffleOn

    // Tab and Folder state
    private val _selectedTab = MutableStateFlow(LibraryTab.ALL_SONGS)
    val selectedTab: StateFlow<LibraryTab> = _selectedTab.asStateFlow()

    private val _selectedFolder = MutableStateFlow<String?>(null)
    val selectedFolder: StateFlow<String?> = _selectedFolder.asStateFlow()

    private val _folders = MutableStateFlow<List<MusicFolder>>(emptyList())
    val folders: StateFlow<List<MusicFolder>> = _folders.asStateFlow()

    val positionFlow: StateFlow<Long> = musicPlayer.positionFlow
    val durationFlow: StateFlow<Long> = musicPlayer.durationFlow

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val progressFlow: Flow<Float> = combine(
        musicPlayer.positionFlow,
        musicPlayer.durationFlow
    ) { position: Long, duration: Long ->
        if (duration > 0) position.toFloat() / duration else 0f
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentProgressFlow: Flow<Float> = currentSong
        .flatMapLatest { song ->
            if (song == null) flowOf(0f) else progressFlow
        }

    private val themePref = ThemePreference(application)
    private val _isDarkTheme = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            themePref.isDarkMode.collect {
                _isDarkTheme.value = it
            }
        }

        musicPlayer.onCompletion = {
            playNextSong()
        }

        viewModelScope.launch {
            val saved = songDao.getLastPlayed()
            if (saved != null) {
                _currentSong.value = saved
            }
            loadSongs()
        }
    }

    fun selectTab(tab: LibraryTab) {
        _selectedTab.value = tab
        if (tab == LibraryTab.ALL_SONGS) {
            _selectedFolder.value = null
            applySearchFilter(_searchQuery.value)
        } else {
            updateFolderList()
        }
    }

    fun openFolder(folderPath: String) {
        _selectedFolder.value = folderPath
        applySearchFilter(_searchQuery.value)
    }

    fun closeFolder() {
        _selectedFolder.value = null
        applySearchFilter(_searchQuery.value)
    }

    private fun updateFolderList() {
        val folderMap = originalSongs.groupBy {
            try {
                File(it.path).parent ?: "Root"
            } catch (_: Exception) {
                "Unknown"
            }
        }

        _folders.value = folderMap.map { (path, songList) ->
            val folderName = try {
                File(path).name.ifBlank { path }
            } catch (_: Exception) {
                path
            }
            MusicFolder(
                name = folderName,
                path = path,
                songCount = songList.size
            )
        }.sortedBy { it.name.lowercase() }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        applySearchFilter(query)
    }

    private fun applySearchFilter(query: String) {
        var baseList = if (_selectedFolder.value != null) {
            originalSongs.filter {
                try {
                    File(it.path).parent == _selectedFolder.value
                } catch (_: Exception) {
                    false
                }
            }
        } else {
            originalSongs
        }

        if (query.isBlank()) {
            _songs.value = baseList
        } else {
            val q = query.trim().lowercase()
            _songs.value = baseList.filter {
                it.title.lowercase().contains(q) || it.artist.lowercase().contains(q) || it.album.lowercase().contains(q)
            }
        }
    }

    fun toggleTheme() {
        viewModelScope.launch {
            themePref.setDarkMode(!_isDarkTheme.value)
        }
    }

    fun loadSongs() {
        viewModelScope.launch {
            _isLoading.value = true
            val context = getApplication<Application>().applicationContext
            val loadedSongs = withContext(Dispatchers.IO) {
                songDao.getAllSongs().ifEmpty {
                    val scannedSongs = AudioScanner.scan(context) { loaded, total ->
                        _loadedCount.value = loaded
                        _totalCount.value = total
                    }
                    songDao.insertAll(scannedSongs)
                    scannedSongs
                }
            }

            originalSongs = loadedSongs.sortedBy { it.title.lowercase() }

            if (shuffledSongs.isEmpty()) {
                shuffledSongs = originalSongs.shuffled().toMutableList()
            }

            _songs.value = originalSongs
            updateFolderList()

            // Background prefetch for cover art
            com.aashik.music.cache.AlbumArtCache.preload(
                context = context,
                paths = originalSongs.take(50).map { it.path }
            ) { path ->
                com.aashik.music.utils.loadAlbumArt(path)
            }

            if (_currentSong.value == null) {
                _currentSong.value = shuffledSongs.firstOrNull()
            }

            _isLoading.value = false
        }
    }

    fun play(song: Song) {
        _currentSong.value = song
        musicPlayer.play(song)
        viewModelScope.launch {
            songDao.saveLastPlayed(song)
        }
        _isPlaying.value = true
    }

    fun playNextSong() {
        val list = if (_isShuffleOn.value) shuffledSongs else _songs.value.ifEmpty { originalSongs }
        if (list.isEmpty()) return
        val index = list.indexOf(_currentSong.value)
        val nextSong = if (index in 0 until list.lastIndex) {
            list[index + 1]
        } else {
            list[0]
        }
        play(nextSong)
        MusicController.play(nextSong)
        triggerScrollToCurrentSong()
    }

    fun playPreviousSong() {
        val list = if (_isShuffleOn.value) shuffledSongs else _songs.value.ifEmpty { originalSongs }
        if (list.isEmpty()) return
        val index = list.indexOf(_currentSong.value)
        val prevSong = if (index > 0) {
            list[index - 1]
        } else {
            list.last()
        }
        play(prevSong)
        MusicController.play(prevSong)
        triggerScrollToCurrentSong()
    }

    fun toggleShuffle() {
        val newState = !_isShuffleOn.value
        _isShuffleOn.value = newState
        val baseList = _songs.value.ifEmpty { originalSongs }

        if (newState) {
            val current = _currentSong.value
            val otherSongs = if (current != null) {
                baseList.filter { it.id != current.id }.shuffled()
            } else {
                baseList.shuffled()
            }
            shuffledSongs = if (current != null) {
                (listOf(current) + otherSongs).toMutableList()
            } else {
                otherSongs.toMutableList()
            }
        }
    }

    fun pause() {
        musicPlayer.pause()
        _isPlaying.value = false
        MusicController.pause()
    }

    fun resumeMusic() {
        musicPlayer.resume()
        MusicController.play(currentSong.value)
    }

    fun togglePlayPause() {
        viewModelScope.launch {
            if (_isPlaying.value) {
                pause()
            } else {
                currentSong.value?.let {
                    if (musicPlayer.isPrepared()) {
                        resumeMusic()
                    } else {
                        play(it)
                    }
                }
                _isPlaying.value = true
            }
        }
    }

    fun triggerScrollToCurrentSong() {
        val index = songs.value.indexOfFirst { it.id == currentSong.value?.id }
        if (index >= 0) {
            _scrollToIndex.value = index
        }
    }

    fun triggerScrollToSong(index: Int) {
        _scrollToIndex.value = index
    }

    fun clearScrollToIndex() {
        _scrollToIndex.value = null
    }

    fun seekToFraction(fraction: Float) {
        viewModelScope.launch {
            val duration = musicPlayer.durationFlow.replayCache.firstOrNull() ?: return@launch
            val newPosition = (duration * fraction).toLong()
            musicPlayer.seekTo(newPosition)
        }
    }

    fun syncCurrentPlayingFromPlayer() {
        val currentMediaId = musicPlayer.getCurrentMediaId() ?: return
        val index = songs.value.indexOfFirst { it.path == currentMediaId }
        if (index != -1) {
            _currentSong.value = songs.value[index]
            _scrollToIndex.value = index
        }
    }

    fun deleteSong(song: Song) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                songDao.delete(song)
                originalSongs = originalSongs.filter { it.id != song.id }
                shuffledSongs.removeAll { it.id == song.id }
                applySearchFilter(_searchQuery.value)
                updateFolderList()

                if (_currentSong.value?.id == song.id) {
                    if (originalSongs.isNotEmpty()) {
                        play(originalSongs.first())
                    } else {
                        _currentSong.value = null
                        _isPlaying.value = false
                    }
                }

                val file = File(song.path)
                if (file.exists()) {
                    file.delete()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
