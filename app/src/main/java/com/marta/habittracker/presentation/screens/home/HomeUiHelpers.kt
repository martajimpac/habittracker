package com.marta.habittracker.presentation.screens.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.marta.habittracker.R
import com.marta.habittracker.core.toJava
import com.marta.habittracker.domain.model.Habit
import com.marta.habittracker.domain.model.HabitRecord
import com.marta.habittracker.presentation.theme.HabitPrimary
import java.time.LocalDate

fun parseHabitColor(colorHex: String): Color {
    val cleaned = colorHex.removePrefix("#").trim()
    if (cleaned.length != 6) return HabitPrimary
    return runCatching {
        Color(("FF$cleaned").toLong(16))
    }.getOrDefault(HabitPrimary)
}

fun habitAccentColor(habit: Habit): Color = parseHabitColor(habit.colorHex)

@Composable
fun habitTimeLabel(habit: Habit): String =
    habit.reminderTime?.takeIf { it.isNotBlank() }
        ?: stringResource(R.string.home_all_day)

fun calculateStreak(records: List<HabitRecord>, today: LocalDate = LocalDate.now()): Int {
    val completedDates = records
        .filter { it.isCompleted }
        .map { it.date.toJava() }
        .toSet()

    var streak = 0
    var date = today
    while (completedDates.contains(date)) {
        streak++
        date = date.minusDays(1)
    }
    return streak
}

fun isHabitCompletedOnDate(habit: Habit, date: LocalDate): Boolean =
    habit.records.any { it.isCompleted && it.date.toJava() == date }

fun habitsForDate(habits: List<Habit>, date: LocalDate): List<Habit> {
    val dayOfWeek = date.dayOfWeek
    return habits.filter { habit ->
        habit.daysOfWeek.any { kotlinDay ->
            kotlinDay.name == dayOfWeek.name
        }
    }
}

fun completionStats(habits: List<Habit>, date: LocalDate): Pair<Int, Int> {
    val dayHabits = habitsForDate(habits, date)
    if (dayHabits.isEmpty()) return 0 to 0
    val completed = dayHabits.count { isHabitCompletedOnDate(it, date) }
    return completed to dayHabits.size
}

fun completionPercent(completed: Int, total: Int): Int =
    if (total == 0) 0 else ((completed.toFloat() / total) * 100).toInt()

@Composable
fun userAvatarInitials(displayName: String): String {
    val parts = displayName.trim().split(" ").filter { it.isNotBlank() }
    val fallback = stringResource(R.string.user_avatar_initials_fallback)
    return when {
        parts.isEmpty() -> fallback
        parts.size == 1 -> parts.first().take(2).uppercase()
        else -> "${parts.first().first()}${parts.last().first()}".uppercase()
    }
}

@Composable
fun firstName(displayName: String): String {
    val fallback = stringResource(R.string.user_fallback_name)
    return displayName.trim().split(" ").firstOrNull().orEmpty().ifBlank { fallback }
}
