package com.aashik.music.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aashik.music.controller.MusicController
import com.aashik.music.data.MusicDatabase
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
    private val _isShuffleOn = MutableStateFlow(false)
    val isShuffleOn: StateFlow<Boolean> = _isShuffleOn

    private val progressFlow: Flow<Float> = combine(
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
        viewModelScope.launch {
            val saved = songDao.getLastPlayed()
            if (saved != null) {
                _currentSong.value = saved
            }
            loadSongs()
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
            shuffledSongs = originalSongs.shuffled().toMutableList()

            _songs.value = originalSongs
            _currentSong.value = shuffledSongs.firstOrNull()
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
        val list = if (_isShuffleOn.value) shuffledSongs else originalSongs
        val index = list.indexOf(_currentSong.value)
        if (index in 0 until list.lastIndex) {
            play(list[index + 1])
        } else if (index == list.lastIndex && list.isNotEmpty()) {
            play(list[0])
        }
        MusicController.next(list)

    }

    fun playPreviousSong() {
        val list = if (_isShuffleOn.value) shuffledSongs else originalSongs
        val index = list.indexOf(_currentSong.value)
        if (index > 0) {
            play(list[index - 1])
        } else if (index == 0 && list.isNotEmpty()) {
            play(list.last())
        }
        MusicController.next(list)

    }

    fun toggleShuffle() {
        _isShuffleOn.value = !_isShuffleOn.value
        if (_isShuffleOn.value) {
            shuffledSongs = originalSongs.shuffled().toMutableList()
            _currentSong.value?.let {
                if (!shuffledSongs.contains(it)) {
                    shuffledSongs.add(0, it)
                } else {
                    shuffledSongs.remove(it)
                    shuffledSongs.add(0, it)
                }
            }
        }
        play(shuffledSongs[0])

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
            val newPosition = (duration * fraction).toLong() // convert to Long here
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
}
