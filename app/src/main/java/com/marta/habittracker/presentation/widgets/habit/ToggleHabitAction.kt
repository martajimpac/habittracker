package com.marta.habittracker.presentation.widgets.habit

import android.content.Context
import android.util.Log
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.model.AppError
import com.marta.habittracker.domain.model.Habit
import com.marta.habittracker.presentation.widgets.WidgetEntryPoint
import com.marta.habittracker.presentation.widgets.WidgetRefresher
import dagger.hilt.android.EntryPointAccessors
import java.time.LocalDate
import kotlinx.coroutines.flow.first

internal sealed interface HabitWidgetToggleOutcome {
    data object IgnoredMissingHabit : HabitWidgetToggleOutcome
    data object ToggledAndShouldRefresh : HabitWidgetToggleOutcome
    data object ToggleFailed : HabitWidgetToggleOutcome
}

/**
 * Resolves the configured habit, applies today's completion flag, then toggles via [toggle].
 * Returns whether widgets should refresh (success) or be left alone (missing/error).
 */
internal suspend fun performHabitWidgetToggle(
    configuredHabitId: String?,
    habits: List<Habit>,
    today: LocalDate,
    toggle: suspend (Habit, LocalDate) -> DataResult<Unit, AppError>,
): HabitWidgetToggleOutcome {
    val habit = configuredHabitId
        ?.let { id -> habits.firstOrNull { it.id == id } }
        ?.withTodayCompletion(today)
        ?: return HabitWidgetToggleOutcome.IgnoredMissingHabit

    return when (toggle(habit, today)) {
        is DataResult.Success -> HabitWidgetToggleOutcome.ToggledAndShouldRefresh
        is DataResult.Error -> HabitWidgetToggleOutcome.ToggleFailed
    }
}

class ToggleHabitAction : ActionCallback {

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val appWidgetId = parameters[appWidgetIdKey] ?: return
        val entryPoint = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
        val repository = entryPoint.habitRepository()
        val today = LocalDate.now()

        try {
            val configuredHabitId =
                entryPoint.widgetPreferencesDataSource().getHabitWidgetHabitId(appWidgetId)
            val habits = repository.getAllHabitsWithRecords().first()
            when (
                performHabitWidgetToggle(
                    configuredHabitId = configuredHabitId,
                    habits = habits,
                    today = today,
                    toggle = repository::toggleHabitCompletion,
                )
            ) {
                HabitWidgetToggleOutcome.IgnoredMissingHabit ->
                    Log.w(TAG, "Toggle ignored because the configured habit was not found")

                HabitWidgetToggleOutcome.ToggledAndShouldRefresh ->
                    WidgetRefresher.refreshAll(context)

                HabitWidgetToggleOutcome.ToggleFailed ->
                    Log.e(TAG, "Toggle failed for widgetId=$appWidgetId")
            }
        } catch (exception: Exception) {
            Log.e(TAG, "Toggle failed for widgetId=$appWidgetId", exception)
        }
    }

    companion object {
        private const val TAG = "HabitWidgetToggle"
        val appWidgetIdKey = ActionParameters.Key<Int>("habit_widget_id")
    }
}
