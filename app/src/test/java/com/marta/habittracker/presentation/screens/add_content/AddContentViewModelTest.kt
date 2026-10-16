package com.marta.habittracker.presentation.screens.add_content

import androidx.compose.ui.graphics.Color
import com.marta.habittracker.R
import com.marta.habittracker.domain.usecase.FakeHabitRepository
import com.marta.habittracker.domain.usecase.SaveHabit
import com.marta.habittracker.presentation.theme.HabitColor1
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
        viewModel.onColorSelected(HabitColor1)
        viewModel.onDayToggled(DayOfWeek.MONDAY)
        viewModel.onDayToggled(DayOfWeek.WEDNESDAY)
        viewModel.onSaveClicked()
        advanceUntilIdle()

        val inserted = repository.insertedHabits.single()
        assertEquals("Morning Run", inserted.name)
        assertEquals("Park loop", inserted.description)
        assertEquals("directions_run", inserted.icon)
        assertEquals("#F04F45", inserted.colorHex)
        assertNull(inserted.reminderTime)
        assertTrue(inserted.id.isNotBlank())
        assertTrue(saved)
        assertNull(viewModel.uiState.value.errorMessageRes)
        collectJob.cancel()
    }

    @Test
    fun `save with reminder disabled sends null reminderTime even if time is set`() = runTest {
        val repository = FakeHabitRepository()
        val viewModel = AddContentViewModel(SaveHabit(repository))

        viewModel.onNameChanged("Morning Run")
        viewModel.onDayToggled(DayOfWeek.MONDAY)
        viewModel.onReminderTimeChanged("08:00")
        viewModel.onSaveClicked()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.reminderEnabled)
        assertNull(repository.insertedHabits.single().reminderTime)
    }

    @Test
    fun `save with reminder enabled persists reminderTime`() = runTest {
        val repository = FakeHabitRepository()
        val viewModel = AddContentViewModel(SaveHabit(repository))

        viewModel.onNameChanged("Morning Run")
        viewModel.onDayToggled(DayOfWeek.MONDAY)
        viewModel.onReminderEnabled(true)
        viewModel.onReminderTimeChanged("08:00")
        viewModel.onSaveClicked()
        advanceUntilIdle()

        assertEquals("08:00", repository.insertedHabits.single().reminderTime)
        assertNull(viewModel.uiState.value.errorMessageRes)
    }

    @Test
    fun `invalid reminder format only errors when reminder is enabled`() = runTest {
        val disabledRepository = FakeHabitRepository()
        val disabledViewModel = AddContentViewModel(SaveHabit(disabledRepository))
        disabledViewModel.onNameChanged("Morning Run")
        disabledViewModel.onDayToggled(DayOfWeek.MONDAY)
        disabledViewModel.onReminderTimeChanged("25:99")
        disabledViewModel.onSaveClicked()
        advanceUntilIdle()

        assertNull(disabledViewModel.uiState.value.errorMessageRes)
        assertNull(disabledRepository.insertedHabits.single().reminderTime)

        val enabledRepository = FakeHabitRepository()
        val enabledViewModel = AddContentViewModel(SaveHabit(enabledRepository))
        enabledViewModel.onNameChanged("Morning Run")
        enabledViewModel.onDayToggled(DayOfWeek.MONDAY)
        enabledViewModel.onReminderEnabled(true)
        enabledViewModel.onReminderTimeChanged("25:99")
        enabledViewModel.onSaveClicked()
        advanceUntilIdle()

        assertEquals(
            R.string.add_habit_error_reminder_format,
            enabledViewModel.uiState.value.errorMessageRes,
        )
        assertTrue(enabledRepository.insertedHabits.isEmpty())
    }

    @Test
    fun `disabling reminder keeps last time in state`() {
        val viewModel = createViewModel()

        viewModel.onReminderEnabled(true)
        viewModel.onReminderTimeChanged("07:30")
        viewModel.onReminderEnabled(false)

        assertFalse(viewModel.uiState.value.reminderEnabled)
        assertEquals("07:30", viewModel.uiState.value.reminderTime)
    }

    @Test
    fun `reminder denied turns toggle off and sets error`() {
        val viewModel = createViewModel()

        viewModel.onReminderEnabled(true)
        viewModel.onReminderEnabled(false)
        viewModel.onReminderDenied(R.string.habit_reminder_permission_denied)

        assertFalse(viewModel.uiState.value.reminderEnabled)
        assertEquals(
            R.string.habit_reminder_permission_denied,
            viewModel.uiState.value.errorMessageRes,
        )
    }

    private fun createViewModel(): AddContentViewModel =
        AddContentViewModel(SaveHabit(FakeHabitRepository()))
}
