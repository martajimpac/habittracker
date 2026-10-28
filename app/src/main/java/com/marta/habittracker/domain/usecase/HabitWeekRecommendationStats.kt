package com.marta.habittracker.domain.usecase

import com.marta.habittracker.domain.model.Habit
import kotlinx.datetime.LocalDate

data class HabitWeekStat(
    val name: String,
    val completedDays: Int,
    val totalDays: Int,
)

fun habitWeekRecommendationStats(habits: List<Habit>, today: LocalDate): List<HabitWeekStat> {
    return habits.mapNotNull { habit ->
        val name = habit.name.trim()
        if (name.isEmpty()) {
            null
        } else {
            HabitWeekStat(
                name = name,
                completedDays = habit.completedInWeek(today).size,
                totalDays = habit.weeklyTarget(),
            )
        }
    }
}
