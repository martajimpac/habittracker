package com.marta.habittracker.domain.usecase

import com.marta.habittracker.core.toJava
import com.marta.habittracker.domain.model.Habit
import java.time.DayOfWeek
import kotlinx.datetime.LocalDate as KotlinLocalDate
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

data class DaySummary(val date: LocalDate, val completed: Int, val scheduled: Int) {
    val percent: Int get() = if (scheduled == 0) 0 else (completed * 100) / scheduled
}

/**
 * Builds a Mon–Sun summary for the calendar week that contains [anchor].
 *
 * Week boundaries follow ISO-style Monday start: the returned list always begins on the
 * Monday on or before [anchor] and ends on the following Sunday (seven days total).
 * For example, anchor Wednesday 2026-08-05 yields Mon 2026-08-03 through Sun 2026-08-09.
 *
 * For each day, [DaySummary.scheduled] counts habits due that day. A 7-day habit is due on
 * each saved weekday. A smaller weekly count is due today and later until the count is
 * met, and on past days only when it was completed.
 */
fun buildWeeklyHabitSummary(habits: List<Habit>, anchor: LocalDate): List<DaySummary> {
    val monday = anchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    return (0 until 7).map { offset ->
        val date = monday.plusDays(offset.toLong())
        val scheduledHabits = habitsScheduledOn(habits, date, anchor)
        val completed = scheduledHabits.count { isHabitCompletedOnDate(it, date) }
        DaySummary(
            date = date,
            completed = completed,
            scheduled = scheduledHabits.size,
        )
    }
}

private fun habitsScheduledOn(habits: List<Habit>, date: LocalDate, today: LocalDate): List<Habit> =
    habits.filter {
        it.scheduledForStats(
            KotlinLocalDate.parse(date.toString()),
            KotlinLocalDate.parse(today.toString()),
        )
    }

private fun isHabitCompletedOnDate(habit: Habit, date: LocalDate): Boolean =
    habit.records.any { it.isCompleted && it.date.toJava() == date }
