// ThemeManager.kt
package com.aashik.music.theme

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore("settings")
object ThemeManager {
    private val THEME_KEY = booleanPreferencesKey("is_dark")

    fun getThemeFlow(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[THEME_KEY] ?: false }

    suspend fun setTheme(context: Context, dark: Boolean) {
        context.dataStore.edit { it[THEME_KEY] = dark }
    }
}
