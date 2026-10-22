package com.marta.habittracker.domain.usecase

import com.marta.habittracker.domain.model.Habit
import com.marta.habittracker.domain.model.HabitRecord
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class WeeklyQuotaTest {

    private val today = LocalDate(2026, 8, 5) // Wednesday

    @Test
    fun `old weekday set becomes the weekly target`() {
        val habit = habit(days = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY))

        assertEquals(3, habit.weeklyTarget())
    }

    @Test
    fun `explicit count wins over a full week of days`() {
        val habit = habit(days = DayOfWeek.entries.toSet(), daysPerWeek = 2)

        assertEquals(2, habit.weeklyTarget())
    }

    @Test
    fun `habit stays visible until the week reaches its count`() {
        val habit = habit(
            days = DayOfWeek.entries.toSet(),
            daysPerWeek = 1,
            records = listOf(record(today.minus(DatePeriod(days = 2)))),
        )

        assertTrue(habit.shownOn(today.minus(DatePeriod(days = 2))))
        assertFalse(habit.shownOn(today))
    }

    @Test
    fun `current week still open does not break earlier weeks`() {
        val lastWeek = today.minus(DatePeriod(days = 7))
        val streak = calculateWeeklyQuotaStreak(
            target = 1,
            records = listOf(record(lastWeek)),
            today = today,
            createdOn = LocalDate(2026, 1, 1),
        )

        assertEquals(1, streak)
    }

    @Test
    fun `a finished week counts and a missed week stops the run`() {
        val thisMonday = LocalDate(2026, 8, 3)
        val previousMonday = thisMonday.minus(DatePeriod(days = 7))
        val olderMonday = previousMonday.minus(DatePeriod(days = 7))
        val streak = calculateWeeklyQuotaStreak(
            target = 1,
            records = listOf(record(thisMonday), record(olderMonday)),
            today = today,
            createdOn = LocalDate(2026, 1, 1),
        )

        assertEquals(1, streak)
    }

    private fun record(date: LocalDate) = HabitRecord(
        id = date.toString(),
        habitId = "h",
        date = date,
        isCompleted = true,
    )

    private fun habit(
        days: Set<DayOfWeek>,
        daysPerWeek: Int = 7,
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
        daysPerWeek = daysPerWeek,
    )
}
