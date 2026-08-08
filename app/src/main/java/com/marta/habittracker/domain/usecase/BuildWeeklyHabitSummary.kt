package com.marta.habittracker.domain.usecase

import com.marta.habittracker.core.toJava
import com.marta.habittracker.domain.model.Habit
import java.time.DayOfWeek
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
 * For each day, [DaySummary.scheduled] counts habits whose [Habit.daysOfWeek] includes that
 * weekday. [DaySummary.completed] counts those scheduled habits that have a record on that
 * date with [com.marta.habittracker.domain.model.HabitRecord.isCompleted] true.
 *
 * Scheduling semantics mirror Home/Stats: a habit is scheduled on a day when its
 * [Habit.daysOfWeek] contains the day's weekday (kotlinx vs java [DayOfWeek] matched by name).
 */
fun buildWeeklyHabitSummary(habits: List<Habit>, anchor: LocalDate): List<DaySummary> {
    val monday = anchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    return (0 until 7).map { offset ->
        val date = monday.plusDays(offset.toLong())
        val scheduledHabits = habitsScheduledOn(habits, date)
        val completed = scheduledHabits.count { isHabitCompletedOnDate(it, date) }
        DaySummary(
            date = date,
            completed = completed,
            scheduled = scheduledHabits.size,
        )
    }
}

private fun habitsScheduledOn(habits: List<Habit>, date: LocalDate): List<Habit> {
    val dayOfWeek = date.dayOfWeek
    return habits.filter { habit ->
        habit.daysOfWeek.any { kotlinDay -> kotlinDay.name == dayOfWeek.name }
    }
}

private fun isHabitCompletedOnDate(habit: Habit, date: LocalDate): Boolean =
    habit.records.any { it.isCompleted && it.date.toJava() == date }
