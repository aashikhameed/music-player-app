package com.aashik.music.viewmodel

import android.Manifest
import android.app.Application
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import android.provider.MediaStore
import com.aashik.music.controller.MusicController
import com.aashik.music.data.MusicDatabase
import com.aashik.music.model.MusicFolder
import com.aashik.music.model.Song
import com.aashik.music.model.NavigatingTrack
import com.aashik.music.model.RadioCategory
import com.aashik.music.model.RadioStation
import com.aashik.music.pref.ThemePreference
import com.aashik.music.repository.AudioScanner
import com.aashik.music.service.MusicPlayer
import com.aashik.music.service.OnlineStreamRepository
import com.aashik.music.service.DownloadStatus
import com.aashik.music.service.StreamDownloadManager
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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class LibraryTab {
    ALL_SONGS,
    FOLDERS,
    FAVORITES,
    RADIO
}

private const val TAG = "MusicViewModel"

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

    // ── Auto HUD Mode & Bottom Bar Visibility ──
    private val _autoHudEnabled = MutableStateFlow(appPrefs.getBoolean("auto_hud_enabled", true))
    val autoHudEnabled: StateFlow<Boolean> = _autoHudEnabled.asStateFlow()

    private val _hideBottomBar = MutableStateFlow(appPrefs.getBoolean("hide_bottom_bar", true))
    val hideBottomBar: StateFlow<Boolean> = _hideBottomBar.asStateFlow()

    // ── YouTube & Video Playback Toggle & State ──
    private val _playVideoForStreams = MutableStateFlow(appPrefs.getBoolean("play_video_for_streams", true))
    val playVideoForStreams: StateFlow<Boolean> = _playVideoForStreams.asStateFlow()

    private val _currentVideoId = MutableStateFlow<String?>(null)
    val currentVideoId: StateFlow<String?> = _currentVideoId.asStateFlow()
    private var videoResolverJob: Job? = null

    // ── Favorites State (persisted in SharedPreferences) ──
    private val _favoritePaths = MutableStateFlow<Set<String>>(
        appPrefs.getStringSet("favorite_paths", emptySet())?.toSet() ?: emptySet()
    )
    val favoritePaths: StateFlow<Set<String>> = _favoritePaths.asStateFlow()

    // ── Radio Stations & Community Streams State ──
    private val _radioStations = MutableStateFlow<List<RadioStation>>(RadioStation.CURATED_STATIONS)
    val radioStations: StateFlow<List<RadioStation>> = _radioStations.asStateFlow()

    private val _radioCategories = MutableStateFlow<List<RadioCategory>>(RadioStation.CATEGORIES)
    val radioCategories: StateFlow<List<RadioCategory>> = _radioCategories.asStateFlow()

    val moodCategories: StateFlow<List<RadioCategory>> = MutableStateFlow(RadioStation.MOOD_CATEGORIES).asStateFlow()
    val genreCategories: StateFlow<List<RadioCategory>> = MutableStateFlow(RadioStation.GENRE_CATEGORIES).asStateFlow()

    private val _selectedRadioCategory = MutableStateFlow<RadioCategory?>(null)
    val selectedRadioCategory: StateFlow<RadioCategory?> = _selectedRadioCategory.asStateFlow()

    private val _categorySongs = MutableStateFlow<List<Song>>(emptyList())
    val categorySongs: StateFlow<List<Song>> = _categorySongs.asStateFlow()

    private val _exploreCategoryDetails = MutableStateFlow<com.aashik.music.service.ExploreCategoryDetails?>(null)
    val exploreCategoryDetails: StateFlow<com.aashik.music.service.ExploreCategoryDetails?> = _exploreCategoryDetails.asStateFlow()

    private val _isLoadingCategorySongs = MutableStateFlow(false)
    val isLoadingCategorySongs: StateFlow<Boolean> = _isLoadingCategorySongs.asStateFlow()

    private var categorySearchJob: Job? = null

    private val _favoriteStationIds = MutableStateFlow<Set<String>>(
        appPrefs.getStringSet("radio_favorite_ids", emptySet())?.toSet() ?: emptySet()
    )
    val favoriteStationIds: StateFlow<Set<String>> = _favoriteStationIds.asStateFlow()

    private val _favoriteCategoryIds = MutableStateFlow<Set<String>>(
        appPrefs.getStringSet("radio_favorite_category_ids", emptySet())?.toSet() ?: emptySet()
    )
    val favoriteCategoryIds: StateFlow<Set<String>> = _favoriteCategoryIds.asStateFlow()

    private val _selectedRadioGenre = MutableStateFlow("All")
    val selectedRadioGenre: StateFlow<String> = _selectedRadioGenre.asStateFlow()

    // ── Unified Global Search State ──
    private val _searchSongs = MutableStateFlow<List<Song>>(emptyList())
    val searchSongs: StateFlow<List<Song>> = _searchSongs.asStateFlow()

    private val _searchStations = MutableStateFlow<List<RadioStation>>(emptyList())
    val searchStations: StateFlow<List<RadioStation>> = _searchStations.asStateFlow()

    private val _onlineSearchSongs = MutableStateFlow<List<Song>>(emptyList())
    val onlineSearchSongs: StateFlow<List<Song>> = _onlineSearchSongs.asStateFlow()

    private val _isSearchingOnline = MutableStateFlow(false)
    val isSearchingOnline: StateFlow<Boolean> = _isSearchingOnline.asStateFlow()

    private val _isRadioLoading = MutableStateFlow(false)
    val isRadioLoading: StateFlow<Boolean> = _isRadioLoading.asStateFlow()

    val isTrackBuffering: StateFlow<Boolean> = musicPlayer.isTrackBufferingFlow

    val isPlayerLoading: StateFlow<Boolean> = combine(
        musicPlayer.isTrackBufferingFlow,
        _isRadioLoading
    ) { isBuffering, isRadio ->
        isBuffering || isRadio
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _activeRadioStationName = MutableStateFlow<String?>(null)
    val activeRadioStationName: StateFlow<String?> = _activeRadioStationName.asStateFlow()

    private var onlineSearchJob: Job? = null
    private var radioJob: Job? = null

    // ── Automotive Time Alignment & Bass Normalizer (Automotive DSP) ──
    private val _timeAlignmentMode = MutableStateFlow(eqPrefs.getInt("time_alignment_mode", 0)) // 0: Center, 1: RHD, 2: LHD
    val timeAlignmentMode: StateFlow<Int> = _timeAlignmentMode.asStateFlow()

    private val _bassNormalizerMode = MutableStateFlow(eqPrefs.getInt("bass_normalizer_mode", 0)) // 0: Off, 1: Mild, 2: Deep
    val bassNormalizerMode: StateFlow<Int> = _bassNormalizerMode.asStateFlow()

    private val _virtualSurroundMode = MutableStateFlow(eqPrefs.getInt("virtual_surround_mode", 0)) // 0: Off, 1: Cabin, 2: Wide
    val virtualSurroundMode: StateFlow<Int> = _virtualSurroundMode.asStateFlow()

    // ── SCVC (Speed-Compensated Volume Control) ──
    private val _scvcMode = MutableStateFlow(appPrefs.getInt("scvc_mode", 0)) // 0: Off, 1: Mild, 2: Moderate, 3: Dynamic
    val scvcMode: StateFlow<Int> = _scvcMode.asStateFlow()

    private val _currentSpeedKmh = MutableStateFlow(0f)
    val currentSpeedKmh: StateFlow<Float> = _currentSpeedKmh.asStateFlow()

    private val _scvcGainDb = MutableStateFlow(0f)
    val scvcGainDb: StateFlow<Float> = _scvcGainDb.asStateFlow()

    private val _testSpeedSimulation = MutableStateFlow(false)
    val testSpeedSimulation: StateFlow<Boolean> = _testSpeedSimulation.asStateFlow()

    private val _testSpeedKmh = MutableStateFlow(60f)
    val testSpeedKmh: StateFlow<Float> = _testSpeedKmh.asStateFlow()

    // ── 4-Way Balance & Fader (Cabin Acoustic Grid) ──
    private val _balance = MutableStateFlow(eqPrefs.getFloat("eq_balance", 0.0f)) // -1.0 (Left) to +1.0 (Right)
    val balance: StateFlow<Float> = _balance.asStateFlow()

    private val _fader = MutableStateFlow(eqPrefs.getFloat("eq_fader", 0.0f)) // -1.0 (Rear) to +1.0 (Front)
    val fader: StateFlow<Float> = _fader.asStateFlow()

    // ── Play Queue & "Play Next" State ──
    private val _userQueue = MutableStateFlow<List<Song>>(emptyList())
    val userQueue: StateFlow<List<Song>> = _userQueue.asStateFlow()

    private val _isQueueOpen = MutableStateFlow(false)
    val isQueueOpen: StateFlow<Boolean> = _isQueueOpen.asStateFlow()

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
    private var locationManager: LocationManager? = null
    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            if (!_testSpeedSimulation.value) {
                val speed = if (location.hasSpeed()) (location.speed * 3.6f) else 0f
                applySpeedUpdate(speed)
            }
        }
        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

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
        viewModelScope.launch {
            _currentSong.collect { song ->
                resolveVideoForSong(song)
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

        // Initialize 4-Way Balance & Fader in Native Audio Core
        musicPlayer.setBalance(_balance.value)
        musicPlayer.setFader(_fader.value)

        // Initialize SCVC
        setupLocationUpdates()
        val initialEffectiveSpeed = if (_testSpeedSimulation.value) _testSpeedKmh.value else _currentSpeedKmh.value
        applySpeedUpdate(initialEffectiveSpeed)

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

        // Initialize Offline Stream Download Engine
        StreamDownloadManager.init(application, songDao)
    }

    fun setupLocationUpdates() {
        val app = getApplication<Application>()
        if (locationManager == null) {
            locationManager = app.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        }
        val lm = locationManager ?: return
        val hasFine = ContextCompat.checkSelfPermission(app, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(app, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!hasFine && !hasCoarse) return

        try {
            lm.removeUpdates(locationListener)
            if (_scvcMode.value > 0) {
                if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 1.0f, locationListener)
                }
                if (lm.isProviderEnabled(LocationManager.PASSIVE_PROVIDER)) {
                    lm.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, 1000L, 1.0f, locationListener)
                }
            }
        } catch (_: Exception) {}
    }

    private fun applySpeedUpdate(speedKmh: Float) {
        val clampedSpeed = speedKmh.coerceAtLeast(0f)
        _currentSpeedKmh.value = clampedSpeed
        musicPlayer.setSpeedCompensation(clampedSpeed, _scvcMode.value)
        _scvcGainDb.value = musicPlayer.getSpeedCompensationDb()
    }

    fun setScvcMode(mode: Int) {
        _scvcMode.value = mode
        appPrefs.edit().putInt("scvc_mode", mode).apply()
        setupLocationUpdates()
        val effectiveSpeed = if (_testSpeedSimulation.value) _testSpeedKmh.value else _currentSpeedKmh.value
        applySpeedUpdate(effectiveSpeed)
    }

    fun setTestSpeedSimulation(enabled: Boolean) {
        _testSpeedSimulation.value = enabled
        val speed = if (enabled) _testSpeedKmh.value else _currentSpeedKmh.value
        applySpeedUpdate(speed)
    }

    fun setTestSpeedKmh(speed: Float) {
        _testSpeedKmh.value = speed
        if (_testSpeedSimulation.value) {
            applySpeedUpdate(speed)
        }
    }

    // ── Balance & Fader ──
    fun setBalance(bal: Float) {
        val clamped = bal.coerceIn(-1.0f, 1.0f)
        _balance.value = clamped
        musicPlayer.setBalance(clamped)
        eqPrefs.edit().putFloat("eq_balance", clamped).apply()
    }

    fun setFader(fad: Float) {
        val clamped = fad.coerceIn(-1.0f, 1.0f)
        _fader.value = clamped
        musicPlayer.setFader(clamped)
        eqPrefs.edit().putFloat("eq_fader", clamped).apply()
    }

    fun resetBalanceFader() {
        setBalance(0.0f)
        setFader(0.0f)
    }

    fun setAcousticFocus(bal: Float, fad: Float) {
        setBalance(bal)
        setFader(fad)
    }

    // ── Play Queue & "Play Next" ──
    fun openQueue() { _isQueueOpen.value = true }
    fun closeQueue() { _isQueueOpen.value = false }
    fun toggleQueue() { _isQueueOpen.value = !_isQueueOpen.value }

    fun playNext(song: Song) {
        val current = _userQueue.value.toMutableList()
        current.removeAll { it.id == song.id }
        current.add(0, song)
        _userQueue.value = current
    }

    fun addToQueue(song: Song) {
        val current = _userQueue.value.toMutableList()
        current.removeAll { it.id == song.id }
        current.add(song)
        _userQueue.value = current
    }

    fun playFromQueue(index: Int) {
        val q = _userQueue.value
        if (index !in q.indices) return
        val selectedSong = q[index]
        val isLast = (index == q.lastIndex)
        // Remove selected item and all items before it
        _userQueue.value = q.drop(index + 1)
        play(selectedSong, direction = 1)
        if (isLast) {
            startRadioForSong(selectedSong)
        }
    }

    fun removeFromQueue(index: Int) {
        val current = _userQueue.value.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            _userQueue.value = current
        }
    }

    fun clearQueue() {
        _userQueue.value = emptyList()
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        val list = _userQueue.value.toMutableList()
        if (fromIndex in list.indices && toIndex in list.indices && fromIndex != toIndex) {
            val item = list.removeAt(fromIndex)
            list.add(toIndex, item)
            _userQueue.value = list
        }
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

    fun cycleEqualizerPreset() {
        // Cycle through presets 0 (Flat), 1 (Acoustic), 2 (Vocal), 3 (Bass Punch), 4 (Rock Dynamic)
        val next = (_equalizerPreset.value + 1) % 5
        setEqualizerPreset(next)
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

    private val _isEqualizerOpen = MutableStateFlow(false)
    val isEqualizerOpen: StateFlow<Boolean> = _isEqualizerOpen.asStateFlow()

    fun openEqualizer() {
        _isEqualizerOpen.value = true
    }

    fun closeEqualizer() {
        _isEqualizerOpen.value = false
    }

    fun toggleEqualizer() {
        _isEqualizerOpen.value = !_isEqualizerOpen.value
    }

    fun getNextSong(): Song? {
        val queued = _userQueue.value.firstOrNull()
        if (queued != null) return queued
        val currentPlaylist = if (activePlaylist.isNotEmpty()) activePlaylist else _songs.value.ifEmpty { originalSongs }
        val list = if (_isShuffleOn.value) {
            if (activeShuffledSongs.isNotEmpty()) activeShuffledSongs else currentPlaylist
        } else {
            currentPlaylist
        }
        if (list.isEmpty()) return null
        val curId = _currentSong.value?.id ?: return list.firstOrNull()
        val index = list.indexOfFirst { it.id == curId }
        return if (index in 0 until list.lastIndex) {
            list[index + 1]
        } else {
            list.firstOrNull()
        }
    }

    fun getUpcomingSongs(limit: Int = 15): List<Song> {
        val userQ = _userQueue.value
        if (userQ.isNotEmpty()) {
            return userQ.take(limit)
        }
        val currentPlaylist = if (activePlaylist.isNotEmpty()) activePlaylist else _songs.value.ifEmpty { originalSongs }
        val list = if (_isShuffleOn.value) {
            if (activeShuffledSongs.isNotEmpty()) activeShuffledSongs else currentPlaylist
        } else {
            currentPlaylist
        }
        if (list.isEmpty()) return emptyList()
        val curId = _currentSong.value?.id ?: return list.take(limit)
        val index = list.indexOfFirst { it.id == curId }
        if (index == -1) return list.take(limit)

        val upcoming = ArrayList<Song>(limit)
        for (i in (index + 1) until list.size) {
            upcoming.add(list[i])
            if (upcoming.size >= limit) break
        }
        if (upcoming.size < limit && list.size > 1) {
            for (i in 0 until index) {
                upcoming.add(list[i])
                if (upcoming.size >= limit) break
            }
        }
        return upcoming
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
            LibraryTab.RADIO -> {
                _selectedFolder.value = null
                viewModelScope.launch(Dispatchers.Default) { applySearchFilter(_searchQuery.value) }
            }
        }
    }

    fun toggleRadioFavorite(stationId: String) {
        val current = _favoriteStationIds.value.toMutableSet()
        if (current.contains(stationId)) {
            current.remove(stationId)
        } else {
            current.add(stationId)
        }
        _favoriteStationIds.value = current
        appPrefs.edit().putStringSet("radio_favorite_ids", current).apply()
    }

    fun isRadioFavorite(stationId: String): Boolean = _favoriteStationIds.value.contains(stationId)

    fun toggleCategoryFavorite(categoryId: String) {
        val current = _favoriteCategoryIds.value.toMutableSet()
        if (current.contains(categoryId)) {
            current.remove(categoryId)
        } else {
            current.add(categoryId)
        }
        _favoriteCategoryIds.value = current
        appPrefs.edit().putStringSet("radio_favorite_category_ids", current).apply()
    }

    fun isCategoryFavorite(categoryId: String): Boolean = _favoriteCategoryIds.value.contains(categoryId)

    fun selectRadioGenre(genre: String) {
        _selectedRadioGenre.value = genre
    }

    fun openRadioCategory(category: RadioCategory) {
        _selectedRadioCategory.value = category
        _categorySongs.value = emptyList()
        _exploreCategoryDetails.value = null
        categorySearchJob?.cancel()
        categorySearchJob = viewModelScope.launch {
            _isLoadingCategorySongs.value = true
            try {
                val details = OnlineStreamRepository.fetchCategoryDetails(category.title, ytParams = category.params)
                _exploreCategoryDetails.value = details
                val finalSongs = if (details.songs.isNotEmpty()) details.songs else OnlineStreamRepository.searchSongs("${category.title} Hits", limit = 15)
                for (s in finalSongs) {
                    OnlineStreamRepository.registerSongGenre(s.id, category.title)
                    OnlineStreamRepository.registerSongGenre(s.path, category.title)
                }
                _categorySongs.value = finalSongs
            } catch (_: Exception) {
                _categorySongs.value = emptyList()
            } finally {
                _isLoadingCategorySongs.value = false
            }
        }
    }

    fun closeRadioCategory() {
        _selectedRadioCategory.value = null
        _categorySongs.value = emptyList()
        _exploreCategoryDetails.value = null
        categorySearchJob?.cancel()
    }

    fun playShelfItem(item: com.aashik.music.service.ExploreShelfItem) {
        categorySearchJob?.cancel()
        categorySearchJob = viewModelScope.launch {
            _isLoadingCategorySongs.value = true
            try {
                // YouTube Music playlist IDs start with VL, RDCLAK, or are MPREb (albums)
                val isYouTubeId = item.id.startsWith("VL") || item.id.startsWith("RDCLAK") || item.id.startsWith("MPREb") || item.id.startsWith("OLAK")
                val songs: List<com.aashik.music.model.Song> = if (isYouTubeId) {
                    // Resolve via YouTube Music browse + JioSaavn stream resolution
                    val ytTracks = com.aashik.music.service.YouTubeVideoResolver.fetchPlaylistOrAlbumTracks(item.id)
                    val genre = _selectedRadioCategory.value?.title
                    val resolvedSongs = ArrayList<com.aashik.music.model.Song>(ytTracks.size)
                    for (track in ytTracks) {
                        val artUrl = track.thumbnailUrl ?: ""
                        val song = com.aashik.music.model.Song(
                            id = "online_yt_${track.videoId}",
                            title = track.title,
                            artist = track.artist,
                            album = track.album,
                            path = "https://www.youtube.com/watch?v=${track.videoId}",
                            duration = track.durationMs,
                            artworkUrl = artUrl
                        )
                        if (artUrl.isNotBlank()) {
                            com.aashik.music.cache.AlbumArtCache.registerStreamArtwork(song.path, artUrl)
                        }
                        if (genre != null) {
                            OnlineStreamRepository.registerSongGenre(song.id, genre)
                            OnlineStreamRepository.registerSongGenre(song.path, genre)
                        }
                        resolvedSongs.add(song)
                    }
                    resolvedSongs
                } else if (item.type == "playlist") {
                    OnlineStreamRepository.fetchPlaylistSongs(item.id)
                } else {
                    OnlineStreamRepository.fetchAlbumSongs(item.id)
                }
                if (songs.isNotEmpty()) {
                    val genre = _selectedRadioCategory.value?.title
                    for (s in songs) {
                        genre?.let {
                            OnlineStreamRepository.registerSongGenre(s.id, it)
                            OnlineStreamRepository.registerSongGenre(s.path, it)
                        }
                    }
                    _categorySongs.value = songs
                    play(songs.first(), playlist = songs)
                }
            } catch (e: Exception) {
                Log.w("MusicViewModel", "Failed to play shelf item ${item.title}: ${e.message}")
            } finally {
                _isLoadingCategorySongs.value = false
            }
        }
    }

    fun playRadioStation(station: RadioStation) {
        val stationSong = station.toSong()
        val stationSongs = _radioStations.value.map { it.toSong() }
        OnlineStreamRepository.registerSongGenre(stationSong.id, station.genre)
        OnlineStreamRepository.registerSongGenre(stationSong.path, station.genre)
        _activeRadioStationName.value = station.name
        _userQueue.value = stationSongs.filter { it.id != stationSong.id }
        play(stationSong, playlist = stationSongs)
    }

    fun playRandomOrNextRadioStation() {
        val stations = _radioStations.value
        if (stations.isEmpty()) return
        val favIds = _favoriteStationIds.value
        val curPath = _currentSong.value?.path
        val favStations = stations.filter { favIds.contains(it.id) }
        val pool = if (favStations.isNotEmpty()) favStations else stations
        val nextStation = if (curPath != null) {
            val idx = pool.indexOfFirst { it.streamUrl == curPath }
            if (idx in 0 until pool.lastIndex) pool[idx + 1] else pool.first()
        } else {
            pool.first()
        }
        playRadioStation(nextStation)
    }

    fun searchOnline(query: String) {
        onlineSearchJob?.cancel()
        val q = query.trim()
        if (q.length < 2) {
            _onlineSearchSongs.value = emptyList()
            _isSearchingOnline.value = false
            return
        }
        onlineSearchJob = viewModelScope.launch {
            _isSearchingOnline.value = true
            try {
                kotlinx.coroutines.delay(350L) // Debounce typing on headunit
                val results = OnlineStreamRepository.searchSongs(q, limit = 15)
                _onlineSearchSongs.value = results
            } catch (e: Exception) {
                _onlineSearchSongs.value = emptyList()
            } finally {
                _isSearchingOnline.value = false
            }
        }
    }

    fun startRadioForSong(seedSong: Song) {
        radioJob?.cancel()
        radioJob = viewModelScope.launch {
            _isRadioLoading.value = true
            OnlineStreamRepository.resetActiveStation()
            val genre = _selectedRadioCategory.value?.title
                ?: OnlineStreamRepository.getSongGenre(seedSong)
            _activeRadioStationName.value = if (genre != null) "Radio: $genre • ${seedSong.title}" else "Radio: ${seedSong.title}"
            try {
                // Clear any manual user queue so the radio station replaces the current queue
                _userQueue.value = emptyList()

                // If the seed song isn't already playing or is paused, start playing it
                if (_currentSong.value?.id != seedSong.id || !_isPlaying.value) {
                    play(seedSong, playlist = listOf(seedSong))
                }

                // Fetch authentic algorithmic radio tracks for this seed song in the EXACT SAME GENRE
                val radioTracks = OnlineStreamRepository.getRadioTracksForSong(seedSong, targetGenre = genre, limit = 15)
                val resolvedTracks = if (radioTracks.isNotEmpty()) {
                    radioTracks
                } else if (genre != null && genre.isNotBlank()) {
                    OnlineStreamRepository.searchSongs("$genre Hits", limit = 15).filter { it.id != seedSong.id }
                } else {
                    emptyList()
                }

                val fullQueue = ArrayList<Song>(1 + resolvedTracks.size)
                fullQueue.add(seedSong)
                for (t in resolvedTracks) {
                    if (t.id != seedSong.id) fullQueue.add(t)
                }

                // Completely replace the active playlist & queue with the radio tracks
                activePlaylist = fullQueue
                _songs.value = fullQueue

                // Update categorySongs so the UI list on the screen refreshes immediately!
                _categorySongs.value = fullQueue

                // Populate userQueue with upcoming radio tracks so they appear in Queue drawer and Next Song
                val upcomingTracks = fullQueue.filter { it.id != seedSong.id }
                _userQueue.value = upcomingTracks

                if (_isShuffleOn.value) {
                    val others = fullQueue.filter { it.id != seedSong.id }.shuffled()
                    activeShuffledSongs = (listOf(seedSong) + others).toMutableList()
                    shuffledSongs = activeShuffledSongs
                } else {
                    activeShuffledSongs = mutableListOf()
                }

                val queue = if (_isShuffleOn.value) activeShuffledSongs else activePlaylist
                val startIndex = queue.indexOfFirst { it.id == seedSong.id }.coerceAtLeast(0)
                musicPlayer.setPlaylist(queue, startIndex)

            } catch (e: Exception) {
                Log.w("MusicViewModel", "startRadioForSong error: ${e.message}")
            } finally {
                _isRadioLoading.value = false
            }
        }
    }

    private var isRefillingQueue = false

    fun refillStreamingQueue(seedSong: Song) {
        if (isRefillingQueue) return
        isRefillingQueue = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val genre = _selectedRadioCategory.value?.title
                    ?: OnlineStreamRepository.getSongGenre(seedSong)
                var moreTracks = OnlineStreamRepository.fetchNextRadioBatch(targetGenre = genre, limit = 15)
                if (moreTracks.isEmpty()) {
                    moreTracks = OnlineStreamRepository.getRadioTracksForSong(seedSong, targetGenre = genre, limit = 15)
                }
                val existingIds = activePlaylist.map { it.id }.toSet()
                val newTracks = moreTracks.filter { !existingIds.contains(it.id) }
                if (newTracks.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        val updatedPlaylist = activePlaylist + newTracks
                        activePlaylist = updatedPlaylist
                        _songs.value = updatedPlaylist
                        _userQueue.value = _userQueue.value + newTracks
                        if (_selectedRadioCategory.value != null && _categorySongs.value.isNotEmpty()) {
                            _categorySongs.value = _categorySongs.value + newTracks
                        }
                        val curId = _currentSong.value?.id
                        val startIdx = if (curId != null) updatedPlaylist.indexOfFirst { it.id == curId }.coerceAtLeast(0) else 0
                        musicPlayer.setPlaylist(updatedPlaylist, startIdx)
                    }
                }
            } catch (e: Exception) {
                Log.w("MusicViewModel", "refillStreamingQueue failed: ${e.message}")
            } finally {
                isRefillingQueue = false
            }
        }
    }

    fun startRadioForCurrentSong() {
        val current = _currentSong.value
        if (current != null) {
            startRadioForSong(current)
        } else {
            playRandomOrNextRadioStation()
        }
    }

    // ── Stream Download Management ──
    val downloadStates: StateFlow<Map<String, DownloadStatus>> = StreamDownloadManager.downloadStates
    val downloadStoragePreset: StateFlow<String> = StreamDownloadManager.downloadStoragePreset
    val downloadLocationPath: StateFlow<String> = StreamDownloadManager.downloadLocationPath

    fun setDownloadStorage(preset: String, customPath: String? = null): Boolean {
        val app = getApplication<Application>()
        return StreamDownloadManager.setStoragePreset(app, preset, customPath)
    }

    fun testCustomPathWritable(path: String): Pair<Boolean, String> {
        return StreamDownloadManager.testDirectoryWritable(path)
    }

    fun downloadSong(song: Song) {
        val app = getApplication<Application>()
        StreamDownloadManager.downloadSong(app, song, songDao) { downloadedSong ->
            viewModelScope.launch(Dispatchers.Main) {
                if (originalSongs.none { it.id == downloadedSong.id }) {
                    originalSongs = (originalSongs + downloadedSong).sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })
                    _songs.value = originalSongs
                    updateFolderList()
                }
            }
        }
    }

    fun isSongDownloaded(songId: String): Boolean = StreamDownloadManager.isDownloaded(songId)

    fun getDownloadStatus(song: Song): DownloadStatus = StreamDownloadManager.getStatus(song.id)

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
                // Online streams must never be parsed by URL slashes into individual fake folders
                if (song.isStream() || song.path.startsWith("http://", ignoreCase = true) || song.path.startsWith("https://", ignoreCase = true)) {
                    continue
                }
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
            _searchSongs.value = emptyList()
            _searchStations.value = emptyList()
            _onlineSearchSongs.value = emptyList()
            _isSearchingOnline.value = false
            _searchSongs.value = emptyList()
            _searchStations.value = emptyList()
            _onlineSearchSongs.value = emptyList()
            _isSearchingOnline.value = false
        } else {
            val q = query.trim()
            val matchedSongs = originalSongs.filter {
                it.title.contains(q, ignoreCase = true) ||
                it.artist.contains(q, ignoreCase = true) ||
                it.album.contains(q, ignoreCase = true) ||
                it.path.contains(q, ignoreCase = true)
            }
            val matchedStations = _radioStations.value.filter {
                it.name.contains(q, ignoreCase = true) ||
                it.genre.contains(q, ignoreCase = true) ||
                it.description.contains(q, ignoreCase = true) ||
                it.country.contains(q, ignoreCase = true)
            }
            _searchSongs.value = matchedSongs
            _searchStations.value = matchedStations
            _songs.value = baseList.filter {
                it.title.contains(q, ignoreCase = true) ||
                it.artist.contains(q, ignoreCase = true) ||
                it.album.contains(q, ignoreCase = true) ||
                it.path.contains(q, ignoreCase = true)
            }
            searchOnline(q)
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

    fun setAutoHudEnabled(enabled: Boolean) {
        _autoHudEnabled.value = enabled
        appPrefs.edit().putBoolean("auto_hud_enabled", enabled).apply()
    }

    fun setHideBottomBar(hide: Boolean) {
        _hideBottomBar.value = hide
        appPrefs.edit().putBoolean("hide_bottom_bar", hide).apply()
    }

    fun toggleHideBottomBar() {
        setHideBottomBar(!_hideBottomBar.value)
    }

    private fun resolveVideoForSong(song: Song?) {
        videoResolverJob?.cancel()
        if (song == null) {
            _currentVideoId.value = null
            return
        }
        val directId = song.getYouTubeVideoId()
        if (!directId.isNullOrBlank()) {
            _currentVideoId.value = directId
            return
        }
        if (song.isLocalVideo()) {
            _currentVideoId.value = null
            return
        }
        videoResolverJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d(TAG, "Resolving video for ${song.title} - ${song.artist}")
                val resolved = com.aashik.music.service.YouTubeVideoResolver.resolveVideoId(song.title, song.artist)
                Log.d(TAG, "Resolved video ID for ${song.title}: $resolved")
                _currentVideoId.value = resolved
            } catch (e: Exception) {
                Log.w(TAG, "Video resolution failed: ${e.message}")
            }
        }
    }

    fun setPlayVideoForStreams(enabled: Boolean) {
        _playVideoForStreams.value = enabled
        appPrefs.edit().putBoolean("play_video_for_streams", enabled).apply()
        if (enabled && _currentVideoId.value.isNullOrBlank()) {
            resolveVideoForSong(_currentSong.value)
        }
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

        // Synchronize and advance user queue based on played song
        val qIndex = _userQueue.value.indexOfFirst { it.id == song.id }
        if (qIndex != -1) {
            val isLast = (qIndex == _userQueue.value.lastIndex)
            _userQueue.value = _userQueue.value.drop(qIndex + 1)
            if (isLast) {
                startRadioForSong(song)
            }
        } else {
            val curIdx = targetList.indexOfFirst { it.id == song.id }
            if (curIdx != -1 && curIdx < targetList.lastIndex) {
                _userQueue.value = targetList.subList(curIdx + 1, targetList.size)
            } else {
                _userQueue.value = emptyList()
                if (song.isStream()) {
                    startRadioForSong(song)
                }
            }
        }

        if (song.isStream()) {
            _isShuffleOn.value = false
        }

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

        if (song.isStream() && !song.isDirectPlayable()) {
            _isRadioLoading.value = true
            viewModelScope.launch(Dispatchers.IO) {
                val resolved = OnlineStreamRepository.resolvePlayableSong(song)
                withContext(Dispatchers.Main) {
                    _isRadioLoading.value = false
                    if (_currentSong.value?.id == song.id) {
                        _currentSong.value = resolved
                        activePlaylist = activePlaylist.map { if (it.id == song.id) resolved else it }
                        _songs.value = _songs.value.map { if (it.id == song.id) resolved else it }
                        musicPlayer.play(resolved, isAutoCrossfade)
                    }
                }
            }
        } else {
            musicPlayer.play(song, isAutoCrossfade)
        }
        viewModelScope.launch {
            songDao.saveLastPlayed(song)
        }
        _isPlaying.value = true
    }

    fun playNextSong(isAutoCrossfade: Boolean = false) {
        if (_userQueue.value.isNotEmpty()) {
            val queued = _userQueue.value.first()
            val isLast = (_userQueue.value.size == 1)
            _userQueue.value = _userQueue.value.drop(1)
            play(queued, playlist = if (activePlaylist.isNotEmpty()) activePlaylist else _songs.value, isAutoCrossfade = isAutoCrossfade, direction = 1)
            MusicController.play(queued)
            triggerScrollToCurrentSong()

            if (isLast) {
                startRadioForSong(queued)
            } else if (queued.isStream() && _userQueue.value.size <= 2) {
                refillStreamingQueue(queued)
            }
            return
        }
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

        val isCurrentStream = _currentSong.value?.isStream() == true
        if (isCurrentStream) {
            if (index in 0 until list.lastIndex) {
                // There are more tracks — advance normally
                val nextSong = list[index + 1]
                play(nextSong, playlist = currentPlaylist, isAutoCrossfade = isAutoCrossfade, direction = 1)
                MusicController.play(nextSong)
                triggerScrollToCurrentSong()
                // Proactively refill when approaching end of playlist (remaining tracks <= 4)
                val remainingTracks = list.lastIndex - (index + 1)
                if (remainingTracks <= 4) {
                    val lastSeed = list.lastOrNull() ?: nextSong
                    refillStreamingQueue(lastSeed)
                }
            } else {
                // End of stream playlist — fetch new radio tracks and immediately play the next one!
                val lastSeed = _currentSong.value ?: list.lastOrNull() ?: return
                viewModelScope.launch(Dispatchers.IO) {
                    val genre = _selectedRadioCategory.value?.title ?: OnlineStreamRepository.getSongGenre(lastSeed)
                    var moreTracks = OnlineStreamRepository.fetchNextRadioBatch(targetGenre = genre, limit = 15)
                    if (moreTracks.isEmpty()) {
                        moreTracks = OnlineStreamRepository.getRadioTracksForSong(lastSeed, targetGenre = genre, limit = 15)
                    }
                    val existingIds = activePlaylist.map { it.id }.toSet()
                    val newTracks = moreTracks.filter { !existingIds.contains(it.id) }
                    if (newTracks.isNotEmpty()) {
                        withContext(Dispatchers.Main) {
                            val updatedPlaylist = activePlaylist + newTracks
                            activePlaylist = updatedPlaylist
                            _songs.value = updatedPlaylist
                            _userQueue.value = newTracks.drop(1)
                            play(newTracks.first(), playlist = updatedPlaylist, isAutoCrossfade = isAutoCrossfade, direction = 1)
                            MusicController.play(newTracks.first())
                            triggerScrollToCurrentSong()
                        }
                    }
                }
            }
        } else {
            val nextSong = if (index in 0 until list.lastIndex) {
                list[index + 1]
            } else {
                list[0]
            }
            play(nextSong, playlist = currentPlaylist, isAutoCrossfade = isAutoCrossfade, direction = 1)
            MusicController.play(nextSong)
            triggerScrollToCurrentSong()
        }
    }

    fun playPreviousSong() {
        val curPos = positionFlow.value
        // If track has been playing for more than 3 seconds, standard automotive behavior is to restart track
        if (curPos > 3000L) {
            musicPlayer.seekTo(0L)
            return
        }

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
        // Online streams & live radios preserve uninterrupted curated progression - shuffle is disabled
        if (_currentSong.value?.isStream() == true) {
            _isShuffleOn.value = false
            return
        }
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
                _userQueue.value = _userQueue.value.filter { it.id != song.id }
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

                // 5. Hard-delete from Physical Storage (Filesystem & MediaStore)
                val context = getApplication<Application>().applicationContext
                val targetFile = File(song.path)

                // Step A: Direct Physical Filesystem Hard-Deletion
                try {
                    if (targetFile.exists()) {
                        val deleted = targetFile.delete()
                        if (!deleted) {
                            targetFile.setWritable(true)
                            if (!targetFile.delete()) {
                                try {
                                    java.io.FileOutputStream(targetFile).use { /* zero out file */ }
                                    targetFile.delete()
                                } catch (_: Exception) {}
                            }
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.w("MusicViewModel", "Filesystem hard delete failed: ${e.message}")
                }

                // Step B: Delete from Android MediaStore using precise ContentUris with item ID
                try {
                    val projection = arrayOf(MediaStore.Audio.Media._ID)
                    val cursor = context.contentResolver.query(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        projection,
                        "${MediaStore.Audio.Media.DATA} = ?",
                        arrayOf(song.path),
                        null
                    )
                    cursor?.use {
                        if (it.moveToFirst()) {
                            val id = it.getLong(it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID))
                            val itemUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                            context.contentResolver.delete(itemUri, null, null)
                        }
                    }
                    // Secondary query-based cleanup
                    context.contentResolver.delete(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        "${MediaStore.Audio.Media.DATA} = ?",
                        arrayOf(song.path)
                    )
                } catch (e: Exception) {
                    android.util.Log.w("MusicViewModel", "MediaStore deletion failed: ${e.message}")
                }

                // Step C: Trigger MediaScanner to prune stale OS index records so rescans don't resurrect the track
                try {
                    MediaScannerConnection.scanFile(context, arrayOf(song.path), null, null)
                } catch (_: Exception) {}

                // Step D: StreamDownloadManager cleanup (if this was a downloaded track)
                try {
                    StreamDownloadManager.onSongDeleted(context, song)
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
        try {
            locationManager?.removeUpdates(locationListener)
            locationManager = null
        } catch (_: Exception) {}
    }
}
