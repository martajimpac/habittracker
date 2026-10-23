package com.marta.habittracker.data.local.datastore

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.marta.habittracker.domain.model.DailyCheckInSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DailyCheckInPreferencesDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    fun settings(): Flow<DailyCheckInSettings> = dataStore.data.map { prefs ->
        DailyCheckInSettings(
            enabled = prefs[Keys.ENABLED] == true,
            time = prefs[Keys.TIME] ?: DailyCheckInSettings().time,
        )
    }

    suspend fun save(enabled: Boolean, time: String) {
        try {
            dataStore.edit { prefs ->
                prefs[Keys.ENABLED] = enabled
                prefs[Keys.TIME] = time
            }
            Log.d(TAG, "Daily check-in saved enabled=$enabled")
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to save daily check-in", exception)
            throw exception
        }
    }

    private object Keys {
        val ENABLED = booleanPreferencesKey("daily_check_in_enabled")
        val TIME = stringPreferencesKey("daily_check_in_time")
    }

    private companion object {
        const val TAG = "DailyCheckInPrefs"
    }
}
