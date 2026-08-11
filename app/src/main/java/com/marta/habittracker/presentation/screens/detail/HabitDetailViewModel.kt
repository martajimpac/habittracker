package com.marta.habittracker.presentation.screens.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.model.Habit
import com.marta.habittracker.domain.model.HabitRecord
import com.marta.habittracker.domain.repository.HabitRepository
import com.marta.habittracker.presentation.utils.toUserMessageRes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HabitDetailViewModel @Inject constructor(
    private val repository: HabitRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val habitId: String = checkNotNull(savedStateHandle["habitId"])

    val habit: StateFlow<Habit?> = repository.getHabitById(habitId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null,
        )

    val history: StateFlow<List<HabitRecord>> = repository.getRecordsForHabit(habitId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    private val _actionErrorRes = MutableStateFlow<Int?>(null)
    val actionErrorRes: StateFlow<Int?> = _actionErrorRes.asStateFlow()

    private val _habitDeleted = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val habitDeleted: SharedFlow<Unit> = _habitDeleted.asSharedFlow()

    fun deleteHabit() {
        viewModelScope.launch {
            val current = habit.value ?: return@launch
            when (val result = repository.deleteHabit(current)) {
                is DataResult.Success -> {
                    _actionErrorRes.value = null
                    _habitDeleted.emit(Unit)
                }
                is DataResult.Error -> {
                    _actionErrorRes.value = result.error.toUserMessageRes()
                }
            }
        }
    }

    val completionPercentage: StateFlow<Float> = history.map { records ->
        if (records.isEmpty()) {
            0f
        } else {
            val completed = records.count { it.isCompleted }
            completed.toFloat() / records.size
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)
}
