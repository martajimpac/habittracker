package com.marta.habittracker.presentation.screens.add_content

import com.marta.habittracker.R
import com.marta.habittracker.domain.usecase.FakeHabitRepository
import com.marta.habittracker.domain.usecase.SaveHabit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.DayOfWeek

@OptIn(ExperimentalCoroutinesApi::class)
class AddContentViewModelTest {

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
    fun `save with blank name sets validation error`() = runTest {
        val viewModel = createViewModel()

        viewModel.onDayToggled(DayOfWeek.MONDAY)
        viewModel.onSaveClicked()
        advanceUntilIdle()

        assertEquals(R.string.add_habit_error_name_required, viewModel.uiState.value.errorMessageRes)
        assertFalse(viewModel.uiState.value.isSaving)
    }

    @Test
    fun `save without days sets validation error`() = runTest {
        val viewModel = createViewModel()

        viewModel.onNameChanged("Morning Run")
        viewModel.onSaveClicked()
        advanceUntilIdle()

        assertEquals(R.string.add_habit_error_days_required, viewModel.uiState.value.errorMessageRes)
    }

    @Test
    fun `save with valid data persists visuals and emits saved`() = runTest {
        val repository = FakeHabitRepository()
        val viewModel = AddContentViewModel(SaveHabit(repository))
        var saved = false
        val collectJob = launch {
            viewModel.habitSaved.collect { saved = true }
        }

        viewModel.onNameChanged("Morning Run")
        viewModel.onDescriptionChanged("Park loop")
        viewModel.onIconSelected("directions_run")
        viewModel.onColorSelected("#059669")
        viewModel.onReminderTimeChanged("07:30")
        viewModel.onDayToggled(DayOfWeek.MONDAY)
        viewModel.onDayToggled(DayOfWeek.WEDNESDAY)
        viewModel.onSaveClicked()
        advanceUntilIdle()

        val inserted = repository.insertedHabits.single()
        assertEquals("Morning Run", inserted.name)
        assertEquals("Park loop", inserted.description)
        assertEquals("directions_run", inserted.icon)
        assertEquals("#059669", inserted.colorHex)
        assertEquals("07:30", inserted.reminderTime)
        assertTrue(inserted.id.isNotBlank())
        assertTrue(saved)
        assertNull(viewModel.uiState.value.errorMessageRes)
        collectJob.cancel()
    }

    private fun createViewModel(): AddContentViewModel =
        AddContentViewModel(SaveHabit(FakeHabitRepository()))
}
