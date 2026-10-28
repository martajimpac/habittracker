package com.marta.habittracker.domain.usecase

import com.marta.habittracker.domain.model.Habit
import com.marta.habittracker.domain.model.HabitRecord
import java.time.Instant
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import org.junit.Assert.assertEquals
import org.junit.Test

class HabitWeekRecommendationStatsTest {

    private val wednesday = LocalDate(2026, 10, 28)

    @Test
    fun `counts completed days in the current week against the weekly target`() {
        val monday = LocalDate(2026, 10, 26)
        val habit = habit(
            name = "Read",
            daysOfWeek = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            records = listOf(
                record("a", monday, completed = true),
                record("b", monday.plus(DatePeriod(days = 2)), completed = true),
                record("c", monday.plus(DatePeriod(days = -7)), completed = true),
                record("d", monday.plus(DatePeriod(days = 1)), completed = false),
            ),
        )

        val stats = habitWeekRecommendationStats(listOf(habit), wednesday)

        assertEquals(
            listOf(HabitWeekStat(name = "Read", completedDays = 2, totalDays = 3)),
            stats,
        )
    }

    @Test
    fun `omits a blank name and does not cap completed days`() {
        val monday = LocalDate(2026, 10, 26)
        val overTarget = habit(
            name = "  Walk  ",
            daysPerWeek = 2,
            records = (0..3).map { offset ->
                record("w$offset", monday.plus(DatePeriod(days = offset)), completed = true)
            },
        )
        val blank = habit(name = "   ")

        val stats = habitWeekRecommendationStats(listOf(blank, overTarget), wednesday)

        assertEquals(
            listOf(HabitWeekStat(name = "Walk", completedDays = 4, totalDays = 2)),
            stats,
        )
    }
}

private fun habit(
    name: String,
    daysOfWeek: Set<DayOfWeek> = emptySet(),
    daysPerWeek: Int = 7,
    records: List<HabitRecord> = emptyList(),
) = Habit(
    id = name,
    name = name,
    description = null,
    daysOfWeek = daysOfWeek,
    icon = "book",
    colorHex = "#6750A4",
    reminderTime = null,
    createdAt = Instant.parse("2026-01-01T00:00:00Z"),
    records = records,
    daysPerWeek = daysPerWeek,
)

private fun record(id: String, date: LocalDate, completed: Boolean) = HabitRecord(
    id = id,
    habitId = "h",
    date = date,
    isCompleted = completed,
)
