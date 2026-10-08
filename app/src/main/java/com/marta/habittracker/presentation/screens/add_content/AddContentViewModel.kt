package com.marta.habittracker.presentation.screens.add_content

import androidx.compose.ui.graphics.Color
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marta.habittracker.R
import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.usecase.SaveHabit
import com.marta.habittracker.presentation.utils.toUserMessageRes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import com.marta.habittracker.presentation.components.HabitIconKeys
import com.marta.habittracker.presentation.utils.HabitColorOptions
import com.marta.habittracker.presentation.utils.toHex
import javax.inject.Inject



private val ReminderTimePattern = Regex("""^([01]\d|2[0-3]):([0-5]\d)$""")

data class AddContentUiState(
    val name: String = "",
    val description: String = "",
    val icon: String = HabitIconKeys.DEFAULT,
    val color: Color = HabitColorOptions.first(),
    val reminderTime: String = "08:00",
    val selectedDays: Set<DayOfWeek> = emptySet(),
    val isPublic: Boolean = false,
    val isSaving: Boolean = false,
    @StringRes val errorMessageRes: Int? = null,
)

@HiltViewModel
class AddContentViewModel @Inject constructor(
    private val saveHabit: SaveHabit,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddContentUiState())
    val uiState: StateFlow<AddContentUiState> = _uiState.asStateFlow()

    private val _habitSaved = Channel<Unit>(Channel.BUFFERED)
    val habitSaved = _habitSaved.receiveAsFlow()

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

    fun onDayToggled(day: DayOfWeek) {
        _uiState.update { current ->
            val updatedDays = if (current.selectedDays.contains(day)) {
                current.selectedDays - day
            } else {
                current.selectedDays + day
            }
            current.copy(selectedDays = updatedDays, errorMessageRes = null)
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
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessageRes = null) }
            when (
                val result = saveHabit(
                    name = currentState.name.trim(),
                    description = currentState.description.trim().ifBlank { null },
                    daysOfWeek = currentState.selectedDays,
                    icon = currentState.icon,
                    colorHex = currentState.color.toHex(),
                    reminderTime = currentState.reminderTime.trim().ifBlank { null },
                    isPublic = currentState.isPublic,
                )
            ) {
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
            state.selectedDays.isEmpty() -> R.string.add_habit_error_days_required
            state.reminderTime.isNotBlank() && !ReminderTimePattern.matches(state.reminderTime.trim()) ->
                R.string.add_habit_error_reminder_format
            else -> null
        }
    }
}
