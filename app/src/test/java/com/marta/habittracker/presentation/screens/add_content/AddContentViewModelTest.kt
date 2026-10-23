package com.marta.habittracker.presentation.screens.add_content

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.SavedStateHandle
import com.marta.habittracker.R
import com.marta.habittracker.domain.model.Habit
import com.marta.habittracker.domain.usecase.FakeHabitRepository
import com.marta.habittracker.domain.usecase.SaveHabit
import com.marta.habittracker.presentation.theme.HabitColor1
import kotlinx.datetime.DayOfWeek as KotlinDayOfWeek
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
import java.time.Instant

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

        var focused: AddContentField? = null
        val collectJob = launch { viewModel.focusField.collect { focused = it } }
        viewModel.onSaveClicked()
        advanceUntilIdle()

        assertEquals(R.string.add_habit_error_name_required, viewModel.uiState.value.errorMessageRes)
        assertEquals(AddContentField.Name, focused)
        assertFalse(viewModel.uiState.value.isSaving)
        collectJob.cancel()
    }

    @Test
    fun `save stores the chosen days per week on every weekday`() = runTest {
        val repository = FakeHabitRepository()
        val viewModel = createViewModel(repository)

        viewModel.onNameChanged("Morning Run")
        viewModel.onScheduleSelected(com.marta.habittracker.domain.model.HabitSchedule.WEEKLY_COUNT)
        viewModel.onDaysPerWeekSelected(2)
        viewModel.onSaveClicked()
        advanceUntilIdle()

        val inserted = repository.insertedHabits.single()
        assertEquals(2, inserted.daysPerWeek)
        assertEquals(KotlinDayOfWeek.entries.toSet(), inserted.daysOfWeek)
    }

    @Test
    fun `save with valid data persists visuals and emits saved`() = runTest {
        val repository = FakeHabitRepository()
        val viewModel = createViewModel(repository)
        var saved = false
        val collectJob = launch {
            viewModel.habitSaved.collect { saved = true }
        }

        viewModel.onNameChanged("Morning Run")
        viewModel.onDescriptionChanged("Park loop")
        viewModel.onIconSelected("directions_run")
        viewModel.onColorSelected(HabitColor1)
        viewModel.onDaysPerWeekSelected(2)
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
    fun `save keeps the time when the reminder is off`() = runTest {
        val repository = FakeHabitRepository()
        val viewModel = createViewModel(repository)

        viewModel.onNameChanged("Morning Run")
        viewModel.onDaysPerWeekSelected(3)
        viewModel.onReminderTimeChanged("08:00")
        viewModel.onSaveClicked()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.reminderEnabled)
        assertEquals("08:00", repository.insertedHabits.single().reminderTime)
        assertFalse(repository.insertedHabits.single().reminderEnabled)
    }

    @Test
    fun `save with reminder enabled persists reminderTime`() = runTest {
        val repository = FakeHabitRepository()
        val viewModel = createViewModel(repository)

        viewModel.onNameChanged("Morning Run")
        viewModel.onDaysPerWeekSelected(3)
        viewModel.onReminderEnabled(true)
        viewModel.onReminderTimeChanged("08:00")
        viewModel.onSaveClicked()
        advanceUntilIdle()

        assertEquals("08:00", repository.insertedHabits.single().reminderTime)
        assertTrue(repository.insertedHabits.single().reminderEnabled)
        assertNull(viewModel.uiState.value.errorMessageRes)
    }

    @Test
    fun `invalid reminder format only errors when reminder is enabled`() = runTest {
        val disabledRepository = FakeHabitRepository()
        val disabledViewModel = createViewModel(disabledRepository)
        disabledViewModel.onNameChanged("Morning Run")
        disabledViewModel.onDaysPerWeekSelected(3)
        disabledViewModel.onReminderTimeChanged("25:99")
        disabledViewModel.onSaveClicked()
        advanceUntilIdle()

        assertNull(disabledViewModel.uiState.value.errorMessageRes)
        assertNull(disabledRepository.insertedHabits.single().reminderTime)

        val enabledRepository = FakeHabitRepository()
        val enabledViewModel = createViewModel(enabledRepository)
        enabledViewModel.onNameChanged("Morning Run")
        enabledViewModel.onDaysPerWeekSelected(3)
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

    @Test
    fun `editing an existing habit prefills the form and updates instead of inserting`() = runTest {
        val habit = Habit(
            id = "habit-1",
            name = "Read",
            description = "A chapter",
            daysOfWeek = setOf(KotlinDayOfWeek.MONDAY, KotlinDayOfWeek.FRIDAY),
            icon = "book",
            colorHex = "#F04F45",
            reminderTime = "07:30",
            reminderEnabled = true,
            createdAt = Instant.parse("2026-01-01T00:00:00Z"),
            records = emptyList(),
            isPublic = true,
        )
        val repository = FakeHabitRepository(habitsById = mapOf(habit.id to habit))
        val viewModel = AddContentViewModel(
            saveHabit = SaveHabit(repository),
            habitRepository = repository,
            savedStateHandle = SavedStateHandle(mapOf("habitId" to habit.id)),
        )
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isEditing)
        assertEquals("Read", viewModel.uiState.value.name)
        assertEquals("A chapter", viewModel.uiState.value.description)
        assertEquals(com.marta.habittracker.domain.model.HabitSchedule.WEEKDAYS, viewModel.uiState.value.schedule)
        assertEquals(setOf(KotlinDayOfWeek.MONDAY, KotlinDayOfWeek.FRIDAY), viewModel.uiState.value.selectedDays)
        assertEquals("book", viewModel.uiState.value.icon)
        assertTrue(viewModel.uiState.value.reminderEnabled)
        assertEquals("07:30", viewModel.uiState.value.reminderTime)
        assertTrue(viewModel.uiState.value.isPublic)

        viewModel.onNameChanged("Read more")
        viewModel.onSaveClicked()
        advanceUntilIdle()

        val updated = repository.updatedHabits.single()
        assertEquals(habit.id, updated.id)
        assertEquals("Read more", updated.name)
        assertEquals(habit.createdAt, updated.createdAt)
        assertTrue(repository.insertedHabits.isEmpty())
    }

    private fun createViewModel(
        repository: FakeHabitRepository = FakeHabitRepository(),
    ): AddContentViewModel = AddContentViewModel(
        saveHabit = SaveHabit(repository),
        habitRepository = repository,
        savedStateHandle = SavedStateHandle(),
    )
}
