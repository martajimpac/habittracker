package com.marta.habittracker.domain.reminder

import com.marta.habittracker.domain.model.Habit
import com.marta.habittracker.domain.model.HabitRecord
import java.time.Instant
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyCheckInTest {

    private val friday = LocalDate(2026, 10, 9)

    @Test
    fun `notifies when a habit due today is still open`() {
        val habit = habit(days = DayOfWeek.entries.toSet())

        assertTrue(shouldSendDailyCheckIn(listOf(habit), friday))
    }

    @Test
    fun `stays quiet when every due habit is already done`() {
        val habit = habit(
            days = DayOfWeek.entries.toSet(),
            records = listOf(HabitRecord("r", "h", friday, true)),
        )

        assertFalse(shouldSendDailyCheckIn(listOf(habit), friday))
    }

    @Test
    fun `ignores a weekday habit on a day it is not scheduled`() {
        val habit = habit(
            days = setOf(DayOfWeek.MONDAY),
            scheduleMode = "weekdays",
        )

        assertFalse(shouldSendDailyCheckIn(listOf(habit), friday))
    }

    private fun habit(
        days: Set<DayOfWeek>,
        scheduleMode: String = "daily",
        records: List<HabitRecord> = emptyList(),
    ) = Habit(
        id = "h",
        name = "Read",
        description = null,
        daysOfWeek = days,
        icon = "book",
        colorHex = "#6750A4",
        reminderTime = null,
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        records = records,
        scheduleMode = scheduleMode,
    )
}
