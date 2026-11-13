package com.marta.habittracker.presentation.screens.detail

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.model.Habit
import com.marta.habittracker.domain.model.HabitRecord
import com.marta.habittracker.domain.repository.FriendsRepository
import com.marta.habittracker.domain.repository.HabitRepository
import com.marta.habittracker.domain.repository.StreakProtectionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class HabitProtectionProgress(
    val progress: Int,
    val milestone: Int,
)

@HiltViewModel
class HabitDetailViewModel @Inject constructor(
    private val repository: HabitRepository,
    private val friendsRepository: FriendsRepository,
    private val streakProtectionRepository: StreakProtectionRepository,
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

    private val _inChallenge = MutableStateFlow(false)
    val inChallenge: StateFlow<Boolean> = _inChallenge.asStateFlow()

    private val _protectionProgress = MutableStateFlow<HabitProtectionProgress?>(null)
    val protectionProgress: StateFlow<HabitProtectionProgress?> = _protectionProgress.asStateFlow()

    private val _habitDeleted = Channel<Unit>(Channel.BUFFERED)
    val habitDeleted = _habitDeleted.receiveAsFlow()

    private var loadedHabit: Habit? = null

    init {
        refreshChallengeMembership()
        viewModelScope.launch {
            repository.getHabitById(habitId).collect { loaded ->
                loadedHabit = loaded
                _protectionProgress.value = loaded?.let { habit ->
                    val status = streakProtectionRepository
                        .reconcile(listOf(habit), LocalDate.now())
                        .habits
                        .firstOrNull { it.habitId == habit.id }
                    status?.let { HabitProtectionProgress(it.progress, it.milestone) }
                }
            }
        }
    }

    fun refreshChallengeMembership() {
        viewModelScope.launch {
            when (val result = friendsRepository.challengeHabitIds()) {
                is DataResult.Success -> _inChallenge.value = habitId in result.data
                is DataResult.Error -> Unit
            }
        }
    }

    fun deleteHabit() {
        viewModelScope.launch {
            val current = habit.value ?: loadedHabit ?: return@launch
            when (val result = repository.deleteHabit(current)) {
                is DataResult.Success -> _habitDeleted.send(Unit)
                is DataResult.Error -> Log.e(TAG, "deleteHabit failed: ${result.error}")
            }
        }
    }

    val completionPercentage: StateFlow<Float> = history.map { records ->
        completionPercent(records)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    companion object {
        private const val TAG = "HabitDetailViewModel"
    }
}
