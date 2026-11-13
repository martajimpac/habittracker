package com.marta.habittracker.presentation.widgets

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import com.marta.habittracker.domain.model.Habit
import com.marta.habittracker.presentation.widgets.today.TodayGlanceWidget
import com.marta.habittracker.presentation.widgets.today.TodayWidgetItem
import com.marta.habittracker.presentation.widgets.today.TodayWidgetPrefs
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import java.time.LocalDate
import kotlin.coroutines.cancellation.CancellationException

object WidgetRefresher {

    /**
     * Writes today's habits into every Today widget's Glance state and redraws it.
     * The date is stored with the list, so the header always matches the rows.
     */
    suspend fun refreshTodayWidget(habits: List<Habit>, context: Context) {
        try {
            val json = Json.encodeToString(habits.map { it.toTodayWidgetItem() })
            val date = LocalDate.now().toString()
            val glance = TodayGlanceWidget()
            GlanceAppWidgetManager(context)
                .getGlanceIds(TodayGlanceWidget::class.java)
                .forEach { glanceId ->
                    updateAppWidgetState(context, glanceId) { prefs ->
                        prefs[TodayWidgetPrefs.habitsJson] = json
                        prefs[TodayWidgetPrefs.date] = date
                    }
                    glance.update(context, glanceId)
                }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to update Today widget", exception)
        }
    }

    /**
     * For callers outside the repository (widget receiver, app lifecycle): loads today's
     * habits from Room with the same query as Home and pushes them.
     */
    suspend fun refreshTodayWidget(context: Context) {
        try {
            val habits = EntryPointAccessors
                .fromApplication(context, WidgetEntryPoint::class.java)
                .habitRepository()
                .getHabitsWithStatus(LocalDate.now())
                .first()
            refreshTodayWidget(habits, context)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to load today's habits for the widget", exception)
        }
    }

    private fun Habit.toTodayWidgetItem() = TodayWidgetItem(
        id = id,
        name = name,
        colorHex = colorHex,
        icon = icon,
        isCompleted = isCompleted,
    )

    private const val TAG = "WidgetRefresher"
}
