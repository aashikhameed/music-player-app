package com.aashik.music.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aashik.music.pref.ThemePreference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class ThemeViewModel(application: Application) : AndroidViewModel(application) {

    private val pref = ThemePreference(application)

    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> get() = _isDarkMode

    init {
        pref.isDarkMode.onEach {
            _isDarkMode.value = it
        }.launchIn(viewModelScope)
    }

    fun toggleTheme() {
        viewModelScope.launch {
            pref.setDarkMode(!_isDarkMode.value)
        }
    }
}
