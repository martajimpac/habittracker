package com.marta.habittracker.domain.usecase

import com.marta.habittracker.domain.model.Habit
import com.marta.habittracker.domain.model.HabitRecord
import kotlinx.datetime.DayOfWeek as KotlinDayOfWeek
import kotlinx.datetime.LocalDate as KotlinLocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate

class HabitStatsInsightTest {

    private val today = LocalDate.of(2026, 8, 5) // Wednesday

    @Test
    fun `strongest weekday is the one with the most completed records and monday wins a tie`() {
        val habits = listOf(
            sampleHabit(
                id = "mon",
                records = listOf(
                    record("1", "mon", KotlinLocalDate(2026, 8, 3), true),
                    record("2", "mon", KotlinLocalDate(2026, 7, 27), true),
                ),
            ),
            sampleHabit(
                id = "wed",
                records = listOf(
                    record("3", "wed", KotlinLocalDate(2026, 8, 5), true),
                    record("4", "wed", KotlinLocalDate(2026, 7, 29), true),
                    record("5", "wed", KotlinLocalDate(2026, 8, 5), false),
                ),
            ),
        )

        val insight = habitStatsInsight(habits, today)

        assertEquals(DayOfWeek.MONDAY, insight.strongestWeekday)
        assertEquals(2, insight.strongestWeekdayCount)
    }

    @Test
    fun `no completed records leaves the strongest weekday empty`() {
        val insight = habitStatsInsight(
            listOf(sampleHabit(id = "none", records = listOf(record("1", "none", KotlinLocalDate(2026, 8, 5), false)))),
            today,
        )

        assertNull(insight.strongestWeekday)
        assertEquals(0, insight.strongestWeekdayCount)
    }

    @Test
    fun `the habit that breaks tomorrow is the longest streak still alive today`() {
        val habits = listOf(
            sampleHabit(
                id = "short",
                name = "Short",
                records = listOf(record("1", "short", KotlinLocalDate(2026, 8, 5), true)),
            ),
            sampleHabit(
                id = "long",
                name = "Long",
                records = listOf(
                    record("2", "long", KotlinLocalDate(2026, 8, 5), true),
                    record("3", "long", KotlinLocalDate(2026, 8, 4), true),
                ),
            ),
            sampleHabit(
                id = "already-broken",
                name = "Broken",
                records = listOf(
                    record("4", "already-broken", KotlinLocalDate(2026, 8, 4), true),
                    record("5", "already-broken", KotlinLocalDate(2026, 8, 3), true),
                ),
            ),
        )

        val insight = habitStatsInsight(habits, today)

        assertEquals("long", insight.streakAtRisk?.habitId)
        assertEquals("Long", insight.streakAtRisk?.habitName)
        assertEquals(2, insight.streakAtRisk?.streakDays)
    }

    private fun sampleHabit(
        id: String,
        name: String = "Test",
        records: List<HabitRecord> = emptyList(),
    ) = Habit(
        id = id,
        name = name,
        description = null,
        daysOfWeek = setOf(KotlinDayOfWeek.WEDNESDAY),
        icon = "water_drop",
        colorHex = "#6750A4",
        reminderTime = null,
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        records = records,
    )

    private fun record(id: String, habitId: String, date: KotlinLocalDate, completed: Boolean) =
        HabitRecord(id = id, habitId = habitId, date = date, isCompleted = completed)
}
