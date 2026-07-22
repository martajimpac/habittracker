package com.marta.habittracker.view.screens.add_content

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marta.habittracker.domain.usecase.SaveHabit
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import javax.inject.Inject

val HabitIconOptions = listOf(
    "💧", "🏃", "📚", "🧘", "🍎", "😴", "☕", "🎵", "✍️", "❤️", "🎯", "⚡",
)

val HabitColorOptions = listOf(
    "#6750A4", "#0D9488", "#D97706", "#E11D48",
    "#059669", "#2563EB", "#EA580C", "#7C3AED",
)

private val ReminderTimePattern = Regex("""^([01]\d|2[0-3]):([0-5]\d)$""")

data class AddContentUiState(
    val name: String = "",
    val description: String = "",
    val icon: String = HabitIconOptions.first(),
    val colorHex: String = HabitColorOptions.first(),
    val reminderTime: String = "08:00",
    val selectedDays: Set<DayOfWeek> = emptySet(),
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class AddContentViewModel @Inject constructor(
    private val saveHabit: SaveHabit,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddContentUiState())
    val uiState: StateFlow<AddContentUiState> = _uiState.asStateFlow()

    private val _habitSaved = MutableSharedFlow<Unit>()
    val habitSaved: SharedFlow<Unit> = _habitSaved.asSharedFlow()

    fun onNameChanged(name: String) {
        _uiState.update { it.copy(name = name, errorMessage = null) }
    }

    fun onDescriptionChanged(description: String) {
        _uiState.update { it.copy(description = description) }
    }

    fun onIconSelected(icon: String) {
        _uiState.update { it.copy(icon = icon, errorMessage = null) }
    }

    fun onColorSelected(colorHex: String) {
        _uiState.update { it.copy(colorHex = colorHex, errorMessage = null) }
    }

    fun onReminderTimeChanged(reminderTime: String) {
        _uiState.update { it.copy(reminderTime = reminderTime, errorMessage = null) }
    }

    fun onDayToggled(day: DayOfWeek) {
        _uiState.update { current ->
            val updatedDays = if (current.selectedDays.contains(day)) {
                current.selectedDays - day
            } else {
                current.selectedDays + day
            }
            current.copy(selectedDays = updatedDays, errorMessage = null)
        }
    }

    fun onSaveClicked() {
        val currentState = _uiState.value
        val validationError = validate(currentState)
        if (validationError != null) {
            _uiState.update { it.copy(errorMessage = validationError) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            try {
                saveHabit(
                    name = currentState.name.trim(),
                    description = currentState.description.trim().ifBlank { null },
                    daysOfWeek = currentState.selectedDays,
                    icon = currentState.icon,
                    colorHex = currentState.colorHex,
                    reminderTime = currentState.reminderTime.trim().ifBlank { null },
                )
                _habitSaved.emit(Unit)
                _uiState.update { it.copy(isSaving = false) }
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = "Could not save the habit. Please try again.",
                    )
                }
            }
        }
    }

    private fun validate(state: AddContentUiState): String? {
        return when {
            state.name.isBlank() -> "Habit name is required."
            state.selectedDays.isEmpty() -> "Select at least one day of the week."
            state.reminderTime.isNotBlank() && !ReminderTimePattern.matches(state.reminderTime.trim()) ->
                "Reminder time must use HH:mm format."
            else -> null
        }
    }
}
