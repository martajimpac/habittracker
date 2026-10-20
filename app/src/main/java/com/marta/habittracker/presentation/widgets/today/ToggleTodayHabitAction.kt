package com.marta.habittracker.presentation.widgets.today

import android.content.Context
import android.util.Log
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.marta.habittracker.presentation.widgets.WidgetEntryPoint
import com.marta.habittracker.presentation.widgets.WidgetRefresher
import com.marta.habittracker.presentation.widgets.habit.HabitWidgetToggleOutcome
import com.marta.habittracker.presentation.widgets.habit.performHabitWidgetToggle
import dagger.hilt.android.EntryPointAccessors
import java.time.LocalDate
import kotlinx.coroutines.flow.first

class ToggleTodayHabitAction : ActionCallback {

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val habitId = parameters[habitIdKey]
        if (habitId.isNullOrBlank()) {
            Log.w(TAG, "Toggle ignored because the habit id was missing")
            return
        }
        val entryPoint = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
        val repository = entryPoint.habitRepository()
        val today = LocalDate.now()

        try {
            val habits = repository.getAllHabitsWithRecords().first()
            when (
                performHabitWidgetToggle(
                    configuredHabitId = habitId,
                    habits = habits,
                    today = today,
                    toggle = repository::toggleHabitCompletion,
                )
            ) {
                HabitWidgetToggleOutcome.IgnoredMissingHabit ->
                    Log.w(TAG, "Toggle ignored because habit $habitId was not found")

                HabitWidgetToggleOutcome.ToggledAndShouldRefresh ->
                    WidgetRefresher.refreshAll(context)

                HabitWidgetToggleOutcome.ToggleFailed ->
                    Log.e(TAG, "Toggle failed for habit $habitId")
            }
        } catch (exception: Exception) {
            Log.e(TAG, "Toggle failed for habit $habitId", exception)
        }
    }

    companion object {
        private const val TAG = "TodayWidgetToggle"
        val habitIdKey = ActionParameters.Key<String>("today_widget_habit_id")
    }
}
