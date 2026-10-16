package com.marta.habittracker.presentation.screens.goals

import com.marta.habittracker.domain.model.Habit
import com.marta.habittracker.domain.model.HabitRecord
import com.marta.habittracker.domain.usecase.FakeHabitRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate as KotlinLocalDate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class GoalsViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `shows completed days for the current week`() = runTest {
        val habit = Habit(
            id = "done",
            name = "Water",
            description = null,
            daysOfWeek = setOf(DayOfWeek.MONDAY),
            icon = "water_drop",
            colorHex = "#6750A4",
            reminderTime = null,
            createdAt = Instant.parse("2026-01-01T00:00:00Z"),
            records = listOf(HabitRecord("1", "done", KotlinLocalDate(2026, 8, 3), true)),
        )
        val viewModel = GoalsViewModel(
            habitRepository = FakeHabitRepository(allHabitsWithRecords = listOf(habit)),
            today = { LocalDate.of(2026, 8, 5) },
        )
        val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.completedDays)
        assertEquals(5, viewModel.uiState.value.targetDays)
        collectJob.cancel()
    }
}
