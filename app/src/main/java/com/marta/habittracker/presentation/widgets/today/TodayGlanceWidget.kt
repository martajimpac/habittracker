package com.marta.habittracker.presentation.widgets.today

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.unit.dp
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
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.marta.habittracker.R
import com.marta.habittracker.domain.model.Habit
import com.marta.habittracker.presentation.theme.HabitOnSurfaceVariant
import com.marta.habittracker.presentation.utils.parseHabitColor
import com.marta.habittracker.presentation.widgets.WidgetCheck
import com.marta.habittracker.presentation.widgets.WidgetEntryPoint
import com.marta.habittracker.presentation.widgets.WidgetHabitRow
import com.marta.habittracker.presentation.widgets.WidgetLaunchExtras
import com.marta.habittracker.presentation.widgets.WidgetLoad
import com.marta.habittracker.presentation.widgets.WidgetLoading
import com.marta.habittracker.presentation.widgets.WidgetScrollingScreen
import com.marta.habittracker.presentation.widgets.widgetDateLabel
import com.marta.habittracker.presentation.widgets.widgetDayName
import dagger.hilt.android.EntryPointAccessors
import java.time.LocalDate
import kotlinx.coroutines.flow.first

class TodayGlanceWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
        provideContent {
            val state by produceState<WidgetLoad<List<Habit>>>(WidgetLoad.Loading) {
                value = loadTodayHabits(entryPoint)
            }
            when (val current = state) {
                WidgetLoad.Loading -> WidgetLoading(context.getString(R.string.widget_loading))
                is WidgetLoad.Ready -> {
                    val today = LocalDate.now()
                    val snapshot = todayWidgetSnapshot(habits = current.value, today = today)
                    val countText = if (snapshot.items.isEmpty()) {
                        null
                    } else {
                        context.getString(
                            R.string.today_widget_count,
                            snapshot.items.count { it.completed },
                            snapshot.items.size,
                        )
                    }
                    TodayWidgetContent(
                        dayName = widgetDayName(today),
                        dateLabel = context.getString(R.string.home_today_label, widgetDateLabel(today)),
                        countText = countText,
                        rows = snapshot.items,
                        emptyText = context.getString(R.string.today_widget_empty),
                        openAppAction = actionStartActivity(
                            WidgetLaunchExtras.openHomeIntent(context, snapshot.firstPendingId),
                        ),
                    )
                }
            }
        }
    }

    private companion object {
        const val TAG = "TodayWidget"

        suspend fun loadTodayHabits(entryPoint: WidgetEntryPoint): WidgetLoad<List<Habit>> =
            try {
                WidgetLoad.Ready(entryPoint.habitRepository().getAllHabitsWithRecords().first())
            } catch (exception: Exception) {
                Log.e(TAG, "Today widget load failed", exception)
                WidgetLoad.Ready(emptyList())
            }
    }
}

@Composable
internal fun TodayWidgetContent(
    dayName: String,
    dateLabel: String,
    countText: String?,
    rows: List<TodayWidgetItem>,
    emptyText: String,
    openAppAction: Action,
) {
    val subtitle = listOfNotNull(dateLabel, countText).joinToString("  ")
    WidgetScrollingScreen(
        headerTitle = dayName,
        headerSubtitle = subtitle,
        openAppAction = openAppAction,
    ) {
        if (rows.isEmpty()) {
            item {
                Text(
                    text = emptyText,
                    modifier = GlanceModifier.clickable(openAppAction).padding(vertical = 4.dp),
                    style = TextStyle(color = ColorProvider(HabitOnSurfaceVariant)),
                )
            }
        } else {
            items(rows) { row ->
                val accent = parseHabitColor(row.colorHex)
                val toggleAction = actionRunCallback<ToggleTodayHabitAction>(
                    actionParametersOf(ToggleTodayHabitAction.habitIdKey to row.id),
                )
                WidgetHabitRow(
                    name = row.name,
                    iconKey = row.iconKey,
                    accent = accent,
                    completed = row.completed,
                    detail = null,
                    onClick = openAppAction,
                    trailing = {
                        WidgetCheck(
                            completed = row.completed,
                            accent = accent,
                            onToggle = toggleAction,
                        )
                    },
                )
            }
        }
    }
}
