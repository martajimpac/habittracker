package com.marta.habittracker.presentation.widgets.habit

import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.model.AppError
import com.marta.habittracker.domain.model.Habit
import com.marta.habittracker.domain.model.HabitRecord
import com.marta.habittracker.domain.usecase.FakeHabitRepository
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DayOfWeek
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ToggleHabitActionTest {

    private val today = LocalDate.of(2026, 8, 7)

    @Test
    fun `ignores toggle when configured habit id is null`() = runTest {
        val repository = FakeHabitRepository()

        val outcome = performHabitWidgetToggle(
            configuredHabitId = null,
            habits = listOf(sampleHabit()),
            today = today,
            toggle = repository::toggleHabitCompletion,
        )

        assertEquals(HabitWidgetToggleOutcome.IgnoredMissingHabit, outcome)
        assertTrue(repository.toggledHabits.isEmpty())
    }

    @Test
    fun `ignores toggle when configured habit is missing from list`() = runTest {
        val repository = FakeHabitRepository()

        val outcome = performHabitWidgetToggle(
            configuredHabitId = "missing",
            habits = listOf(sampleHabit(id = "other")),
            today = today,
            toggle = repository::toggleHabitCompletion,
        )

        assertEquals(HabitWidgetToggleOutcome.IgnoredMissingHabit, outcome)
        assertTrue(repository.toggledHabits.isEmpty())
    }

    @Test
    fun `success outcome requests refresh and passes today completion flag`() = runTest {
        val habit = sampleHabit(
            records = listOf(
                HabitRecord(
                    habitId = "habit-id",
                    date = kotlinx.datetime.LocalDate(2026, 8, 7),
                    isCompleted = true,
                ),
            ),
        )
        val repository = FakeHabitRepository()

        val outcome = performHabitWidgetToggle(
            configuredHabitId = "habit-id",
            habits = listOf(habit),
            today = today,
            toggle = repository::toggleHabitCompletion,
        )

        assertEquals(HabitWidgetToggleOutcome.ToggledAndShouldRefresh, outcome)
        val (toggled, date) = repository.toggledHabits.single()
        assertEquals(today, date)
        assertTrue(toggled.isCompleted)
    }

    @Test
    fun `network error outcome does not request refresh`() = runTest {
        val repository = FakeHabitRepository().apply {
            toggleResult = DataResult.Error(AppError.Common.Network)
        }

        val outcome = performHabitWidgetToggle(
            configuredHabitId = "habit-id",
            habits = listOf(sampleHabit()),
            today = today,
            toggle = repository::toggleHabitCompletion,
        )

        assertEquals(HabitWidgetToggleOutcome.ToggleFailed, outcome)
        assertEquals(1, repository.toggledHabits.size)
        assertFalse(repository.toggledHabits.single().first.isCompleted)
    }

    private fun sampleHabit(
        id: String = "habit-id",
        records: List<HabitRecord> = emptyList(),
    ) = Habit(
        id = id,
        name = "Read",
        description = null,
        daysOfWeek = setOf(DayOfWeek.FRIDAY),
        icon = "book",
        colorHex = "#6750A4",
        reminderTime = null,
        createdAt = Instant.EPOCH,
        records = records,
    )
}
