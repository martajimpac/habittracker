package com.marta.habittracker.presentation.screens.detail

import com.marta.habittracker.core.toJava
import com.marta.habittracker.domain.model.HabitRecord
import kotlinx.datetime.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.time.DayOfWeek as JavaDayOfWeek

data class WeekdayCompletion(
    val day: JavaDayOfWeek,
    val count: Int,
)

data class WeekCompletion(
    val weekStart: LocalDate,
    val completed: Int,
    val scheduled: Int,
) {
    val rate: Float
        get() = if (scheduled == 0) 0f else completed.toFloat() / scheduled
}

/** Completed days grouped Monday to Sunday. Days after [today] are left out. */
fun weekdayCompletions(
    records: List<HabitRecord>,
    today: LocalDate,
): List<WeekdayCompletion> {
    val counts = IntArray(7)
    records.forEach { record ->
        if (!record.isCompleted) return@forEach
        val date = record.date.toJava()
        if (date.isAfter(today)) return@forEach
        counts[date.dayOfWeek.value - 1]++
    }
    return JavaDayOfWeek.entries.map { day ->
        WeekdayCompletion(day = day, count = counts[day.value - 1])
    }
}

fun completionPercent(records: List<HabitRecord>): Float {
    if (records.isEmpty()) return 0f
    return records.count { it.isCompleted } * 100f / records.size
}

/**
 * One bar per week, oldest first. A day counts only when it is scheduled,
 * on or after [createdOn], and not after [today].
 */
fun recentWeekCompletions(
    daysOfWeek: Set<DayOfWeek>,
    records: List<HabitRecord>,
    createdOn: LocalDate,
    today: LocalDate,
    weekCount: Int = 4,
): List<WeekCompletion> {
    val scheduledDays = daysOfWeek.map { JavaDayOfWeek.valueOf(it.name) }.toSet()
    val completedDates = records.filter { it.isCompleted }.map { it.date.toJava() }.toSet()
    val thisMonday = today.with(TemporalAdjusters.previousOrSame(JavaDayOfWeek.MONDAY))
    val firstMonday = thisMonday.minusWeeks((weekCount - 1).toLong())
    return List(weekCount) { index ->
        val monday = firstMonday.plusWeeks(index.toLong())
        var scheduled = 0
        var completed = 0
        for (offset in 0..6) {
            val date = monday.plusDays(offset.toLong())
            if (date.isAfter(today) || date.isBefore(createdOn)) continue
            if (date.dayOfWeek !in scheduledDays) continue
            scheduled++
            if (date in completedDates) completed++
        }
        WeekCompletion(weekStart = monday, completed = completed, scheduled = scheduled)
    }
}
