package com.marta.habittracker.domain.usecase

import com.marta.habittracker.domain.model.Habit
import com.marta.habittracker.domain.model.HabitRecord
import kotlinx.datetime.DayOfWeek as KotlinDayOfWeek
import kotlinx.datetime.LocalDate as KotlinLocalDate
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class WeeklyGoalTest {

    private val anchor = LocalDate.of(2026, 8, 5) // Wednesday, week Aug 3–9

    @Test
    fun `target is five days and an empty week counts zero`() {
        val progress = weeklyGoalProgress(emptyList(), anchor)

        assertEquals(5, progress.targetDays)
        assertEquals(0, progress.completedDays)
    }

    @Test
    fun `a day counts only when every scheduled habit is completed`() {
        val habits = listOf(
            sampleHabit(
                id = "done",
                days = setOf(KotlinDayOfWeek.MONDAY),
                records = listOf(HabitRecord("1", "done", KotlinLocalDate(2026, 8, 3), true)),
            ),
            sampleHabit(
                id = "missed",
                days = setOf(KotlinDayOfWeek.WEDNESDAY),
                records = listOf(HabitRecord("2", "missed", KotlinLocalDate(2026, 8, 5), false)),
            ),
        )

        val progress = weeklyGoalProgress(habits, anchor)

        assertEquals(1, progress.completedDays)
    }

    @Test
    fun `a partial day and a day with no habits do not count`() {
        val habits = listOf(
            sampleHabit(
                id = "a",
                days = setOf(KotlinDayOfWeek.MONDAY),
                records = listOf(HabitRecord("1", "a", KotlinLocalDate(2026, 8, 3), true)),
            ),
            sampleHabit(
                id = "b",
                days = setOf(KotlinDayOfWeek.MONDAY),
            ),
            sampleHabit(
                id = "friday",
                days = setOf(KotlinDayOfWeek.FRIDAY),
                records = listOf(HabitRecord("3", "friday", KotlinLocalDate(2026, 8, 7), true)),
            ),
        )

        val progress = weeklyGoalProgress(habits, anchor)

        assertEquals(1, progress.completedDays)
    }

    private fun sampleHabit(
        id: String,
        days: Set<KotlinDayOfWeek>,
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
