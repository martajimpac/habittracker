package com.marta.habittracker.presentation.widgets

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.updateAll
import com.marta.habittracker.presentation.widgets.challenge.ChallengeGlanceWidget
import com.marta.habittracker.presentation.widgets.habit.HabitGlanceWidget
import com.marta.habittracker.presentation.widgets.weekly.WeeklyGlanceWidget

object WidgetRefresher {

    suspend fun refreshAll(context: Context) {
        try {
            Log.d(TAG, "Refreshing all Glance widgets")
            HabitGlanceWidget().updateAll(context)
            ChallengeGlanceWidget().updateAll(context)
            WeeklyGlanceWidget().updateAll(context)
            Log.d(TAG, "Finished refreshing all Glance widgets")
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to refresh Glance widgets", exception)
        }
    }

    private const val TAG = "WidgetRefresher"
}
