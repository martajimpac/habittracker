package com.marta.habittracker.presentation.widgets.habit

import com.marta.habittracker.domain.model.Habit
import com.marta.habittracker.domain.model.HabitRecord
import java.time.Instant
import java.time.LocalDate
import kotlinx.datetime.DayOfWeek
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HabitWidgetStateTest {

    @Test
    fun `withTodayCompletion uses the record for the supplied day`() {
        val today = LocalDate.of(2026, 8, 7)
        val habit = sampleHabit(
            records = listOf(
                HabitRecord(
                    habitId = "habit-id",
                    date = kotlinx.datetime.LocalDate(2026, 8, 7),
                    isCompleted = true,
                ),
                HabitRecord(
                    habitId = "habit-id",
                    date = kotlinx.datetime.LocalDate(2026, 8, 6),
                    isCompleted = false,
                ),
            ),
        )

        assertTrue(habit.withTodayCompletion(today).isCompleted)
        assertFalse(habit.withTodayCompletion(today.plusDays(1)).isCompleted)
    }

    @Test
    fun `withTodayCompletion is false when there are no records`() {
        val habit = sampleHabit(records = emptyList())

        assertFalse(habit.withTodayCompletion(LocalDate.of(2026, 8, 7)).isCompleted)
    }

    @Test
    fun `withTodayCompletion is false when today record is incomplete`() {
        val habit = sampleHabit(
            records = listOf(
                HabitRecord(
                    habitId = "habit-id",
                    date = kotlinx.datetime.LocalDate(2026, 8, 7),
                    isCompleted = false,
                ),
            ),
        )

        assertFalse(habit.withTodayCompletion(LocalDate.of(2026, 8, 7)).isCompleted)
    }

    private fun sampleHabit(records: List<HabitRecord>) = Habit(
        id = "habit-id",
        name = "Read",
        description = null,
        daysOfWeek = setOf(DayOfWeek.FRIDAY),
        icon = "book",
        colorHex = "#6750A4",
        reminderTime = null,
        createdAt = Instant.EPOCH,
        records = records,
    )
}
