package com.marta.habittracker.domain.usecase

import com.marta.habittracker.domain.model.HabitRecord
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

private const val STREAK_LOOKBACK_DAYS = 730

/**
 * Completed scheduled days in a row, ending today.
 * A day outside [daysOfWeek] is skipped: it does not add to the streak and does not break it.
 * A scheduled day that is not completed breaks the streak, including today.
 * Days before [createdOn] are ignored.
 */
fun calculateScheduledStreak(
    daysOfWeek: Set<DayOfWeek>,
    records: List<HabitRecord>,
    today: LocalDate,
    createdOn: LocalDate,
): Int {
    if (daysOfWeek.isEmpty() || today < createdOn) return 0
    val completedDates = records
        .filter { it.isCompleted }
        .map { it.date }
        .toSet()
    val earliest = maxOf(createdOn, today.minus(DatePeriod(days = STREAK_LOOKBACK_DAYS)))
    var streak = 0
    var date = today
    while (date >= earliest) {
        if (date.dayOfWeek in daysOfWeek) {
            if (!completedDates.contains(date)) return streak
            streak++
        }
        date = date.minus(DatePeriod(days = 1))
    }
    return streak
}
