package com.marta.habittracker.presentation.widgets.habit

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.marta.habittracker.R
import com.marta.habittracker.domain.model.Habit
import com.marta.habittracker.presentation.components.habitIconRes
import com.marta.habittracker.presentation.screens.home.calculateStreak
import com.marta.habittracker.presentation.screens.home.parseHabitColor
import com.marta.habittracker.presentation.theme.HabitSurface
import com.marta.habittracker.presentation.widgets.WidgetEntryPoint
import com.marta.habittracker.presentation.widgets.WidgetLaunchExtras
import dagger.hilt.android.EntryPointAccessors
import java.time.LocalDate
import kotlinx.coroutines.flow.first

class HabitGlanceWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
        val appWidgetId = androidx.glance.appwidget.GlanceAppWidgetManager(context).getAppWidgetId(id)
        val habitId = entryPoint.widgetPreferencesDataSource().getHabitWidgetHabitId(appWidgetId)
        val habit = habitId?.let { selectedHabitId ->
            entryPoint.habitRepository()
                .getAllHabitsWithRecords()
                .first()
                .firstOrNull { it.id == selectedHabitId }
                ?.withTodayCompletion(LocalDate.now())
        }
        val openAppAction = actionStartActivity(
            WidgetLaunchExtras.openHomeIntent(context, habit?.id),
        )

        provideContent {
            HabitWidgetContent(
                habit = habit,
                appWidgetId = appWidgetId,
                configureText = context.getString(R.string.habit_widget_configure),
                completedText = context.getString(R.string.habit_widget_completed),
                notCompletedText = context.getString(R.string.habit_widget_not_completed),
                markCompleteText = context.getString(R.string.habit_widget_mark_complete),
                markIncompleteText = context.getString(R.string.habit_widget_mark_incomplete),
                streakText = habit?.let {
                    val streak = calculateStreak(it.records)
                    if (streak > 0) {
                        context.getString(R.string.habit_widget_streak, streak)
                    } else {
                        ""
                    }
                }.orEmpty(),
                openAppAction = openAppAction,
            )
        }
    }
}

@Composable
private fun HabitWidgetContent(
    habit: Habit?,
    appWidgetId: Int,
    configureText: String,
    completedText: String,
    notCompletedText: String,
    markCompleteText: String,
    markIncompleteText: String,
    streakText: String,
    openAppAction: androidx.glance.action.Action,
) {
    val accent = habit?.let { parseHabitColor(it.colorHex) }
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(accent?.copy(alpha = 0.12f) ?: HabitSurface)
            .clickable(openAppAction)
            .padding(16.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically,
        horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
    ) {
        if (habit == null) {
            Text(text = configureText)
        } else {
            Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
                Image(
                    provider = ImageProvider(habitIconRes(habit.icon)),
                    contentDescription = null,
                    modifier = GlanceModifier.padding(end = 8.dp),
                )
                Text(
                    text = habit.name,
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        color = androidx.glance.unit.ColorProvider(requireNotNull(accent)),
                    ),
                )
            }
            Text(
                text = if (habit.isCompleted) completedText else notCompletedText,
            )
            if (streakText.isNotEmpty()) {
                Text(text = streakText)
            }
            Button(
                text = if (habit.isCompleted) markIncompleteText else markCompleteText,
                onClick = actionRunCallback<ToggleHabitAction>(
                    parameters = androidx.glance.action.actionParametersOf(
                        ToggleHabitAction.appWidgetIdKey to appWidgetId,
                    ),
                ),
            )
        }
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
