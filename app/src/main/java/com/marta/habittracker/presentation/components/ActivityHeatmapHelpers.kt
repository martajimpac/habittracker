package com.marta.habittracker.presentation.components

import com.marta.habittracker.domain.model.Habit
import com.marta.habittracker.presentation.screens.home.habitsForDate
import com.marta.habittracker.presentation.screens.home.isHabitCompletedOnDate
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

data class ActivityHeatmapCell(
    val date: LocalDate,
    /** 0f = Less (white), 1f = More (dark purple). */
    val intensity: Float,
)

data class ActivityHeatmapWeek(
    val weekIndex: Int, // 1..4
    val cells: List<ActivityHeatmapCell>, // size 7, Mon..Sun
)

/**
 * Last 4 Monday-aligned weeks ending with the week that contains [today].
 * Week 1 = oldest, Week 4 = current week.
 */
fun buildRollingFourWeeks(today: LocalDate = LocalDate.now()): List<LocalDate> {
    val currentMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val startMonday = currentMonday.minusWeeks(3)
    return (0 until 28).map { startMonday.plusDays(it.toLong()) }
}

fun toHeatmapWeeks(
    dates: List<LocalDate>,
    intensityForDate: (LocalDate) -> Float,
): List<ActivityHeatmapWeek> {
    require(dates.size == 28) { "Expected 28 days (4 weeks)" }
    return dates.chunked(7).mapIndexed { index, weekDates ->
        ActivityHeatmapWeek(
            weekIndex = index + 1,
            cells = weekDates.map { date ->
                ActivityHeatmapCell(
                    date = date,
                    intensity = intensityForDate(date).coerceIn(0f, 1f),
                )
            },
        )
    }
}

fun habitDayIntensity(habit: Habit, date: LocalDate): Float =
    if (isHabitCompletedOnDate(habit, date)) 1f else 0f

fun globalDayIntensity(habits: List<Habit>, date: LocalDate): Float {
    val dayHabits = habitsForDate(habits, date)
    if (dayHabits.isEmpty()) return 0f
    val completed = dayHabits.count { isHabitCompletedOnDate(it, date) }
    return completed.toFloat() / dayHabits.size
}
