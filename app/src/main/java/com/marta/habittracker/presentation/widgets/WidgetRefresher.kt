package com.marta.habittracker.presentation.widgets

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import com.marta.habittracker.domain.model.Habit
import com.marta.habittracker.presentation.widgets.today.TodayGlanceWidget
import com.marta.habittracker.presentation.widgets.today.TodayWidgetItem
import com.marta.habittracker.presentation.widgets.today.TodayWidgetPrefs
import kotlinx.serialization.json.Json
import kotlin.coroutines.cancellation.CancellationException

object WidgetRefresher {

    /*



        refreshWidget("ChallengeGlanceWidget") {
            ChallengeGlanceWidget().updateAll(context)
        }

     */
    suspend fun refreshTodayWidget(habits: List<Habit>, context: Context) {
        val glance = TodayGlanceWidget()
        val manager = GlanceAppWidgetManager(context)

        val glanceIds = manager.getGlanceIds(TodayGlanceWidget::class.java)

        val habitsWidget = habits.map {
            TodayWidgetItem(
                id = it.id,
                name = it.name,
                colorHex = it.colorHex,
                icon = it.icon,
                isCompleted = it.isCompleted,
            )
        }

        val json = Json.encodeToString(habitsWidget)


        glanceIds.forEach { glanceId ->
            updateAppWidgetState(context, glanceId) { prefs ->
                prefs[TodayWidgetPrefs.habitsJson] = json
            }

            try {
                glance.update(context, glanceId)

            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to update", exception)
            }


        }
    }


    private const val TAG = "WidgetRefresher"
}
