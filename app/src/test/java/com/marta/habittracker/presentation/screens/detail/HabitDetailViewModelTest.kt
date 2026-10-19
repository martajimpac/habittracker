package com.marta.habittracker.presentation.screens.detail

import androidx.lifecycle.SavedStateHandle
import com.marta.habittracker.R
import com.marta.habittracker.domain.model.Habit
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
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class HabitDetailViewModelTest {

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
    fun `load with reminderTime 08 00 enables reminder`() = runTest {
        val viewModel = createViewModel(sampleHabit(reminderTime = "08:00"))
        advanceUntilIdle()

        assertTrue(viewModel.reminderEnabled.value)
        assertEquals("08:00", viewModel.reminderTime.value)
    }

    @Test
    fun `load with null reminderTime disables reminder and drafts 08 00`() = runTest {
        val viewModel = createViewModel(sampleHabit(reminderTime = null))
        advanceUntilIdle()

        assertFalse(viewModel.reminderEnabled.value)
        assertEquals("08:00", viewModel.reminderTime.value)
    }

    @Test
    fun `save with reminder disabled updates habit reminderTime to null`() = runTest {
        val habit = sampleHabit(reminderTime = "08:00")
        val repository = FakeHabitRepository(habitsById = mapOf(habit.id to habit))
        val viewModel = HabitDetailViewModel(
            repository = repository,
            savedStateHandle = SavedStateHandle(mapOf("habitId" to habit.id)),
        )
        advanceUntilIdle()

        viewModel.onReminderEnabled(false)
        viewModel.saveReminder()
        advanceUntilIdle()

        assertNull(repository.updatedHabits.single().reminderTime)
        assertEquals(habit.id, repository.updatedHabits.single().id)
    }

    @Test
    fun `save with reminder enabled persists 07 30`() = runTest {
        val habit = sampleHabit(reminderTime = null)
        val repository = FakeHabitRepository(habitsById = mapOf(habit.id to habit))
        val viewModel = HabitDetailViewModel(
            repository = repository,
            savedStateHandle = SavedStateHandle(mapOf("habitId" to habit.id)),
        )
        advanceUntilIdle()

        viewModel.onReminderEnabled(true)
        viewModel.onReminderTimeChanged("07:30")
        viewModel.saveReminder()
        advanceUntilIdle()

        assertEquals("07:30", repository.updatedHabits.single().reminderTime)
    }

    @Test
    fun `deleteHabit emits habitDeleted`() = runTest {
        val habit = sampleHabit(reminderTime = null)
        val repository = FakeHabitRepository(habitsById = mapOf(habit.id to habit))
        val viewModel = HabitDetailViewModel(
            repository = repository,
            savedStateHandle = SavedStateHandle(mapOf("habitId" to habit.id)),
        )
        advanceUntilIdle()
        var deleted = false
        val collectJob = launch {
            viewModel.habitDeleted.collect { deleted = true }
        }

        viewModel.deleteHabit()
        advanceUntilIdle()

        assertTrue(deleted)
        collectJob.cancel()
    }

    @Test
    fun `reminder denied turns toggle off and sets actionErrorRes`() = runTest {
        val viewModel = createViewModel(sampleHabit(reminderTime = "08:00"))
        advanceUntilIdle()

        viewModel.onReminderEnabled(true)
        viewModel.onReminderDenied(R.string.habit_reminder_permission_denied)

        assertFalse(viewModel.reminderEnabled.value)
        assertEquals(
            R.string.habit_reminder_permission_denied,
            viewModel.actionErrorRes.value,
        )
    }

    private fun createViewModel(habit: Habit): HabitDetailViewModel {
        val repository = FakeHabitRepository(habitsById = mapOf(habit.id to habit))
        return HabitDetailViewModel(
            repository = repository,
            savedStateHandle = SavedStateHandle(mapOf("habitId" to habit.id)),
        )
    }

    private fun sampleHabit(reminderTime: String?) = Habit(
        id = "habit-1",
        name = "Read",
        description = null,
        daysOfWeek = setOf(DayOfWeek.MONDAY),
        icon = "book",
        colorHex = "#6750A4",
        reminderTime = reminderTime,
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        records = emptyList(),
    )
}
