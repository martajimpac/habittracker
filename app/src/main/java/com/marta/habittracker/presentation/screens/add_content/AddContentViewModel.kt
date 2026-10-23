package com.marta.habittracker.presentation.screens.add_content

import androidx.compose.ui.graphics.Color
import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marta.habittracker.R
import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.model.Habit
import com.marta.habittracker.domain.model.HabitSchedule
import com.marta.habittracker.domain.model.resolvedSchedule
import com.marta.habittracker.domain.model.storedValue
import com.marta.habittracker.domain.repository.HabitRepository
import com.marta.habittracker.domain.usecase.SaveHabit
import com.marta.habittracker.presentation.utils.parseHabitColor
import com.marta.habittracker.presentation.utils.toUserMessageRes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.marta.habittracker.presentation.components.HabitIconKeys
import kotlinx.datetime.DayOfWeek
import com.marta.habittracker.presentation.utils.HabitColorOptions
import com.marta.habittracker.presentation.utils.toHex
import javax.inject.Inject



private val ReminderTimePattern = Regex("""^([01]\d|2[0-3]):([0-5]\d)$""")

enum class AddContentField {
    Name,
    Days,
    Reminder,
}

data class AddContentUiState(
    val name: String = "",
    val description: String = "",
    val icon: String = HabitIconKeys.DEFAULT,
    val color: Color = HabitColorOptions.first(),
    val reminderEnabled: Boolean = false,
    val reminderTime: String = "",
    val daysPerWeek: Int = 7,
    val schedule: HabitSchedule = HabitSchedule.DAILY,
    val selectedDays: Set<DayOfWeek> = emptySet(),
    val isPublic: Boolean = false,
    val isSaving: Boolean = false,
    val isEditing: Boolean = false,
    @StringRes val errorMessageRes: Int? = null,
)

@HiltViewModel
class AddContentViewModel @Inject constructor(
    private val saveHabit: SaveHabit,
    private val habitRepository: HabitRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val editingHabitId: String? = savedStateHandle.get<String>("habitId")?.takeIf { it.isNotBlank() }
    private var loadedHabit: Habit? = null

    private val _uiState = MutableStateFlow(AddContentUiState(isEditing = editingHabitId != null))
    val uiState: StateFlow<AddContentUiState> = _uiState.asStateFlow()

    private val _habitSaved = Channel<Unit>(Channel.BUFFERED)
    val habitSaved = _habitSaved.receiveAsFlow()

    private val _focusField = Channel<AddContentField>(Channel.BUFFERED)
    val focusField = _focusField.receiveAsFlow()

    init {
        val habitId = editingHabitId
        if (habitId != null) {
            viewModelScope.launch {
                habitRepository.getHabitById(habitId).collect { habit ->
                    if (habit == null || loadedHabit != null) return@collect
                    loadedHabit = habit
                    _uiState.update {
                        it.copy(
                            name = habit.name,
                            description = habit.description.orEmpty(),
                            icon = habit.icon,
                            color = parseHabitColor(habit.colorHex),
                            reminderEnabled = habit.reminderEnabled,
                            reminderTime = habit.reminderTime.orEmpty(),
                            daysPerWeek = habit.daysPerWeek.coerceIn(1, 7),
                            schedule = habit.resolvedSchedule(),
                            selectedDays = habit.daysOfWeek,
                            isPublic = habit.isPublic,
                            isEditing = true,
                        )
                    }
                }
            }
        }
    }

    fun onNameChanged(name: String) {
        _uiState.update { it.copy(name = name, errorMessageRes = null) }
    }

    fun onDescriptionChanged(description: String) {
        _uiState.update { it.copy(description = description) }
    }

    fun onIconSelected(icon: String) {
        _uiState.update { it.copy(icon = icon, errorMessageRes = null) }
    }

    fun onColorSelected(color: Color) {
        _uiState.update { it.copy(color = color, errorMessageRes = null) }
    }

    fun onReminderTimeChanged(reminderTime: String) {
        _uiState.update { it.copy(reminderTime = reminderTime, errorMessageRes = null) }
    }

    fun onReminderEnabled(enabled: Boolean) {
        _uiState.update {
            it.copy(
                reminderEnabled = enabled,
                reminderTime = if (enabled && it.reminderTime.isBlank()) "08:00" else it.reminderTime,
                errorMessageRes = null,
            )
        }
    }

    fun onReminderDenied(@StringRes messageRes: Int) {
        _uiState.update { it.copy(reminderEnabled = false, errorMessageRes = messageRes) }
    }

    fun onDaysPerWeekSelected(daysPerWeek: Int) {
        _uiState.update { it.copy(daysPerWeek = daysPerWeek.coerceIn(1, 7), errorMessageRes = null) }
    }

    fun onScheduleSelected(schedule: HabitSchedule) {
        _uiState.update { it.copy(schedule = schedule, errorMessageRes = null) }
    }

    fun onWeekdayToggled(day: DayOfWeek) {
        _uiState.update { state ->
            val next = if (day in state.selectedDays) state.selectedDays - day else state.selectedDays + day
            state.copy(selectedDays = next, errorMessageRes = null)
        }
    }

    fun onPublicChanged(isPublic: Boolean) {
        _uiState.update { it.copy(isPublic = isPublic) }
    }

    fun onSaveClicked() {
        val currentState = _uiState.value
        val validationError = validate(currentState)
        if (validationError != null) {
            _uiState.update { it.copy(errorMessageRes = validationError) }
            val field = when (validationError) {
                R.string.add_habit_error_name_required -> AddContentField.Name
                R.string.add_habit_error_days_required -> AddContentField.Days
                R.string.add_habit_error_reminder_format -> AddContentField.Reminder
                else -> null
            }
            if (field != null) {
                viewModelScope.launch { _focusField.send(field) }
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessageRes = null) }
            val reminderTime = currentState.reminderTime.trim().takeIf { ReminderTimePattern.matches(it) }
            val result = if (editingHabitId == null) {
                saveHabit(
                    name = currentState.name.trim(),
                    description = currentState.description.trim().ifBlank { null },
                    daysPerWeek = savedDaysPerWeek(currentState),
                    scheduleMode = currentState.schedule.storedValue(),
                    daysOfWeek = savedDays(currentState),
                    icon = currentState.icon,
                    colorHex = currentState.color.toHex(),
                    reminderTime = reminderTime,
                    reminderEnabled = currentState.reminderEnabled && reminderTime != null,
                    isPublic = currentState.isPublic,
                )
            } else {
                val current = loadedHabit
                if (current == null) {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessageRes = R.string.add_habit_error_save_failed,
                        )
                    }
                    return@launch
                }
                when (
                    val update = habitRepository.updateHabit(
                        current.copy(
                            name = currentState.name.trim(),
                            description = currentState.description.trim().ifBlank { null },
                            daysOfWeek = savedDays(currentState),
                            daysPerWeek = savedDaysPerWeek(currentState),
                            scheduleMode = currentState.schedule.storedValue(),
                            icon = currentState.icon,
                            colorHex = currentState.color.toHex(),
                            reminderTime = reminderTime,
                            reminderEnabled = currentState.reminderEnabled && reminderTime != null,
                            isPublic = currentState.isPublic,
                        ),
                    )
                ) {
                    is DataResult.Success -> DataResult.Success(current.id)
                    is DataResult.Error -> DataResult.Error(update.error)
                }
            }
            when (result) {
                is DataResult.Success -> {
                    _habitSaved.send(Unit)
                    _uiState.update { it.copy(isSaving = false) }
                }
                is DataResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessageRes = result.error.toUserMessageRes(),
                        )
                    }
                }
            }
        }
    }

    @StringRes
    private fun validate(state: AddContentUiState): Int? {
        return when {
            state.name.isBlank() -> R.string.add_habit_error_name_required
            state.schedule == HabitSchedule.WEEKDAYS && state.selectedDays.isEmpty() ->
                R.string.add_habit_error_days_required
            state.reminderEnabled &&
                state.reminderTime.isNotBlank() &&
                !ReminderTimePattern.matches(state.reminderTime.trim()) ->
                R.string.add_habit_error_reminder_format
            else -> null
        }
    }
}

private fun savedDays(state: AddContentUiState): Set<DayOfWeek> =
    if (state.schedule == HabitSchedule.WEEKDAYS) state.selectedDays else DayOfWeek.entries.toSet()

private fun savedDaysPerWeek(state: AddContentUiState): Int =
    if (state.schedule == HabitSchedule.WEEKLY_COUNT) state.daysPerWeek.coerceIn(1, 7) else 7
