package com.marta.habittracker.view.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marta.habittracker.domain.model.Habit
import com.marta.habittracker.domain.repository.AuthRepository
import com.marta.habittracker.domain.repository.HabitRepository
import com.marta.habittracker.view.utils.getCurrentWeekDays
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
    private val repository: HabitRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    val weekDays: List<LocalDate> = getCurrentWeekDays()
    val today: LocalDate = LocalDate.now()

    private val _selectedDate = MutableStateFlow(today)
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _userDisplayName = MutableStateFlow("there")
    val userDisplayName: StateFlow<String> = _userDisplayName.asStateFlow()

    val allHabits: StateFlow<List<Habit>> = repository.getAllHabitsWithRecords()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val habits: StateFlow<List<Habit>> = _selectedDate
        .flatMapLatest { date -> repository.getHabitsWithStatus(date) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    init {
        viewModelScope.launch {
            _userDisplayName.value = authRepository.getCurrentUserDisplayName()
        }
    }

    fun onDateSelected(date: LocalDate) {
        _selectedDate.value = date
    }

    fun toggleComplete(habit: Habit) {
        if (_selectedDate.value != today) return
        viewModelScope.launch {
            repository.toggleHabitCompletion(habit, _selectedDate.value)
        }
    }
}
