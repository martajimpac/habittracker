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

data class AddContentUiState(
    val name: String = "",
    val description: String = "",
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
                )
                _habitSaved.emit(Unit)
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = "No se pudo guardar el hábito. Inténtalo de nuevo.",
                    )
                }
            }
        }
    }

    private fun validate(state: AddContentUiState): String? {
        return when {
            state.name.isBlank() -> "El nombre del hábito es obligatorio."
            state.selectedDays.isEmpty() -> "Selecciona al menos un día de la semana."
            else -> null
        }
    }
}
