package com.marta.habittracker.domain.usecase

import com.marta.habittracker.domain.model.Habit
import com.marta.habittracker.domain.model.HabitRecord
import kotlinx.datetime.DayOfWeek as KotlinDayOfWeek
import kotlinx.datetime.LocalDate as KotlinLocalDate
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate

class BuildWeeklyHabitSummaryTest {

    @Test
    fun `empty habits returns seven days with zero counts`() {
        val anchor = LocalDate.of(2026, 8, 7) // Friday

        val summary = buildWeeklyHabitSummary(emptyList(), anchor)

        assertEquals(7, summary.size)
        summary.forEach { day ->
            assertEquals(0, day.scheduled)
            assertEquals(0, day.completed)
            assertEquals(0, day.percent)
        }
    }

    @Test
    fun `week is Mon through Sun containing anchor`() {
        val anchor = LocalDate.of(2026, 8, 5) // Wednesday

        val summary = buildWeeklyHabitSummary(emptyList(), anchor)

        assertEquals(LocalDate.of(2026, 8, 3), summary.first().date)
        assertEquals(DayOfWeek.MONDAY, summary.first().date.dayOfWeek)
        assertEquals(LocalDate.of(2026, 8, 9), summary.last().date)
        assertEquals(DayOfWeek.SUNDAY, summary.last().date.dayOfWeek)
    }

    @Test
    fun `scheduled counts habits for matching weekday only`() {
        val habits = listOf(
            sampleHabit(
                id = "weekday",
                days = setOf(KotlinDayOfWeek.MONDAY, KotlinDayOfWeek.WEDNESDAY, KotlinDayOfWeek.FRIDAY),
            ),
            sampleHabit(
                id = "weekend",
                days = setOf(KotlinDayOfWeek.SATURDAY),
            ),
        )
        val anchor = LocalDate.of(2026, 8, 7) // Friday in week Aug 3–9

        val summary = buildWeeklyHabitSummary(habits, anchor)

        assertEquals(1, summary.single { it.date == LocalDate.of(2026, 8, 3) }.scheduled) // Mon
        assertEquals(0, summary.single { it.date == LocalDate.of(2026, 8, 4) }.scheduled) // Tue
        assertEquals(1, summary.single { it.date == LocalDate.of(2026, 8, 5) }.scheduled) // Wed
        assertEquals(1, summary.single { it.date == LocalDate.of(2026, 8, 7) }.scheduled) // Fri
        assertEquals(1, summary.single { it.date == LocalDate.of(2026, 8, 8) }.scheduled) // Sat
        assertEquals(0, summary.single { it.date == LocalDate.of(2026, 8, 9) }.scheduled) // Sun
    }

    @Test
    fun `completed counts only scheduled habits with completed records`() {
        val habits = listOf(
            sampleHabit(
                id = "done",
                days = setOf(KotlinDayOfWeek.FRIDAY),
                records = listOf(
                    HabitRecord("1", "done", KotlinLocalDate(2026, 8, 7), true),
                ),
            ),
            sampleHabit(
                id = "missed",
                days = setOf(KotlinDayOfWeek.FRIDAY),
                records = listOf(
                    HabitRecord("2", "missed", KotlinLocalDate(2026, 8, 7), false),
                ),
            ),
            sampleHabit(
                id = "other-day",
                days = setOf(KotlinDayOfWeek.FRIDAY),
                records = listOf(
                    HabitRecord("3", "other-day", KotlinLocalDate(2026, 8, 6), true),
                ),
            ),
        )
        val anchor = LocalDate.of(2026, 8, 7)

        val friday = buildWeeklyHabitSummary(habits, anchor)
            .single { it.date == LocalDate.of(2026, 8, 7) }

        assertEquals(3, friday.scheduled)
        assertEquals(1, friday.completed)
        assertEquals(33, friday.percent)
    }

    @Test
    fun `weekend day reflects partial completion ratio`() {
        val habits = listOf(
            sampleHabit(
                id = "a",
                days = setOf(KotlinDayOfWeek.SATURDAY),
                records = listOf(
                    HabitRecord("1", "a", KotlinLocalDate(2026, 8, 8), true),
                ),
            ),
            sampleHabit(
                id = "b",
                days = setOf(KotlinDayOfWeek.SATURDAY),
                records = emptyList(),
            ),
        )
        val anchor = LocalDate.of(2026, 8, 8) // Saturday

        val saturday = buildWeeklyHabitSummary(habits, anchor)
            .single { it.date == LocalDate.of(2026, 8, 8) }

        assertEquals(2, saturday.scheduled)
        assertEquals(1, saturday.completed)
        assertEquals(50, saturday.percent)
    }

    private fun sampleHabit(
        id: String = "h1",
        days: Set<KotlinDayOfWeek> = setOf(KotlinDayOfWeek.MONDAY),
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
