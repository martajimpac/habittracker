package com.marta.habittracker.presentation.widgets.today

import android.content.Context
import android.util.Log
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.state.updateAppWidgetState
import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.presentation.widgets.WidgetEntryPoint
import dagger.hilt.android.EntryPointAccessors
import java.time.LocalDate
import kotlin.coroutines.cancellation.CancellationException

class ToggleTodayHabitAction : ActionCallback {

    companion object {
        const val TAG = "ToggleTodayHabitAction"

        val habitIdKey = ActionParameters.Key<String>("habit_id")
        val habitCompletedKey = ActionParameters.Key<Boolean>("habit_completed")
    }

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val habitId = parameters[habitIdKey] ?: return
        val isCompleted = parameters[habitCompletedKey] ?: return

        val widget = TodayGlanceWidget()

        val repository = EntryPointAccessors
            .fromApplication(context, WidgetEntryPoint::class.java)
            .habitRepository()

        try {
            // 1. Activar el estado de carga.
            updateAppWidgetState(context, glanceId) { prefs ->
                prefs[TodayWidgetPrefs.loadingHabitId] = habitId
            }
            widget.update(context, glanceId)

            // 2. Ejecutar la operación.
            val today = LocalDate.now()

            when (repository.toggleHabitCompletion(habitId, isCompleted, today)) {
                is DataResult.Success -> {
                    Log.d(TAG, "Habit toggled: $habitId")
                }

                is DataResult.Error -> {
                    Log.e(TAG, "Could not toggle habit: $habitId")
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Toggle failed for habit $habitId", e)
        } finally {
            // 3. Quitar el estado de carga siempre.
            updateAppWidgetState(context, glanceId) { prefs ->
                prefs.remove(TodayWidgetPrefs.loadingHabitId)
            }
            widget.update(context, glanceId)
        }
    }
}
