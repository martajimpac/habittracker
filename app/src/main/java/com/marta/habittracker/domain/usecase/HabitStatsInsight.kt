package com.marta.habittracker.domain.usecase

import com.marta.habittracker.core.toJava
import com.marta.habittracker.domain.model.Habit
import java.time.DayOfWeek
import java.time.LocalDate
import kotlinx.datetime.LocalDate as KotlinLocalDate

data class StreakAtRisk(
    val habitId: String,
    val habitName: String,
    val streakDays: Int,
)

data class HabitStatsInsight(
    val strongestWeekday: DayOfWeek?,
    val strongestWeekdayCount: Int,
    val streakAtRisk: StreakAtRisk?,
)

/**
 * Strongest weekday counts completed records. A tie keeps the earlier weekday, Monday first.
 * The streak that breaks tomorrow is the longest streak still alive today, using the same
 * rule as Home: consecutive completed days ending on [today]. A habit not completed today
 * already has no streak, so it is not waiting on tomorrow.
 */
fun habitStatsInsight(habits: List<Habit>, today: LocalDate): HabitStatsInsight {
    val counts = habits
        .asSequence()
        .flatMap { it.records }
        .filter { it.isCompleted }
        .groupingBy { it.date.toJava().dayOfWeek }
        .eachCount()
    val strongest = counts.entries.maxWithOrNull(
        compareBy<Map.Entry<DayOfWeek, Int>> { it.value }.thenBy { -it.key.ordinal },
    )

    val kotlinToday = KotlinLocalDate.parse(today.toString())
    val atRisk = habits.mapNotNull { habit ->
        val streak = calculateHabitStreak(habit.records, kotlinToday)
        if (streak == 0) {
            null
        } else {
            StreakAtRisk(habitId = habit.id, habitName = habit.name, streakDays = streak)
        }
    }.maxWithOrNull(
        compareBy<StreakAtRisk> { it.streakDays }
            .thenByDescending { it.habitName }
            .thenByDescending { it.habitId },
    )

    return HabitStatsInsight(
        strongestWeekday = strongest?.key,
        strongestWeekdayCount = strongest?.value ?: 0,
        streakAtRisk = atRisk,
    )
}
