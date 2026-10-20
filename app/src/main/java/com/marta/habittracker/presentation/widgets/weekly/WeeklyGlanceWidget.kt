package com.marta.habittracker.presentation.widgets.weekly

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.marta.habittracker.R
import com.marta.habittracker.domain.model.Habit
import com.marta.habittracker.domain.usecase.DaySummary
import com.marta.habittracker.domain.usecase.buildWeeklyHabitSummary
import com.marta.habittracker.presentation.theme.HabitCream
import com.marta.habittracker.presentation.theme.HabitField
import com.marta.habittracker.presentation.theme.HabitOnSurface
import com.marta.habittracker.presentation.theme.HabitPrimary
import com.marta.habittracker.presentation.widgets.WidgetEntryPoint
import com.marta.habittracker.presentation.widgets.WidgetLaunchExtras
import com.marta.habittracker.presentation.widgets.WidgetLoad
import com.marta.habittracker.presentation.widgets.WidgetLoading
import com.marta.habittracker.presentation.widgets.WidgetScreen
import dagger.hilt.android.EntryPointAccessors
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.Locale
import kotlinx.coroutines.flow.first

data class WeeklyWidgetDayCell(
    val dayOfWeek: DayOfWeek,
    val percent: Int,
    val isToday: Boolean = false,
)

class WeeklyGlanceWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
        provideContent {
            val state by produceState<WidgetLoad<List<Habit>>>(WidgetLoad.Loading) {
                value = loadWeeklyHabits(entryPoint)
            }
            when (val current = state) {
                WidgetLoad.Loading -> WidgetLoading(context.getString(R.string.widget_loading))
                is WidgetLoad.Ready -> {
                    val today = LocalDate.now()
                    val summaries = buildWeeklyHabitSummary(habits = current.value, anchor = today)
                    WeeklyWidgetContent(
                        cells = toWeeklyWidgetDayCells(summaries, today),
                        title = context.getString(R.string.weekly_widget_title),
                        percentFormat = context.getString(R.string.weekly_widget_percent),
                        dayLabels = mapOf(
                            DayOfWeek.MONDAY to context.getString(R.string.weekly_widget_day_mon),
                            DayOfWeek.TUESDAY to context.getString(R.string.weekly_widget_day_tue),
                            DayOfWeek.WEDNESDAY to context.getString(R.string.weekly_widget_day_wed),
                            DayOfWeek.THURSDAY to context.getString(R.string.weekly_widget_day_thu),
                            DayOfWeek.FRIDAY to context.getString(R.string.weekly_widget_day_fri),
                            DayOfWeek.SATURDAY to context.getString(R.string.weekly_widget_day_sat),
                            DayOfWeek.SUNDAY to context.getString(R.string.weekly_widget_day_sun),
                        ),
                        openAppAction = actionStartActivity(WidgetLaunchExtras.openStatsIntent(context)),
                    )
                }
            }
        }
    }

    private companion object {
        const val TAG = "WeeklyWidget"

        suspend fun loadWeeklyHabits(entryPoint: WidgetEntryPoint): WidgetLoad<List<Habit>> =
            try {
                WidgetLoad.Ready(entryPoint.habitRepository().getAllHabitsWithRecords().first())
            } catch (exception: Exception) {
                Log.e(TAG, "Weekly widget load failed", exception)
                WidgetLoad.Ready(emptyList())
            }
    }
}

@Composable
internal fun WeeklyWidgetContent(
    cells: List<WeeklyWidgetDayCell>,
    title: String,
    percentFormat: String,
    dayLabels: Map<DayOfWeek, String>,
    openAppAction: androidx.glance.action.Action,
) {
    WidgetScreen(
        headerTitle = title,
        headerSubtitle = null,
        openAppAction = openAppAction,
    ) {
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            cells.forEach { cell ->
                val todayChip = cell.isToday
                Column(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .padding(horizontal = 2.dp)
                        .cornerRadius(12.dp)
                        .background(ColorProvider(if (todayChip) HabitPrimary else HabitField))
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
                ) {
                    Text(
                        text = dayLabels.getValue(cell.dayOfWeek),
                        style = TextStyle(
                            fontWeight = if (todayChip) FontWeight.Bold else FontWeight.Medium,
                            color = ColorProvider(if (todayChip) HabitCream else HabitOnSurface),
                            textAlign = TextAlign.Center,
                        ),
                    )
                    Text(
                        text = String.format(Locale.getDefault(), percentFormat, cell.percent),
                        modifier = GlanceModifier.padding(top = 4.dp),
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            color = ColorProvider(if (todayChip) HabitCream else HabitPrimary),
                            textAlign = TextAlign.Center,
                        ),
                    )
                }
            }
        }
    }
}

internal fun toWeeklyWidgetDayCells(
    summaries: List<DaySummary>,
    today: LocalDate? = null,
): List<WeeklyWidgetDayCell> =
    summaries.map { summary ->
        WeeklyWidgetDayCell(
            dayOfWeek = summary.date.dayOfWeek,
            percent = summary.percent,
            isToday = summary.date == today,
        )
    }
