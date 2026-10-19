package com.marta.habittracker.domain.usecase

import com.marta.habittracker.domain.model.Habit
import com.marta.habittracker.domain.model.HabitRecord
import kotlinx.datetime.DayOfWeek as KotlinDayOfWeek
import kotlinx.datetime.LocalDate as KotlinLocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class HabitStatsInsightTest {

    private val today = LocalDate.of(2026, 8, 5) // Wednesday

    @Test
    fun `all habits card appears at the minimum and hides one day below it`() {
        val minimum = StatsStreakMinimums.ALL_HABITS_DAYS

        assertNull(statsStreakHighlight(listOf(dailyHabit("short", perfectDays("short", minimum - 1))), today))

        val atMinimum = statsStreakHighlight(
            listOf(dailyHabit("ready", perfectDays("ready", minimum))),
            today,
        )

        assertEquals(StatsStreakHighlight.AllHabits(days = minimum, isLongestSoFar = true), atMinimum)
    }

    @Test
    fun `all habits card wins over a longer single habit`() {
        val habits = listOf(
            dailyHabit("steady", perfectDays("steady", StatsStreakMinimums.ALL_HABITS_DAYS + 4)),
            dailyHabit("new", perfectDays("new", StatsStreakMinimums.ALL_HABITS_DAYS)),
        )

        val highlight = statsStreakHighlight(habits, today)

        assertEquals(
            StatsStreakHighlight.AllHabits(
                days = StatsStreakMinimums.ALL_HABITS_DAYS,
                isLongestSoFar = true,
            ),
            highlight,
        )
    }

    @Test
    fun `a broken perfect streak is not described as the longest`() {
        val habit = dailyHabit(
            "gap",
            perfectDays("gap", StatsStreakMinimums.ALL_HABITS_DAYS) +
                missedDay("gap", StatsStreakMinimums.ALL_HABITS_DAYS) +
                perfectDays(
                    "gap-old",
                    StatsStreakMinimums.ALL_HABITS_DAYS + 2,
                    startDaysAgo = StatsStreakMinimums.ALL_HABITS_DAYS + 1,
                ),
        )

        val highlight = statsStreakHighlight(listOf(habit), today)

        assertEquals(
            StatsStreakHighlight.AllHabits(
                days = StatsStreakMinimums.ALL_HABITS_DAYS,
                isLongestSoFar = false,
            ),
            highlight,
        )
    }

    @Test
    fun `without an all habits streak the longest habit at the minimum is highlighted`() {
        val minimum = StatsStreakMinimums.SINGLE_HABIT_DAYS
        val habits = listOf(
            dailyHabit("short", perfectDays("short", minimum - 1)),
            dailyHabit("long", perfectDays("long", minimum)),
            dailyHabit(
                "broken",
                perfectDays("broken", minimum + 2, startDaysAgo = 1),
            ),
        )

        val highlight = statsStreakHighlight(habits, today)

        assertEquals(
            StatsStreakHighlight.Habit(habitId = "long", habitName = "Long", days = minimum),
            highlight,
        )
    }

    @Test
    fun `equal habit streaks keep the later name`() {
        val days = StatsStreakMinimums.SINGLE_HABIT_DAYS
        val habits = listOf(
            dailyHabit("blocker", emptyList(), name = "Blocker"),
            dailyHabit("a", perfectDays("a", days), name = "Alpha"),
            dailyHabit("b", perfectDays("b", days), name = "Zulu"),
        )

        assertEquals("b", (statsStreakHighlight(habits, today) as StatsStreakHighlight.Habit).habitId)
    }

    @Test
    fun `unscheduled days do not break the all habits streak`() {
        val weeks = StatsStreakMinimums.ALL_HABITS_DAYS
        val habit = sampleHabit(
            id = "wed",
            name = "Wed",
            days = setOf(KotlinDayOfWeek.WEDNESDAY),
            records = (0 until weeks).flatMap { week ->
                perfectDays("wed", 1, startDaysAgo = week * 7)
            },
        )

        val highlight = statsStreakHighlight(listOf(habit), today)

        assertEquals(StatsStreakHighlight.AllHabits(days = weeks, isLongestSoFar = true), highlight)
    }

    @Test
    fun `nothing is highlighted below both minimums`() {
        val belowBoth = minOf(
            StatsStreakMinimums.ALL_HABITS_DAYS,
            StatsStreakMinimums.SINGLE_HABIT_DAYS,
        ) - 1
        val habits = listOf(dailyHabit("a", perfectDays("a", belowBoth)))

        assertNull(statsStreakHighlight(habits, today))
    }

    @Test
    fun `copy variant stays in range and changes with the day`() {
        val count = 3
        val first = statsStreakCopyVariant(streakDays = 5, dayOfYear = 10, variantCount = count)
        val nextDay = statsStreakCopyVariant(streakDays = 5, dayOfYear = 11, variantCount = count)

        assertTrue(first in 0 until count)
        assertEquals((first + 1) % count, nextDay)
        assertEquals(0, statsStreakCopyVariant(1, 1, variantCount = 1))
    }

    private fun dailyHabit(
        id: String,
        records: List<HabitRecord>,
        name: String = id.replaceFirstChar { it.titlecase() },
    ) = sampleHabit(
        id = id,
        name = name,
        days = KotlinDayOfWeek.entries.toSet(),
        records = records,
    )

    private fun perfectDays(habitId: String, count: Int, startDaysAgo: Int = 0): List<HabitRecord> =
        (0 until count).map { offset ->
            val daysAgo = startDaysAgo + offset
            record(
                id = "$habitId-$daysAgo",
                habitId = habitId,
                date = KotlinLocalDate.parse(today.minusDays(daysAgo.toLong()).toString()),
                completed = true,
            )
        }

    private fun missedDay(habitId: String, daysAgo: Int) = listOf(
        record(
            id = "$habitId-miss-$daysAgo",
            habitId = habitId,
            date = KotlinLocalDate.parse(today.minusDays(daysAgo.toLong()).toString()),
            completed = false,
        ),
    )

    private fun sampleHabit(
        id: String,
        name: String,
        days: Set<KotlinDayOfWeek>,
        records: List<HabitRecord>,
    ) = Habit(
        id = id,
        name = name,
        description = null,
        daysOfWeek = days,
        icon = "water_drop",
        colorHex = "#6750A4",
        reminderTime = null,
        createdAt = Instant.parse("2026-01-01T12:00:00Z"),
        records = records,
    )

    private fun record(id: String, habitId: String, date: KotlinLocalDate, completed: Boolean) =
        HabitRecord(id = id, habitId = habitId, date = date, isCompleted = completed)
}
