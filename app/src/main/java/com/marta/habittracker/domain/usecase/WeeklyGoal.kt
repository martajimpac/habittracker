package com.marta.habittracker.domain.usecase

import com.marta.habittracker.domain.model.Habit
import java.time.DayOfWeek
import java.time.LocalDate

const val WEEKLY_GOAL_TARGET_DAYS = 5

data class WeeklyGoalDay(
    val dayOfWeek: DayOfWeek,
    val counts: Boolean,
)

data class WeeklyGoalProgress(
    val completedDays: Int,
    val targetDays: Int,
    val days: List<WeeklyGoalDay>,
)

/**
 * Monday–Sunday week containing [anchor]. A day counts when at least one habit is scheduled
 * and every scheduled habit has a completed record that day. Days with nothing scheduled do not count.
 */
fun weeklyGoalProgress(habits: List<Habit>, anchor: LocalDate): WeeklyGoalProgress {
    val days = buildWeeklyHabitSummary(habits, anchor).map { day ->
        WeeklyGoalDay(
            dayOfWeek = day.date.dayOfWeek,
            counts = day.scheduled > 0 && day.completed == day.scheduled,
        )
    }
    return WeeklyGoalProgress(
        completedDays = days.count { it.counts },
        targetDays = WEEKLY_GOAL_TARGET_DAYS,
        days = days,
    )
}
