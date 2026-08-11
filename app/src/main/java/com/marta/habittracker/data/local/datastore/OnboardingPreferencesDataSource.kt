package com.marta.habittracker.data.local.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OnboardingPreferencesDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    suspend fun isOnboardingCompleted(): Boolean =
        dataStore.data
            .map { prefs -> prefs[OnboardingPreferencesKeys.ONBOARDING_COMPLETED] == true }
            .first()

    suspend fun setOnboardingCompleted() {
        dataStore.edit { prefs ->
            prefs[OnboardingPreferencesKeys.ONBOARDING_COMPLETED] = true
        }
    }
}
