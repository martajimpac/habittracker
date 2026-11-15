package com.marta.habittracker.domain.usecase

import com.marta.habittracker.domain.model.Habit
import com.marta.habittracker.domain.model.HabitCatalog
import com.marta.habittracker.domain.model.HabitSchedule
import com.marta.habittracker.domain.model.resolvedSchedule
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate

/**
 * What Bloom knows about one habit this week: the counts for advice, plus the id, schedule and
 * done days it needs to propose marking a day.
 */
data class HabitWeekStat(
    val name: String,
    val completedDays: Int,
    val totalDays: Int,
    val id: String = "",
    val schedule: HabitSchedule = HabitSchedule.DAILY,
    val days: Set<DayOfWeek> = emptySet(),
    val weeklyTarget: Int = 7,
    val doneDates: List<LocalDate> = emptyList(),
)

/** Everything sent to Bloom with a chat message. [habits] may be empty. */
data class ChatContext(
    val habits: List<HabitWeekStat>,
    val today: LocalDate,
    val catalog: HabitCatalog,
)

fun habitWeekRecommendationStats(habits: List<Habit>, today: LocalDate): List<HabitWeekStat> {
    return habits.mapNotNull { habit ->
        val name = habit.name.trim()
        if (name.isEmpty()) {
            null
        } else {
            val schedule = habit.resolvedSchedule()
            val done = habit.completedInWeek(today).filter { it <= today }.sorted()
            HabitWeekStat(
                name = name,
                completedDays = habit.completedInWeek(today).size,
                totalDays = habit.weeklyTarget(),
                id = habit.id,
                schedule = schedule,
                days = if (schedule == HabitSchedule.WEEKDAYS) habit.daysOfWeek else emptySet(),
                weeklyTarget = habit.daysPerWeek.coerceIn(1, 7),
                doneDates = done,
            )
        }
    }
}
