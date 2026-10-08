package com.marta.habittracker.presentation.widgets.weekly

import androidx.compose.runtime.Composable
import androidx.glance.action.action
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import java.time.DayOfWeek

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 250, heightDp = 110)
@Composable
fun WeeklyGlanceWidgetPreview() {
    WeeklyWidgetContent(
        cells = listOf(
            WeeklyWidgetDayCell(DayOfWeek.MONDAY, percent = 80),
            WeeklyWidgetDayCell(DayOfWeek.TUESDAY, percent = 80),
            WeeklyWidgetDayCell(DayOfWeek.WEDNESDAY, percent = 80),
            WeeklyWidgetDayCell(DayOfWeek.THURSDAY, percent = 80),
            WeeklyWidgetDayCell(DayOfWeek.FRIDAY, percent = 80),
            WeeklyWidgetDayCell(DayOfWeek.SATURDAY, percent = 0),
            WeeklyWidgetDayCell(DayOfWeek.SUNDAY, percent = 0),
        ),
        title = "This week",
        percentFormat = "%1\$d%%",
        dayLabels = mapOf(
            DayOfWeek.MONDAY to "Mon",
            DayOfWeek.TUESDAY to "Tue",
            DayOfWeek.WEDNESDAY to "Wed",
            DayOfWeek.THURSDAY to "Thu",
            DayOfWeek.FRIDAY to "Fri",
            DayOfWeek.SATURDAY to "Sat",
            DayOfWeek.SUNDAY to "Sun",
        ),
        openAppAction = action { },
    )
}
