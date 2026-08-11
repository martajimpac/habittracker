package com.marta.habittracker.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.widgetPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "widget_preferences",
)

data class ChallengeWidgetPrefs(
    val challengeId: String,
    val snapshotJson: String,
)

@Singleton
class WidgetPreferencesDataSource @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val dataStore = context.widgetPreferencesDataStore

    suspend fun setHabitWidgetHabitId(appWidgetId: Int, habitId: String) {
        dataStore.edit { prefs ->
            prefs[WidgetPreferencesKeys.habitWidgetHabitId(appWidgetId)] = habitId
        }
    }

    suspend fun getHabitWidgetHabitId(appWidgetId: Int): String? =
        dataStore.data
            .map { prefs -> prefs[WidgetPreferencesKeys.habitWidgetHabitId(appWidgetId)] }
            .first()

    suspend fun setChallengeWidget(
        appWidgetId: Int,
        challengeId: String,
        snapshotJson: String,
    ) {
        dataStore.edit { prefs ->
            prefs[WidgetPreferencesKeys.challengeWidgetChallengeId(appWidgetId)] = challengeId
            prefs[WidgetPreferencesKeys.challengeWidgetSnapshotJson(appWidgetId)] = snapshotJson
        }
    }

    suspend fun getChallengeWidget(appWidgetId: Int): ChallengeWidgetPrefs? =
        dataStore.data
            .map { prefs ->
                val challengeId = prefs[WidgetPreferencesKeys.challengeWidgetChallengeId(appWidgetId)]
                val snapshotJson = prefs[WidgetPreferencesKeys.challengeWidgetSnapshotJson(appWidgetId)]
                if (challengeId != null && snapshotJson != null) {
                    ChallengeWidgetPrefs(
                        challengeId = challengeId,
                        snapshotJson = snapshotJson,
                    )
                } else {
                    null
                }
            }
            .first()

    suspend fun clearWidget(appWidgetId: Int) {
        dataStore.edit { prefs ->
            prefs.remove(WidgetPreferencesKeys.habitWidgetHabitId(appWidgetId))
            prefs.remove(WidgetPreferencesKeys.challengeWidgetChallengeId(appWidgetId))
            prefs.remove(WidgetPreferencesKeys.challengeWidgetSnapshotJson(appWidgetId))
        }
    }
}
