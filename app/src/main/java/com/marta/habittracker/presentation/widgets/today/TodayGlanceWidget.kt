package com.marta.habittracker.presentation.widgets.today

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.Action
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.action.clickable
import androidx.glance.currentState
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.marta.habittracker.R
import com.marta.habittracker.presentation.theme.HabitOnSurfaceVariant
import com.marta.habittracker.presentation.utils.parseHabitColor
import com.marta.habittracker.presentation.widgets.WidgetCheck
import com.marta.habittracker.presentation.widgets.WidgetHabitRow
import com.marta.habittracker.presentation.widgets.WidgetLaunchExtras
import com.marta.habittracker.presentation.widgets.WidgetLoad
import com.marta.habittracker.presentation.widgets.WidgetLoading
import com.marta.habittracker.presentation.widgets.WidgetScrollingScreen
import com.marta.habittracker.presentation.widgets.widgetDateLabel
import com.marta.habittracker.presentation.widgets.widgetDayName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalDate
import android.util.Log
import androidx.datastore.preferences.core.Preferences


object TodayWidgetPrefs {
    val habitsJson = stringPreferencesKey("today_habits")
    /** ISO date the stored list belongs to; written together with [habitsJson]. */
    val date = stringPreferencesKey("today_date")
    val loadingHabitId = stringPreferencesKey("loading_habit_id")
}

/**
 * Draws only what [com.marta.habittracker.presentation.widgets.WidgetRefresher] stored in the
 * widget's Glance state. It never reads Room itself.
 */
class TodayGlanceWidget : GlanceAppWidget() {

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        provideContent {
            val state = currentState<Preferences>()
            val loadingHabitId = state[TodayWidgetPrefs.loadingHabitId]
            val storedHabits = state[TodayWidgetPrefs.habitsJson]?.let { json ->
                runCatching { Json.decodeFromString<List<TodayWidgetItem>>(json) }
                    .onFailure { Log.e(TAG, "Could not decode Today widget habits", it) }
                    .getOrNull()
            }
            val date = state[TodayWidgetPrefs.date]
                ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            // A list from another day is stale: wait for the next push instead of showing it as today.
            val load = if (storedHabits != null && date == LocalDate.now()) {
                WidgetLoad.Ready(storedHabits)
            } else {
                WidgetLoad.Loading
            }

            when (load) {
                WidgetLoad.Loading -> WidgetLoading(context.getString(R.string.widget_loading))
                is WidgetLoad.Ready -> {
                    val today = date ?: LocalDate.now()
                    val habits = load.value
                    val countText = if (habits.isEmpty()) {
                        null
                    } else {
                        context.getString(
                            R.string.today_widget_count,
                            habits.count { it.isCompleted },
                            habits.size,
                        )
                    }
                    TodayWidgetContent(
                        dayName = widgetDayName(today),
                        dateLabel = context.getString(
                            R.string.home_today_label,
                            widgetDateLabel(today),
                        ),
                        countText = countText,
                        rows = habits,
                        loadingHabitId = loadingHabitId,
                        emptyText = context.getString(R.string.today_widget_empty),
                        openAppText = context.getString(R.string.widget_open_app),
                        openAppAction = actionStartActivity(
                            WidgetLaunchExtras.openHomeIntent(context),
                        ),
                    )
                }
            }
        }
    }

    private companion object {
        const val TAG = "TodayGlanceWidget"
    }
}

@Serializable
data class TodayWidgetItem(
    val id: String,
    val name: String,
    val colorHex: String,
    val icon: String,
    val isCompleted: Boolean,
)

@SuppressLint("RestrictedApi")
@Composable
internal fun TodayWidgetContent(
    dayName: String,
    dateLabel: String,
    countText: String?,
    rows: List<TodayWidgetItem>,
    emptyText: String,
    openAppText: String,
    openAppAction: Action,
    loadingHabitId: String?,
) {


    val subtitle = listOfNotNull(dateLabel, countText).joinToString("  ")
    WidgetScrollingScreen(
        headerTitle = dayName,
        headerSubtitle = subtitle,
        openAppAction = openAppAction,
        openAppButtonText = openAppText,
    ) {
        if (rows.isEmpty()) {
            item {
                Text(
                    text = emptyText,
                    modifier = GlanceModifier.clickable(openAppAction).padding(vertical = 4.dp),
                    style = TextStyle(
                        color = ColorProvider(HabitOnSurfaceVariant)
                    ),
                )
            }
        } else {
            items(
                items = rows,
                itemId = { it.id.hashCode().toLong() }
            ) { row ->

                val accent = parseHabitColor(row.colorHex)
                val isLoading = loadingHabitId == row.id

                val toggleAction = actionRunCallback<ToggleTodayHabitAction>(
                    actionParametersOf(
                        ToggleTodayHabitAction.habitIdKey to row.id,
                        ToggleTodayHabitAction.habitCompletedKey to row.isCompleted,
                    )
                )

                WidgetHabitRow(
                    name = row.name,
                    accent = accent,
                    completed = row.isCompleted,
                    detail = null,
                    onClick = toggleAction,
                    trailing = {
                        // Always the same composable; loading only changes its content.
                        WidgetCheck(
                            completed = row.isCompleted,
                            accent = accent,
                            onToggle = toggleAction,
                            loading = isLoading,
                        )
                    },
                    iconKey = row.icon,
                )
            }
        }
    }
}


