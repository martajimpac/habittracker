package com.marta.habittracker.presentation.widgets.habit

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceId
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.background
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.marta.habittracker.R
import com.marta.habittracker.domain.model.Habit
import com.marta.habittracker.presentation.screens.home.calculateStreak
import com.marta.habittracker.presentation.theme.HabitOnSurface
import com.marta.habittracker.presentation.theme.HabitSurface
import com.marta.habittracker.presentation.utils.parseHabitColor
import com.marta.habittracker.presentation.widgets.WidgetCheck
import com.marta.habittracker.presentation.widgets.WidgetEntryPoint
import com.marta.habittracker.presentation.widgets.WidgetHabitRow
import com.marta.habittracker.presentation.widgets.WidgetLaunchExtras
import com.marta.habittracker.presentation.widgets.WidgetLoad
import com.marta.habittracker.presentation.widgets.WidgetLoading
import com.marta.habittracker.presentation.widgets.WidgetScreen
import com.marta.habittracker.presentation.widgets.widgetDayName
import dagger.hilt.android.EntryPointAccessors
import java.time.LocalDate
import kotlinx.coroutines.flow.first

class HabitGlanceWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
        val appWidgetId = androidx.glance.appwidget.GlanceAppWidgetManager(context).getAppWidgetId(id)
        val toggleAction = actionRunCallback<ToggleHabitAction>(
            parameters = androidx.glance.action.actionParametersOf(
                ToggleHabitAction.appWidgetIdKey to appWidgetId,
            ),
        )

        provideContent {
            val state by produceState<WidgetLoad<Habit?>>(WidgetLoad.Loading, appWidgetId) {
                value = loadSelectedHabit(entryPoint, appWidgetId)
            }
            when (val current = state) {
                WidgetLoad.Loading -> WidgetLoading(context.getString(R.string.widget_loading))
                is WidgetLoad.Ready -> {
                    val habit = current.value
                    HabitWidgetContent(
                        habit = habit,
                        configureText = context.getString(R.string.habit_widget_configure),
                        completedText = context.getString(R.string.habit_widget_completed),
                        notCompletedText = context.getString(R.string.habit_widget_not_completed),
                        dayName = widgetDayName(LocalDate.now()),
                        streakText = habit?.let {
                            val streak = calculateStreak(it)
                            if (streak > 0) {
                                context.getString(R.string.habit_widget_streak, streak)
                            } else {
                                ""
                            }
                        }.orEmpty(),
                        openAppAction = actionStartActivity(
                            WidgetLaunchExtras.openHomeIntent(context, habit?.id),
                        ),
                        toggleAction = toggleAction,
                    )
                }
            }
        }
    }

    private companion object {
        const val TAG = "HabitWidget"

        suspend fun loadSelectedHabit(
            entryPoint: WidgetEntryPoint,
            appWidgetId: Int,
        ): WidgetLoad<Habit?> = try {
            val habitId = entryPoint.widgetPreferencesDataSource().getHabitWidgetHabitId(appWidgetId)
            val habit = habitId?.let { selectedHabitId ->
                entryPoint.habitRepository()
                    .getAllHabitsWithRecords()
                    .first()
                    .firstOrNull { it.id == selectedHabitId }
                    ?.withTodayCompletion(LocalDate.now())
            }
            WidgetLoad.Ready(habit)
        } catch (exception: Exception) {
            Log.e(TAG, "Habit widget load failed for widget $appWidgetId", exception)
            WidgetLoad.Ready(null)
        }
    }
}

@Composable
internal fun HabitWidgetContent(
    habit: Habit?,
    configureText: String,
    completedText: String,
    notCompletedText: String,
    dayName: String,
    streakText: String,
    openAppAction: androidx.glance.action.Action,
    toggleAction: androidx.glance.action.Action,
) {
    if (habit == null) {
        Column(
            modifier = androidx.glance.GlanceModifier
                .fillMaxSize()
                .background(HabitSurface)
                .clickable(openAppAction)
                .padding(16.dp),
            verticalAlignment = Alignment.Vertical.CenterVertically,
            horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
        ) {
            Text(
                text = configureText,
                style = TextStyle(color = ColorProvider(HabitOnSurface)),
            )
        }
        return
    }
    val accent = parseHabitColor(habit.colorHex)
    val status = if (habit.isCompleted) completedText else notCompletedText
    WidgetScreen(
        headerTitle = dayName,
        headerSubtitle = habit.name,
        openAppAction = openAppAction,
    ) {
        WidgetHabitRow(
            name = habit.name,
            iconKey = habit.icon,
            accent = accent,
            completed = habit.isCompleted,
            detail = listOf(status, streakText).filter { it.isNotBlank() }.joinToString(" · "),
            trailing = {
                WidgetCheck(
                    completed = habit.isCompleted,
                    accent = accent,
                    onToggle = toggleAction,
                )
            },
        )
    }
}

internal fun Habit.withTodayCompletion(today: LocalDate): Habit =
    copy(
        isCompleted = records.any { record ->
            record.date.year == today.year &&
                record.date.month.ordinal + 1 == today.monthValue &&
                record.date.day == today.dayOfMonth &&
                record.isCompleted
        },
    )
