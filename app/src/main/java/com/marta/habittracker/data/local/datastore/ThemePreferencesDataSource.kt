package com.marta.habittracker.data.local.datastore

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.marta.habittracker.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThemePreferencesDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    fun themeMode(): Flow<ThemeMode> = dataStore.data.map { prefs ->
        when (prefs[ThemePreferencesKeys.THEME_MODE]) {
            ThemePreferencesKeys.LIGHT -> ThemeMode.Light
            ThemePreferencesKeys.DARK -> ThemeMode.Dark
            else -> ThemeMode.System
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        try {
            dataStore.edit { prefs ->
                when (mode) {
                    ThemeMode.System -> prefs.remove(ThemePreferencesKeys.THEME_MODE)
                    ThemeMode.Light -> prefs[ThemePreferencesKeys.THEME_MODE] = ThemePreferencesKeys.LIGHT
                    ThemeMode.Dark -> prefs[ThemePreferencesKeys.THEME_MODE] = ThemePreferencesKeys.DARK
                }
            }
            Log.d(TAG, "Theme mode saved: $mode")
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to save theme mode", exception)
            throw exception
        }
    }

    private companion object {
        const val TAG = "ThemePrefs"
    }
}
