package com.marta.habittracker.presentation.screens.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.marta.habittracker.R
import com.marta.habittracker.core.toJava
import com.marta.habittracker.domain.model.Habit
import com.marta.habittracker.domain.model.HabitRecord
import com.marta.habittracker.domain.usecase.shownOn
import com.marta.habittracker.domain.usecase.streakForHabit
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.datetime.LocalDate as KotlinLocalDate


@Composable
fun habitTimeLabel(habit: Habit): String =
    habit.reminderTime?.takeIf { it.isNotBlank() }
        ?: stringResource(R.string.home_all_day)

fun calculateStreak(
    habit: Habit,
    records: List<HabitRecord> = habit.records,
    today: LocalDate = LocalDate.now(),
): Int {
    val createdOn = KotlinLocalDate.parse(
        habit.createdAt.atZone(ZoneId.systemDefault()).toLocalDate().toString(),
    )
    return streakForHabit(
        habit = habit,
        records = records,
        today = KotlinLocalDate.parse(today.toString()),
        createdOn = createdOn,
    )
}

fun isHabitCompletedOnDate(habit: Habit, date: LocalDate): Boolean =
    habit.records.any { it.isCompleted && it.date.toJava() == date }

fun habitsForDate(habits: List<Habit>, date: LocalDate): List<Habit> =
    habits.filter { it.shownOn(KotlinLocalDate.parse(date.toString())) }

fun completionStats(habits: List<Habit>, date: LocalDate): Pair<Int, Int> {
    val dayHabits = habitsForDate(habits, date)
    if (dayHabits.isEmpty()) return 0 to 0
    val completed = dayHabits.count { isHabitCompletedOnDate(it, date) }
    return completed to dayHabits.size
}

fun completionPercent(completed: Int, total: Int): Int =
    if (total == 0) 0 else ((completed.toFloat() / total) * 100).toInt()

@Composable
fun firstName(displayName: String): String {
    val fallback = stringResource(R.string.user_fallback_name)
    return displayName.trim().split(" ").firstOrNull().orEmpty().ifBlank { fallback }
}
