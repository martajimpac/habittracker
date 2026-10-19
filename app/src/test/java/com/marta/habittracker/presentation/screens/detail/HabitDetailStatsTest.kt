package com.marta.habittracker.presentation.screens.detail

import com.marta.habittracker.core.toKotlin
import com.marta.habittracker.domain.model.HabitRecord
import kotlinx.datetime.DayOfWeek
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class HabitDetailStatsTest {

    @Test
    fun `completion percent is the share of completed records`() {
        assertEquals(0f, completionPercent(emptyList()))
        assertEquals(25f, completionPercent(records(completed = 1, missed = 3)))
        assertEquals(100f, completionPercent(records(completed = 2, missed = 0)))
    }

    @Test
    fun `current week ignores days after today`() {
        val today = LocalDate.parse("2026-10-19")
        val weeks = recentWeekCompletions(
            daysOfWeek = DayOfWeek.entries.toSet(),
            records = listOf(record("2026-10-19", completed = true)),
            createdOn = LocalDate.parse("2026-01-01"),
            today = today,
        )

        assertEquals(4, weeks.size)
        assertEquals(LocalDate.parse("2026-09-28"), weeks[0].weekStart)
        assertEquals(0, weeks[0].completed)
        assertEquals(7, weeks[0].scheduled)
        assertEquals(0, weeks[2].completed)
        assertEquals(7, weeks[2].scheduled)
        assertEquals(LocalDate.parse("2026-10-19"), weeks[3].weekStart)
        assertEquals(1, weeks[3].completed)
        assertEquals(1, weeks[3].scheduled)
    }

    @Test
    fun `only scheduled days count`() {
        val weeks = recentWeekCompletions(
            daysOfWeek = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY),
            records = listOf(
                record("2026-10-19", completed = true),
                record("2026-10-20", completed = true),
            ),
            createdOn = LocalDate.parse("2026-01-01"),
            today = LocalDate.parse("2026-10-21"),
        )

        val current = weeks.last()
        assertEquals(1, current.completed)
        assertEquals(2, current.scheduled)
    }

    @Test
    fun `days before the habit existed are not scheduled`() {
        val weeks = recentWeekCompletions(
            daysOfWeek = DayOfWeek.entries.toSet(),
            records = emptyList(),
            createdOn = LocalDate.parse("2026-10-21"),
            today = LocalDate.parse("2026-10-25"),
        )

        assertEquals(0, weeks[2].scheduled)
        assertEquals(LocalDate.parse("2026-10-19"), weeks[3].weekStart)
        assertEquals(5, weeks[3].scheduled)
        assertEquals(0, weeks[3].completed)
    }

    private fun records(completed: Int, missed: Int): List<HabitRecord> {
        val done = List(completed) { index -> record("2026-01-%02d".format(index + 1), completed = true) }
        val open = List(missed) { index -> record("2026-02-%02d".format(index + 1), completed = false) }
        return done + open
    }

    private fun record(date: String, completed: Boolean) = HabitRecord(
        habitId = "habit-1",
        date = LocalDate.parse(date).toKotlin(),
        isCompleted = completed,
    )
}
