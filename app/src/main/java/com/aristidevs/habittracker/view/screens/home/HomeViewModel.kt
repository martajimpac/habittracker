package com.aristidevs.habittracker.view.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aristidevs.habittracker.data.local.database.HabitRepository
import com.aristidevs.habittracker.data.local.database.HabitWithStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: HabitRepository
) : ViewModel() {

    val calendarDays: List<LocalDate> = (-3..3).map { LocalDate.now().plusDays(it.toLong()) }

    // Fecha que el usuario está visualizando (por defecto hoy)
    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    // Lista reactiva de hábitos. Cada vez que cambie la fecha o la DB, esto se actualiza solo.
    @OptIn(ExperimentalCoroutinesApi::class)
    val habits: StateFlow<List<HabitWithStatus>> = _selectedDate
        .flatMapLatest { date ->
            repository.getHabitsWithStatus(date)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList<HabitWithStatus>()
        )

    fun onDateSelected(date: LocalDate) {
        _selectedDate.value = date
    }
    
    fun toggleComplete(habitWithStatus: HabitWithStatus) {
        viewModelScope.launch {
            // Pasamos la fecha actual seleccionada para saber qué día marcar
            repository.toggleHabitCompletion(habitWithStatus, _selectedDate.value)
        }
    }
}