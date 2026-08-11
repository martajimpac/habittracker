package com.marta.habittracker.presentation.widgets.weekly

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
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
import com.marta.habittracker.domain.usecase.DaySummary
import com.marta.habittracker.domain.usecase.buildWeeklyHabitSummary
import com.marta.habittracker.presentation.theme.HabitPrimary
import com.marta.habittracker.presentation.theme.HabitSurface
import com.marta.habittracker.presentation.widgets.WidgetEntryPoint
import com.marta.habittracker.presentation.widgets.WidgetLaunchExtras
import dagger.hilt.android.EntryPointAccessors
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.Locale
import kotlinx.coroutines.flow.first

data class WeeklyWidgetDayCell(
    val dayOfWeek: DayOfWeek,
    val percent: Int,
)

class WeeklyGlanceWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
        val summaries = buildWeeklyHabitSummary(
            habits = entryPoint.habitRepository().getAllHabitsWithRecords().first(),
            anchor = LocalDate.now(),
        )

        provideContent {
            WeeklyWidgetContent(
                cells = toWeeklyWidgetDayCells(summaries),
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

@Composable
private fun WeeklyWidgetContent(
    cells: List<WeeklyWidgetDayCell>,
    title: String,
    percentFormat: String,
    dayLabels: Map<DayOfWeek, String>,
    openAppAction: androidx.glance.action.Action,
) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(HabitSurface)
            .clickable(openAppAction)
            .padding(16.dp),
        horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
    ) {
        Text(
            text = title,
            style = TextStyle(fontWeight = FontWeight.Bold),
        )
        Row(modifier = GlanceModifier.padding(top = 12.dp)) {
            cells.forEach { cell ->
                Column(
                    modifier = GlanceModifier
                        .background(if (cell.percent == 0) Color.White else HabitPrimary.copy(alpha = 0.18f))
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
                ) {
                    Text(text = dayLabels.getValue(cell.dayOfWeek))
                    Text(
                        text = String.format(Locale.getDefault(), percentFormat, cell.percent),
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            color = androidx.glance.unit.ColorProvider(HabitPrimary),
                        ),
                    )
                }
            }
        }
    }
}

internal fun toWeeklyWidgetDayCells(summaries: List<DaySummary>): List<WeeklyWidgetDayCell> =
    summaries.map { summary ->
        WeeklyWidgetDayCell(
            dayOfWeek = summary.date.dayOfWeek,
            percent = summary.percent,
        )
    }
