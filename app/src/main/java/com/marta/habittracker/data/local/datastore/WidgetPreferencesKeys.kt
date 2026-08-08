package com.marta.habittracker.data.local.datastore

import androidx.datastore.preferences.core.stringPreferencesKey

object WidgetPreferencesKeys {
    fun habitWidgetHabitId(appWidgetId: Int) =
        stringPreferencesKey("habit_widget_habit_id_$appWidgetId")

    fun challengeWidgetChallengeId(appWidgetId: Int) =
        stringPreferencesKey("challenge_widget_challenge_id_$appWidgetId")

    fun challengeWidgetSnapshotJson(appWidgetId: Int) =
        stringPreferencesKey("challenge_widget_snapshot_json_$appWidgetId")
}
