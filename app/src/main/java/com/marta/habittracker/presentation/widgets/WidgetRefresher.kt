package com.marta.habittracker.presentation.widgets

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.updateAll
import com.marta.habittracker.presentation.widgets.challenge.ChallengeGlanceWidget
import com.marta.habittracker.presentation.widgets.habit.HabitGlanceWidget
import com.marta.habittracker.presentation.widgets.today.TodayGlanceWidget
import com.marta.habittracker.presentation.widgets.weekly.WeeklyGlanceWidget
import kotlin.coroutines.cancellation.CancellationException

object WidgetRefresher {

    suspend fun refreshAll(context: Context) {
        Log.d(TAG, "Starting widget refresh")

        refreshWidget("HabitGlanceWidget") {
            HabitGlanceWidget().updateAll(context)
        }

        refreshWidget("ChallengeGlanceWidget") {
            ChallengeGlanceWidget().updateAll(context)
        }

        refreshWidget("WeeklyGlanceWidget") {
            WeeklyGlanceWidget().updateAll(context)
        }

        refreshWidget("TodayGlanceWidget") {
            Log.d("AAAAAA", "REFRESHING TODAY WIDGET")
            TodayGlanceWidget().updateAll(context)
        }

        Log.d(TAG, "Widget refresh requests completed")
    }

    private suspend fun refreshWidget(
        name: String,
        action: suspend () -> Unit
    ) {
        try {
            Log.d(TAG, "Updating $name")
            action()
            Log.d(TAG, "$name update request completed")
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to update $name", exception)
        }
    }

    private const val TAG = "WidgetRefresher"
}
