package com.marta.habittracker.domain.usecase

import com.marta.habittracker.core.toJava
import com.marta.habittracker.core.toKotlin
import com.marta.habittracker.domain.model.Habit
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.datetime.LocalDate as KotlinLocalDate

/**
 * Stats streak cards show up once a streak reaches these lengths.
 * Change the two values here to move both thresholds.
 */
object StatsStreakMinimums {
    const val ALL_HABITS_DAYS = 3
    const val SINGLE_HABIT_DAYS = 3
}

sealed interface StatsStreakHighlight {
    data class AllHabits(val days: Int, val isLongestSoFar: Boolean) : StatsStreakHighlight
    data class Habit(val habitId: String, val habitName: String, val days: Int) : StatsStreakHighlight
}

private const val STREAK_LOOKBACK_DAYS = 730

/**
 * Names the habit with the longest streak still alive today when that streak is
 * ahead of the run where every scheduled habit was completed.
 * The group card is used only when nobody is ahead of that shared run.
 * Days with nothing scheduled are skipped, so a rest day does not break the run.
 * Either card stays hidden until its minimum in [StatsStreakMinimums].
 */
fun statsStreakHighlight(habits: List<Habit>, today: LocalDate): StatsStreakHighlight? {
    val kotlinToday = KotlinLocalDate.parse(today.toString())
    val longest = habits.mapNotNull { habit ->
        val streak = streakForHabit(
            habit = habit,
            today = kotlinToday,
            createdOn = KotlinLocalDate.parse(
                habit.createdAt.atZone(ZoneId.systemDefault()).toLocalDate().toString(),
            ),
        )
        if (streak >= StatsStreakMinimums.SINGLE_HABIT_DAYS) {
            StatsStreakHighlight.Habit(habit.id, habit.name, streak)
        } else {
            null
        }
    }.maxWithOrNull(
        compareBy<StatsStreakHighlight.Habit> { it.days }
            .thenBy { it.habitName }
            .thenBy { it.habitId },
    )

    val current = perfectStreakEnding(habits, today)
    val longestDays = longest?.days ?: 0
    if (current >= StatsStreakMinimums.ALL_HABITS_DAYS && current >= longestDays) {
        return StatsStreakHighlight.AllHabits(
            days = current,
            isLongestSoFar = current == bestPerfectStreak(habits, today),
        )
    }
    return longest
}

/** Stable index into a list of copy variants. Same streak and day always pick the same line. */
fun statsStreakCopyVariant(streakDays: Int, dayOfYear: Int, variantCount: Int): Int {
    if (variantCount <= 1) return 0
    return Math.floorMod(streakDays + dayOfYear, variantCount)
}

private enum class ScheduledDay { Perfect, Miss, Rest }

private fun perfectStreakEnding(habits: List<Habit>, today: LocalDate): Int {
    var streak = 0
    var date = today
    repeat(STREAK_LOOKBACK_DAYS) {
        when (scheduledDay(habits, date, today)) {
            ScheduledDay.Perfect -> streak++
            ScheduledDay.Miss -> return streak
            ScheduledDay.Rest -> Unit
        }
        date = date.minusDays(1)
    }
    return streak
}

private fun bestPerfectStreak(habits: List<Habit>, today: LocalDate): Int {
    var best = 0
    var run = 0
    var date = today.minusDays((STREAK_LOOKBACK_DAYS - 1).toLong())
    repeat(STREAK_LOOKBACK_DAYS) {
        when (scheduledDay(habits, date, today)) {
            ScheduledDay.Perfect -> {
                run++
                best = maxOf(best, run)
            }
            ScheduledDay.Miss -> run = 0
            ScheduledDay.Rest -> Unit
        }
        date = date.plusDays(1)
    }
    return best
}

private fun scheduledDay(habits: List<Habit>, date: LocalDate, today: LocalDate): ScheduledDay {
    val due = habits.filter { isScheduled(it, date, today) }
    if (due.isEmpty()) return ScheduledDay.Rest
    val allDone = due.all { habit ->
        habit.records.any { record -> record.isCompleted && record.date.toJava() == date }
    }
    return if (allDone) ScheduledDay.Perfect else ScheduledDay.Miss
}

private fun isScheduled(habit: Habit, date: LocalDate, today: LocalDate): Boolean {
    val created = habit.createdAt.atZone(ZoneId.systemDefault()).toLocalDate()
    if (date.isBefore(created)) return false
    return habit.scheduledForStats(date.toKotlin(), today.toKotlin())
}
