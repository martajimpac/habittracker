package com.marta.habittracker.presentation.components

import com.marta.habittracker.domain.model.Habit
import com.marta.habittracker.domain.model.HabitRecord
import kotlinx.datetime.DayOfWeek as KotlinDayOfWeek
import kotlinx.datetime.LocalDate as KotlinLocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate

class ActivityHeatmapHelpersTest {

    @Test
    fun `buildRollingFourWeeks returns 28 days ending on current week Sunday`() {
        val today = LocalDate.of(2026, 7, 30) // Thursday
        val dates = buildRollingFourWeeks(today)

        assertEquals(28, dates.size)
        assertEquals(DayOfWeek.MONDAY, dates.first().dayOfWeek)
        assertEquals(DayOfWeek.SUNDAY, dates.last().dayOfWeek)
        assertTrue(dates.contains(today))
        assertEquals(LocalDate.of(2026, 7, 6), dates.first()) // Mon 3 weeks before current Mon (Jul 27)
    }

    @Test
    fun `habitDayIntensity is binary`() {
        val habit = sampleHabit(
            records = listOf(
                HabitRecord(
                    id = "1",
                    habitId = "h1",
                    date = KotlinLocalDate(2026, 7, 30),
                    isCompleted = true,
                ),
            ),
        )
        assertEquals(1f, habitDayIntensity(habit, LocalDate.of(2026, 7, 30)))
        assertEquals(0f, habitDayIntensity(habit, LocalDate.of(2026, 7, 29)))
    }

    @Test
    fun `globalDayIntensity uses scheduled habits completion ratio`() {
        val day = LocalDate.of(2026, 7, 30) // Thursday
        val habits = listOf(
            sampleHabit(
                id = "a",
                days = setOf(KotlinDayOfWeek.THURSDAY),
                records = listOf(
                    HabitRecord("1", "a", KotlinLocalDate(2026, 7, 30), true),
                ),
            ),
            sampleHabit(
                id = "b",
                days = setOf(KotlinDayOfWeek.THURSDAY),
                records = emptyList(),
            ),
        )
        assertEquals(0.5f, globalDayIntensity(habits, day))
    }

    private fun sampleHabit(
        id: String = "h1",
        days: Set<KotlinDayOfWeek> = setOf(KotlinDayOfWeek.THURSDAY),
        records: List<HabitRecord> = emptyList(),
    ) = Habit(
        id = id,
        name = "Test",
        description = null,
        daysOfWeek = days,
        icon = "water_drop",
        colorHex = "#6750A4",
        reminderTime = null,
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        records = records,
    )
}
