package com.aashik.music.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import android.provider.MediaStore
import com.aashik.music.controller.MusicController
import com.aashik.music.data.MusicDatabase
import com.aashik.music.model.MusicFolder
import com.aashik.music.model.Song
import com.aashik.music.model.NavigatingTrack
import com.aashik.music.pref.ThemePreference
import com.aashik.music.repository.AudioScanner
import com.aashik.music.service.MusicPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
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
    FAVORITES
}

class MusicViewModel(application: Application) : AndroidViewModel(application) {
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val musicPlayer = MusicPlayer.getInstance(application)
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
    private var activePlaylist: List<Song> = emptyList()
    private var activeShuffledSongs: MutableList<Song> = mutableListOf()
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
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    // Grid columns: defaults to 2 for optimal automotive headunit rendering & large touch targets
    private val _gridColumns = MutableStateFlow(2)
    val gridColumns: StateFlow<Int> = _gridColumns.asStateFlow()

    fun toggleGridColumns() {
        _gridColumns.value = if (_gridColumns.value == 2) 3 else 2
    }

    // ── Equalizer / Audio DSP State (Persisted in SharedPreferences) ──
    private val eqPrefs = application.getSharedPreferences("equalizer_settings", Context.MODE_PRIVATE)

    private val _isEqualizerEnabled = MutableStateFlow(eqPrefs.getBoolean("eq_enabled", true))
    val isEqualizerEnabled: StateFlow<Boolean> = _isEqualizerEnabled.asStateFlow()

    private val _equalizerPreset = MutableStateFlow(eqPrefs.getInt("eq_preset", 1)) // Default: Acoustic Clarity
    val equalizerPreset: StateFlow<Int> = _equalizerPreset.asStateFlow()

    private val _equalizerHpfMode = MutableStateFlow(eqPrefs.getInt("eq_hpf_mode", 2)) // Default: 80Hz Cut
    val equalizerHpfMode: StateFlow<Int> = _equalizerHpfMode.asStateFlow()

    private val _equalizerBands = MutableStateFlow(
        listOf(
            eqPrefs.getFloat("eq_band_0", 0.0f),
            eqPrefs.getFloat("eq_band_1", -4.0f), // Mud Cut default
            eqPrefs.getFloat("eq_band_2", 1.0f),
            eqPrefs.getFloat("eq_band_3", 2.0f),
            eqPrefs.getFloat("eq_band_4", 3.0f)
        )
    )
    val equalizerBands: StateFlow<List<Float>> = _equalizerBands.asStateFlow()

    private val _isEqualizerDialogOpen = MutableStateFlow(false)
    val isEqualizerDialogOpen: StateFlow<Boolean> = _isEqualizerDialogOpen.asStateFlow()

    private val _isSettingsOpen = MutableStateFlow(false)
    val isSettingsOpen: StateFlow<Boolean> = _isSettingsOpen.asStateFlow()

    private val _isDriveModeOpen = MutableStateFlow(false)
    val isDriveModeOpen: StateFlow<Boolean> = _isDriveModeOpen.asStateFlow()

    private val _isRefetching = MutableStateFlow(false)
    val isRefetching: StateFlow<Boolean> = _isRefetching.asStateFlow()

    private val _refetchStatus = MutableStateFlow<String?>(null)
    val refetchStatus: StateFlow<String?> = _refetchStatus.asStateFlow()

    private val _artworkCacheSize = MutableStateFlow(0L)
    val artworkCacheSize: StateFlow<Long> = _artworkCacheSize.asStateFlow()

    private val appPrefs = application.getSharedPreferences("automotive_prefs", Context.MODE_PRIVATE)

    // ── Crossfade Duration (Seconds, default 3s) ──
    private val _crossfadeSeconds = MutableStateFlow(appPrefs.getInt("crossfade_seconds", 3))
    val crossfadeSeconds: StateFlow<Int> = _crossfadeSeconds.asStateFlow()

    private val _autoResume = MutableStateFlow(appPrefs.getBoolean("auto_resume", true))
    val autoResume: StateFlow<Boolean> = _autoResume.asStateFlow()

    // ── Favorites State (persisted in SharedPreferences) ──
    private val _favoritePaths = MutableStateFlow<Set<String>>(
        appPrefs.getStringSet("favorite_paths", emptySet())?.toSet() ?: emptySet()
    )
    val favoritePaths: StateFlow<Set<String>> = _favoritePaths.asStateFlow()

    // ── Automotive Time Alignment & Bass Normalizer (Automotive DSP) ──
    private val _timeAlignmentMode = MutableStateFlow(eqPrefs.getInt("time_alignment_mode", 0)) // 0: Center, 1: RHD, 2: LHD
    val timeAlignmentMode: StateFlow<Int> = _timeAlignmentMode.asStateFlow()

    private val _bassNormalizerMode = MutableStateFlow(eqPrefs.getInt("bass_normalizer_mode", 0)) // 0: Off, 1: Mild, 2: Deep
    val bassNormalizerMode: StateFlow<Int> = _bassNormalizerMode.asStateFlow()

    private val _virtualSurroundMode = MutableStateFlow(eqPrefs.getInt("virtual_surround_mode", 0)) // 0: Off, 1: Cabin, 2: Wide
    val virtualSurroundMode: StateFlow<Int> = _virtualSurroundMode.asStateFlow()

    // ── Navigation Transition Direction (1 = Next/Forward, -1 = Prev/Backward) ──
    private var transitionSeq: Long = 0L
    private val _navigationDirection = MutableStateFlow(1)
    val navigationDirection: StateFlow<Int> = _navigationDirection.asStateFlow()

    private val _navigatingTrack = MutableStateFlow(NavigatingTrack(song = null, direction = 1, transitionId = 0L))
    val navigatingTrack: StateFlow<NavigatingTrack> = _navigatingTrack.asStateFlow()

    // ── Sleep Timer / Battery Saver (Minutes) ──
    private val _sleepTimerMinutes = MutableStateFlow(0)
    val sleepTimerMinutes: StateFlow<Int> = _sleepTimerMinutes.asStateFlow()

    private val _sleepTimerRemainingSec = MutableStateFlow(0)
    val sleepTimerRemainingSec: StateFlow<Int> = _sleepTimerRemainingSec.asStateFlow()

    private var sleepTimerJob: Job? = null
    private var usbReceiver: android.content.BroadcastReceiver? = null

    init {
        viewModelScope.launch {
            themePref.isDarkMode.collect {
                _isDarkTheme.value = it
            }
        }

        viewModelScope.launch {
            musicPlayer.isPlayingFlow.collect {
                _isPlaying.value = it
            }
        }
        viewModelScope.launch {
            musicPlayer.currentSongFlow.collect { song ->
                if (song != null) {
                    _currentSong.value = song
                }
            }
        }

        musicPlayer.onCompletion = { isAutoCrossfade ->
            playNextSong(isAutoCrossfade = isAutoCrossfade)
        }

        viewModelScope.launch {
            val saved = songDao.getLastPlayed()
            if (saved != null) {
                _currentSong.value = saved
                _navigatingTrack.value = NavigatingTrack(song = saved, direction = 1, transitionId = ++transitionSeq)
            }
            loadSongs()
        }

        updateArtworkCacheSize()

        // Initialize Native C++ Audio Equalizer DSP
        val initialEnabled = _isEqualizerEnabled.value
        val initialPreset = _equalizerPreset.value
        val initialHpf = _equalizerHpfMode.value
        val initialBands = _equalizerBands.value
        musicPlayer.setEqualizerEnabled(initialEnabled)
        musicPlayer.setEqualizerPreset(initialPreset)
        musicPlayer.setEqualizerHpfMode(initialHpf)
        if (initialPreset == 5) {
            initialBands.forEachIndexed { index, gain ->
                musicPlayer.setEqualizerBandGain(index, gain)
            }
        }

        // Initialize Time Alignment and Bass Normalizer in Native Audio Core
        musicPlayer.setTimeAlignment(_timeAlignmentMode.value)
        musicPlayer.setBassNormalizer(_bassNormalizerMode.value)
        musicPlayer.setVirtualSurroundMode(_virtualSurroundMode.value)

        // Initialize Crossfade
        musicPlayer.setCrossfadeSeconds(_crossfadeSeconds.value)

        // ── Auto-scan on USB Flash Drive insertion ──
        try {
            val receiver = object : android.content.BroadcastReceiver() {
                override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
                    refetchSongs()
                }
            }
            usbReceiver = receiver
            val filter = android.content.IntentFilter().apply {
                addAction(android.content.Intent.ACTION_MEDIA_MOUNTED)
                addAction(android.content.Intent.ACTION_MEDIA_UNMOUNTED)
                addAction(android.content.Intent.ACTION_MEDIA_EJECT)
                addDataScheme("file")
            }
            application.registerReceiver(receiver, filter)
        } catch (_: Exception) {}
    }

    fun setEqualizerEnabled(enabled: Boolean) {
        _isEqualizerEnabled.value = enabled
        musicPlayer.setEqualizerEnabled(enabled)
        eqPrefs.edit().putBoolean("eq_enabled", enabled).apply()
    }

    fun setEqualizerPreset(preset: Int) {
        _equalizerPreset.value = preset
        musicPlayer.setEqualizerPreset(preset)
        when (preset) {
            0 -> { // Flat
                _equalizerHpfMode.value = 0
                _equalizerBands.value = listOf(0f, 0f, 0f, 0f, 0f)
            }
            1 -> { // Acoustic Clarity
                _equalizerHpfMode.value = 2
                _equalizerBands.value = listOf(0f, -4f, 1f, 2f, 3f)
            }
            2 -> { // Vocal Separation
                _equalizerHpfMode.value = 2
                _equalizerBands.value = listOf(-2f, -5f, 2f, 3f, 1f)
            }
            3 -> { // Bass Punch
                _equalizerHpfMode.value = 1
                _equalizerBands.value = listOf(3f, -3f, 0f, 1f, 2f)
            }
            4 -> { // Rock Dynamic
                _equalizerHpfMode.value = 1
                _equalizerBands.value = listOf(2f, -2f, 0f, 2f, 3f)
            }
        }
        eqPrefs.edit()
            .putInt("eq_preset", preset)
            .putInt("eq_hpf_mode", _equalizerHpfMode.value)
            .putFloat("eq_band_0", _equalizerBands.value[0])
            .putFloat("eq_band_1", _equalizerBands.value[1])
            .putFloat("eq_band_2", _equalizerBands.value[2])
            .putFloat("eq_band_3", _equalizerBands.value[3])
            .putFloat("eq_band_4", _equalizerBands.value[4])
            .apply()
    }

    fun setEqualizerBandGain(bandIndex: Int, gainDb: Float) {
        val current = _equalizerBands.value.toMutableList()
        if (bandIndex in current.indices) {
            current[bandIndex] = gainDb
            _equalizerBands.value = current
            _equalizerPreset.value = 5 // Custom
            musicPlayer.setEqualizerBandGain(bandIndex, gainDb)
            eqPrefs.edit()
                .putInt("eq_preset", 5)
                .putFloat("eq_band_$bandIndex", gainDb)
                .apply()
        }
    }

    fun setEqualizerHpfMode(mode: Int) {
        _equalizerHpfMode.value = mode
        musicPlayer.setEqualizerHpfMode(mode)
        eqPrefs.edit().putInt("eq_hpf_mode", mode).apply()
    }

    fun openEqualizerDialog() {
        _isEqualizerDialogOpen.value = true
    }

    fun closeEqualizerDialog() {
        _isEqualizerDialogOpen.value = false
    }

    fun toggleEqualizerDialog() {
        _isEqualizerDialogOpen.value = !_isEqualizerDialogOpen.value
    }

    fun openSettings() {
        _isSettingsOpen.value = true
    }

    fun closeSettings() {
        _isSettingsOpen.value = false
    }

    fun toggleSettings() {
        _isSettingsOpen.value = !_isSettingsOpen.value
    }

    fun openDriveMode() {
        _isDriveModeOpen.value = true
    }

    fun closeDriveMode() {
        _isDriveModeOpen.value = false
    }

    fun toggleDriveMode() {
        _isDriveModeOpen.value = !_isDriveModeOpen.value
    }

    fun toggleFavorite(path: String) {
        val current = _favoritePaths.value.toMutableSet()
        if (current.contains(path)) {
            current.remove(path)
        } else {
            current.add(path)
        }
        _favoritePaths.value = current
        appPrefs.edit().putStringSet("favorite_paths", current).apply()
        if (_selectedTab.value == LibraryTab.FAVORITES) {
            viewModelScope.launch(Dispatchers.Default) { applySearchFilter(_searchQuery.value) }
        }
    }

    fun isFavorite(path: String): Boolean = _favoritePaths.value.contains(path)

    fun setTimeAlignment(mode: Int) {
        _timeAlignmentMode.value = mode
        eqPrefs.edit().putInt("time_alignment_mode", mode).apply()
        musicPlayer.setTimeAlignment(mode)
    }

    fun setBassNormalizer(mode: Int) {
        _bassNormalizerMode.value = mode
        eqPrefs.edit().putInt("bass_normalizer_mode", mode).apply()
        musicPlayer.setBassNormalizer(mode)
    }

    fun setVirtualSurroundMode(mode: Int) {
        _virtualSurroundMode.value = mode
        eqPrefs.edit().putInt("virtual_surround_mode", mode).apply()
        musicPlayer.setVirtualSurroundMode(mode)
    }

    fun skipForward10s() {
        musicPlayer.seekRelative(10_000L)
    }

    fun skipBackward10s() {
        musicPlayer.seekRelative(-10_000L)
    }

    fun setSleepTimer(minutes: Int) {
        _sleepTimerMinutes.value = minutes
        sleepTimerJob?.cancel()
        if (minutes <= 0) {
            _sleepTimerRemainingSec.value = 0
            return
        }
        _sleepTimerRemainingSec.value = minutes * 60
        sleepTimerJob = viewModelScope.launch {
            while (_sleepTimerRemainingSec.value > 0) {
                kotlinx.coroutines.delay(1000)
                _sleepTimerRemainingSec.value -= 1
            }
            musicPlayer.pause()
            _sleepTimerMinutes.value = 0
        }
    }

    fun selectTab(tab: LibraryTab) {
        _selectedTab.value = tab
        when (tab) {
            LibraryTab.ALL_SONGS, LibraryTab.FAVORITES -> {
                _selectedFolder.value = null
                viewModelScope.launch(Dispatchers.Default) { applySearchFilter(_searchQuery.value) }
            }
            LibraryTab.FOLDERS -> {
                updateFolderList()
            }
        }
    }

    fun openFolder(folderPath: String) {
        _selectedFolder.value = folderPath
        viewModelScope.launch(Dispatchers.Default) { applySearchFilter(_searchQuery.value) }
    }

    fun closeFolder() {
        _selectedFolder.value = null
        viewModelScope.launch(Dispatchers.Default) { applySearchFilter(_searchQuery.value) }
    }


    private fun updateFolderList() {
        // Run single-pass count map on background thread with zero intermediate list allocations
        viewModelScope.launch(Dispatchers.Default) {
            val countMap = LinkedHashMap<String, Int>()
            for (song in originalSongs) {
                val p = song.path
                val lastSlash = p.lastIndexOf('/')
                val folderPath = if (lastSlash > 0) p.substring(0, lastSlash) else "Root"
                countMap[folderPath] = (countMap[folderPath] ?: 0) + 1
            }
            val result = countMap.map { (path, count) ->
                MusicFolder(
                    name = path.substringAfterLast('/', path),
                    path = path,
                    songCount = count
                )
            }.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
            _folders.value = result
        }
    }

    private var searchJob: Job? = null

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()
        searchJob = viewModelScope.launch(Dispatchers.Default) {
            if (query.isNotEmpty()) kotlinx.coroutines.delay(100)
            applySearchFilter(query)
        }
    }

    private fun applySearchFilter(query: String) {
        val selectedFolder = _selectedFolder.value
        val tab = _selectedTab.value
        val baseList = when {
            tab == LibraryTab.FAVORITES -> {
                originalSongs.filter { _favoritePaths.value.contains(it.path) }
            }
            selectedFolder != null -> {
                originalSongs.filter {
                    val p = it.path
                    val lastSlash = p.lastIndexOf('/')
                    lastSlash > 0 && p.substring(0, lastSlash) == selectedFolder
                }
            }
            else -> {
                originalSongs
            }
        }

        if (query.isBlank()) {
            _songs.value = baseList
        } else {
            val q = query.trim()
            _songs.value = baseList.filter {
                it.title.contains(q, ignoreCase = true) ||
                it.artist.contains(q, ignoreCase = true) ||
                it.album.contains(q, ignoreCase = true) ||
                it.path.contains(q, ignoreCase = true)
            }
        }
    }

    fun toggleTheme() {
        viewModelScope.launch {
            themePref.setDarkMode(!_isDarkTheme.value)
        }
    }

    fun setThemeMode(isDark: Boolean) {
        viewModelScope.launch {
            themePref.setDarkMode(isDark)
        }
    }

    fun setAutoResume(enabled: Boolean) {
        _autoResume.value = enabled
        appPrefs.edit().putBoolean("auto_resume", enabled).apply()
    }

    fun setCrossfadeSeconds(seconds: Int) {
        _crossfadeSeconds.value = seconds
        appPrefs.edit().putInt("crossfade_seconds", seconds).apply()
        musicPlayer.setCrossfadeSeconds(seconds)
    }

    fun updateArtworkCacheSize() {
        val context = getApplication<Application>().applicationContext
        _artworkCacheSize.value = com.aashik.music.cache.AlbumArtCache.getDiskCacheSizeBytes(context)
    }

    fun clearArtworkCache() {
        viewModelScope.launch {
            val context = getApplication<Application>().applicationContext
            com.aashik.music.cache.AlbumArtCache.clear(context)
            updateArtworkCacheSize()
        }
    }

    fun refetchSongs() {
        viewModelScope.launch {
            _isRefetching.value = true
            _refetchStatus.value = "Deep scanning storage & USB drives..."
            val context = getApplication<Application>().applicationContext
            val scanned = withContext(Dispatchers.IO) {
                val songs = AudioScanner.scan(context) { loaded, total ->
                    _loadedCount.value = loaded
                    _totalCount.value = total
                }
                songDao.clearAll()
                songDao.insertAll(songs)
                songs
            }

            originalSongs = scanned
            activePlaylist = scanned
            shuffledSongs = originalSongs.shuffled().toMutableList()
            activeShuffledSongs = shuffledSongs
            _songs.value = originalSongs
            updateFolderList()
            updateArtworkCacheSize()

            val preloadLimit = minOf(20, originalSongs.size)
            val preloadPaths = ArrayList<String>(preloadLimit)
            for (i in 0 until preloadLimit) {
                preloadPaths.add(originalSongs[i].path)
            }
            com.aashik.music.cache.AlbumArtCache.preload(
                context = context,
                paths = preloadPaths
            ) { path ->
                com.aashik.music.utils.loadAlbumArt(path)
            }

            val activeList = if (_isShuffleOn.value) shuffledSongs else originalSongs
            musicPlayer.setPlaylist(activeList, 0)

            _refetchStatus.value = "Found ${scanned.size} songs in ${_folders.value.size} folders"
            _isRefetching.value = false
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

            originalSongs = loadedSongs
            activePlaylist = loadedSongs

            if (shuffledSongs.isEmpty()) {
                shuffledSongs = originalSongs.shuffled().toMutableList()
            }
            activeShuffledSongs = shuffledSongs

            _songs.value = originalSongs
            updateFolderList()

            // Background prefetch for cover art (first 20 visible items)
            val preloadLimit = minOf(20, originalSongs.size)
            val preloadPaths = ArrayList<String>(preloadLimit)
            for (i in 0 until preloadLimit) {
                preloadPaths.add(originalSongs[i].path)
            }
            com.aashik.music.cache.AlbumArtCache.preload(
                context = context,
                paths = preloadPaths
            ) { path ->
                com.aashik.music.utils.loadAlbumArt(path)
            }

            val activeList = if (_isShuffleOn.value) shuffledSongs else originalSongs
            musicPlayer.setPlaylist(activeList, 0)

            if (_currentSong.value == null) {
                _currentSong.value = shuffledSongs.firstOrNull()
            }

            _isLoading.value = false
        }
    }

    fun play(
        song: Song,
        playlist: List<Song>? = null,
        isAutoCrossfade: Boolean = false,
        direction: Int? = null
    ) {
        val currentId = _currentSong.value?.id
        _currentSong.value = song

        val targetList = playlist ?: when {
            _songs.value.any { it.id == song.id } -> _songs.value
            activePlaylist.any { it.id == song.id } -> activePlaylist
            else -> originalSongs
        }
        val isNewPlaylist = targetList != activePlaylist
        activePlaylist = targetList

        val targetDir = if (direction != null) {
            direction
        } else if (isAutoCrossfade) {
            1
        } else if (currentId != null) {
            val effectiveQueue = if (_isShuffleOn.value && activeShuffledSongs.isNotEmpty()) activeShuffledSongs else targetList
            val oldIdx = effectiveQueue.indexOfFirst { it.id == currentId }
            val newIdx = effectiveQueue.indexOfFirst { it.id == song.id }
            if (oldIdx != -1 && newIdx != -1) {
                if (newIdx >= oldIdx) 1 else -1
            } else 1
        } else 1

        _navigationDirection.value = targetDir
        _navigatingTrack.value = NavigatingTrack(
            song = song,
            direction = targetDir,
            transitionId = ++transitionSeq
        )

        if (_isShuffleOn.value) {
            val isSongInShuffled = activeShuffledSongs.any { it.id == song.id }
            if (isNewPlaylist || activeShuffledSongs.isEmpty() || !isSongInShuffled) {
                val others = activePlaylist.filter { it.id != song.id }.shuffled()
                activeShuffledSongs = (listOf(song) + others).toMutableList()
                shuffledSongs = activeShuffledSongs
            }
        }

        val queue = if (_isShuffleOn.value) activeShuffledSongs else activePlaylist
        val startIndex = queue.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
        musicPlayer.setPlaylist(queue, startIndex)

        musicPlayer.play(song, isAutoCrossfade)
        viewModelScope.launch {
            songDao.saveLastPlayed(song)
        }
        _isPlaying.value = true
    }

    fun playNextSong(isAutoCrossfade: Boolean = false) {
        _navigationDirection.value = 1
        val currentPlaylist = if (activePlaylist.isNotEmpty()) activePlaylist else _songs.value.ifEmpty { originalSongs }
        val list = if (_isShuffleOn.value) {
            if (activeShuffledSongs.isNotEmpty()) activeShuffledSongs else currentPlaylist.shuffled().toMutableList().also { activeShuffledSongs = it }
        } else {
            currentPlaylist
        }
        if (list.isEmpty()) return

        val curId = _currentSong.value?.id
        val index = if (curId != null) list.indexOfFirst { it.id == curId } else -1
        val nextSong = if (index in 0 until list.lastIndex) {
            list[index + 1]
        } else {
            list[0]
        }
        play(nextSong, playlist = currentPlaylist, isAutoCrossfade = isAutoCrossfade, direction = 1)
        MusicController.play(nextSong)
        triggerScrollToCurrentSong()
    }

    fun playPreviousSong() {
        _navigationDirection.value = -1
        val currentPlaylist = if (activePlaylist.isNotEmpty()) activePlaylist else _songs.value.ifEmpty { originalSongs }
        val list = if (_isShuffleOn.value) {
            if (activeShuffledSongs.isNotEmpty()) activeShuffledSongs else currentPlaylist.shuffled().toMutableList().also { activeShuffledSongs = it }
        } else {
            currentPlaylist
        }
        if (list.isEmpty()) return

        val curId = _currentSong.value?.id
        val index = if (curId != null) list.indexOfFirst { it.id == curId } else -1
        val prevSong = if (index > 0) {
            list[index - 1]
        } else {
            list.last()
        }
        play(prevSong, playlist = currentPlaylist, isAutoCrossfade = false, direction = -1)
        MusicController.play(prevSong)
        triggerScrollToCurrentSong()
    }

    fun toggleShuffle() {
        val newState = !_isShuffleOn.value
        _isShuffleOn.value = newState
        val baseList = if (activePlaylist.isNotEmpty()) activePlaylist else _songs.value.ifEmpty { originalSongs }

        if (newState) {
            val current = _currentSong.value
            val others = if (current != null) {
                baseList.filter { it.id != current.id }.shuffled()
            } else {
                baseList.shuffled()
            }
            activeShuffledSongs = if (current != null) {
                (listOf(current) + others).toMutableList()
            } else {
                others.toMutableList()
            }
            shuffledSongs = activeShuffledSongs
        }
        val activeList = if (newState) activeShuffledSongs else baseList
        val startIndex = _currentSong.value?.let { cur -> activeList.indexOfFirst { it.id == cur.id } } ?: 0
        musicPlayer.setPlaylist(activeList, startIndex.coerceAtLeast(0))
    }

    fun getCurrentFolderName(): String {
        val currentFolder = _selectedFolder.value
        if (currentFolder != null) {
            return currentFolder.substringAfterLast('/', currentFolder)
        }
        val songPath = _currentSong.value?.path ?: return "All Songs"
        val lastSlash = songPath.lastIndexOf('/')
        return if (lastSlash > 0) {
            val folderPath = songPath.substring(0, lastSlash)
            folderPath.substringAfterLast('/', folderPath)
        } else {
            "All Songs"
        }
    }

    fun playNextFolder() {
        val folderList = _folders.value
        if (folderList.isEmpty()) return
        val currentSongPath = _currentSong.value?.path
        val currentFolder = _selectedFolder.value ?: currentSongPath?.let { p ->
            val lastSlash = p.lastIndexOf('/')
            if (lastSlash > 0) p.substring(0, lastSlash) else null
        }
        val currentIndex = if (currentFolder != null) {
            folderList.indexOfFirst { it.path == currentFolder }
        } else -1

        val nextIndex = if (currentIndex in 0 until folderList.lastIndex) currentIndex + 1 else 0
        val targetFolder = folderList[nextIndex]
        openFolderAndPlay(targetFolder.path)
    }

    fun playPreviousFolder() {
        val folderList = _folders.value
        if (folderList.isEmpty()) return
        val currentSongPath = _currentSong.value?.path
        val currentFolder = _selectedFolder.value ?: currentSongPath?.let { p ->
            val lastSlash = p.lastIndexOf('/')
            if (lastSlash > 0) p.substring(0, lastSlash) else null
        }
        val currentIndex = if (currentFolder != null) {
            folderList.indexOfFirst { it.path == currentFolder }
        } else -1

        val prevIndex = if (currentIndex > 0) currentIndex - 1 else folderList.lastIndex
        val targetFolder = folderList[prevIndex]
        openFolderAndPlay(targetFolder.path)
    }

    fun openFolderAndPlay(folderPath: String) {
        _selectedTab.value = LibraryTab.FOLDERS
        _selectedFolder.value = folderPath
        val folderSongs = originalSongs.filter {
            val p = it.path
            val lastSlash = p.lastIndexOf('/')
            lastSlash > 0 && p.substring(0, lastSlash) == folderPath
        }
        _songs.value = folderSongs
        if (folderSongs.isNotEmpty()) {
            val firstSong = if (_isShuffleOn.value) folderSongs.shuffled().first() else folderSongs.first()
            play(firstSong, playlist = folderSongs)
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
            val duration = musicPlayer.durationFlow.value.takeIf { it > 0 }
                ?: currentSong.value?.duration?.takeIf { it > 0 }
                ?: return@launch
            val clampedFraction = fraction.coerceIn(0f, 1f)
            val newPosition = (duration * clampedFraction).toLong()
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
                // 1. Delete from Room database
                songDao.delete(song)

                // 2. Identify active playlist and position before removal
                val isCurrentSong = _currentSong.value?.id == song.id
                val activeList = if (_isShuffleOn.value) shuffledSongs else (_songs.value.ifEmpty { originalSongs })
                val deletedIndex = if (isCurrentSong) activeList.indexOfFirst { it.id == song.id } else -1

                // 3. Remove from in-memory playlists
                originalSongs = originalSongs.filter { it.id != song.id }
                activePlaylist = activePlaylist.filter { it.id != song.id }
                shuffledSongs.removeAll { it.id == song.id }
                activeShuffledSongs.removeAll { it.id == song.id }
                com.aashik.music.service.CarPlaybackService.instance?.let { carService ->
                    carService.songList = carService.songList.filter { it.id != song.id }
                }

                // 4. Handle active playback if deleted track was currently playing
                if (isCurrentSong) {
                    val remainingList = if (_isShuffleOn.value) shuffledSongs else (_songs.value.ifEmpty { originalSongs })
                    val nextSong = when {
                        remainingList.isEmpty() -> null
                        deletedIndex in remainingList.indices -> remainingList[deletedIndex] // Advance to track filling this slot
                        deletedIndex >= remainingList.size -> remainingList.last() // Wrap to new last track if deleted was last
                        else -> remainingList.first()
                    }

                    withContext(Dispatchers.Main) {
                        if (nextSong != null) {
                            play(nextSong)
                            MusicController.play(nextSong)
                        } else {
                            musicPlayer.stop()
                            _currentSong.value = null
                            _isPlaying.value = false
                            MusicController.pause()
                        }
                    }
                }

                // 4. Update UI state flows
                withContext(Dispatchers.Main) {
                    applySearchFilter(_searchQuery.value)
                    updateFolderList()
                }

                // 5. Delete from Android MediaStore ContentResolver & FileSystem
                val context = getApplication<Application>().applicationContext
                try {
                    val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                    val where = "${MediaStore.Audio.Media.DATA} = ?"
                    val selectionArgs = arrayOf(song.path)
                    context.contentResolver.delete(uri, where, selectionArgs)
                } catch (_: Exception) {}

                try {
                    val file = File(song.path)
                    if (file.exists()) {
                        file.delete()
                    }
                } catch (_: Exception) {}
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        sleepTimerJob?.cancel()
        try {
            usbReceiver?.let { getApplication<Application>().unregisterReceiver(it) }
            usbReceiver = null
        } catch (_: Exception) {}
    }
}
