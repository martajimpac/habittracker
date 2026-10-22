package com.marta.habittracker.domain.usecase

import com.marta.habittracker.domain.model.Habit
import com.marta.habittracker.domain.model.HabitRecord
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/** How many days this habit asks for in a Monday–Sunday week. */
fun Habit.weeklyTarget(): Int {
    val explicit = daysPerWeek.coerceIn(1, 7)
    if (explicit < 7) return explicit
    val scheduled = daysOfWeek.size
    return if (scheduled in 1..6) scheduled else 7
}

fun Habit.completedInWeek(date: LocalDate): Set<LocalDate> {
    val start = mondayOf(date)
    val end = start.plus(DatePeriod(days = 6))
    return records
        .filter { it.isCompleted && it.date in start..end }
        .map { it.date }
        .toSet()
}

/** The habit can still be done on [date], or it was already completed that day. */
fun Habit.shownOn(date: LocalDate): Boolean {
    val done = completedInWeek(date)
    if (date in done) return true
    return done.size < weeklyTarget()
}

/**
 * Seven days a week keeps every saved weekday, including past misses.
 * A smaller quota counts a past day only when it was completed.
 * Today and later count while the week still has room before that day.
 */
fun Habit.scheduledForStats(date: LocalDate, today: LocalDate): Boolean {
    val target = weeklyTarget()
    if (target >= 7) return date.dayOfWeek in daysOfWeek
    val done = completedInWeek(date)
    if (date in done) return true
    if (date < today) return false
    return done.count { it < date } < target
}

fun streakForHabit(
    habit: Habit,
    records: List<HabitRecord> = habit.records,
    today: LocalDate,
    createdOn: LocalDate,
): Int {
    val target = habit.weeklyTarget()
    return if (target >= 7) {
        calculateScheduledStreak(
            daysOfWeek = habit.daysOfWeek,
            records = records,
            today = today,
            createdOn = createdOn,
        )
    } else {
        calculateWeeklyQuotaStreak(
            target = target,
            records = records,
            today = today,
            createdOn = createdOn,
        )
    }
}

/**
 * Consecutive Monday–Sunday weeks that reached [target] completions.
 * The current week counts when it is already met. If it can still be met, it
 * does not break the run and it is not counted yet. If it can no longer be met,
 * the streak is 0.
 */
fun calculateWeeklyQuotaStreak(
    target: Int,
    records: List<HabitRecord>,
    today: LocalDate,
    createdOn: LocalDate,
): Int {
    if (target !in 1..6 || today < createdOn) return 0
    val completed = records.filter { it.isCompleted }.map { it.date }.toSet()
    var streak = 0
    var weekStart = mondayOf(today)
    repeat(104) {
        val weekEnd = weekStart.plus(DatePeriod(days = 6))
        if (weekEnd < createdOn) return streak
        val availableStart = maxOf(weekStart, createdOn)
        val needed = minOf(target, inclusiveDays(availableStart, weekEnd))
        val done = completed.count { it in availableStart..weekEnd }
        if (weekStart == mondayOf(today)) {
            val openFrom = maxOf(today, availableStart)
            val open = uncompletedDays(openFrom, weekEnd, completed)
            if (done >= needed) {
                streak++
            } else if (done + open < needed) {
                return 0
            }
        } else if (done >= needed) {
            streak++
        } else {
            return streak
        }
        weekStart = weekStart.minus(DatePeriod(days = 7))
    }
    return streak
}

private fun mondayOf(date: LocalDate): LocalDate =
    date.minus(DatePeriod(days = date.dayOfWeek.ordinal))

private fun inclusiveDays(start: LocalDate, end: LocalDate): Int {
    if (end < start) return 0
    var count = 0
    var cursor = start
    while (cursor <= end) {
        count++
        cursor = cursor.plus(DatePeriod(days = 1))
    }
    return count
}

private fun uncompletedDays(start: LocalDate, end: LocalDate, completed: Set<LocalDate>): Int {
    if (end < start) return 0
    var count = 0
    var cursor = start
    while (cursor <= end) {
        if (cursor !in completed) count++
        cursor = cursor.plus(DatePeriod(days = 1))
    }
    return count
}
