package com.marta.habittracker.domain.usecase

import com.marta.habittracker.domain.model.HabitRecord
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class ScheduledStreakTest {

    @Test
    fun `two weeks of monday and tuesday count as four completed days`() {
        val records = listOf(
            completed(LocalDate(2026, 10, 5)),
            completed(LocalDate(2026, 10, 6)),
            completed(LocalDate(2026, 10, 12)),
            completed(LocalDate(2026, 10, 13)),
            completed(LocalDate(2026, 10, 14)),
        )

        val streak = calculateScheduledStreak(
            daysOfWeek = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY),
            records = records,
            today = LocalDate(2026, 10, 14),
            createdOn = LocalDate(2026, 10, 1),
        )

        assertEquals(4, streak)
    }

    @Test
    fun `a missed scheduled day ends the streak and unscheduled days do not count`() {
        val records = listOf(
            completed(LocalDate(2026, 10, 5)),
            completed(LocalDate(2026, 10, 6)),
            completed(LocalDate(2026, 10, 12)),
            completed(LocalDate(2026, 10, 14)),
        )

        val streak = calculateScheduledStreak(
            daysOfWeek = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY),
            records = records,
            today = LocalDate(2026, 10, 14),
            createdOn = LocalDate(2026, 10, 1),
        )

        assertEquals(0, streak)
    }

    @Test
    fun `today not completed breaks a daily streak`() {
        val records = listOf(
            completed(LocalDate(2026, 10, 12)),
            completed(LocalDate(2026, 10, 13)),
        )

        val streak = calculateScheduledStreak(
            daysOfWeek = DayOfWeek.entries.toSet(),
            records = records,
            today = LocalDate(2026, 10, 14),
            createdOn = LocalDate(2026, 10, 1),
        )

        assertEquals(0, streak)
    }

    private fun completed(date: LocalDate) = HabitRecord(
        id = date.toString(),
        habitId = "run",
        date = date,
        isCompleted = true,
    )
}
