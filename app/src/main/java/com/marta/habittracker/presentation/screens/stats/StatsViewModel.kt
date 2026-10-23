package com.marta.habittracker.presentation.screens.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.model.Habit
import com.marta.habittracker.domain.repository.FriendsRepository
import com.marta.habittracker.domain.repository.HabitRepository
import com.marta.habittracker.domain.repository.StreakProtectionRepository
import com.marta.habittracker.domain.usecase.ProtectionReconcileResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StatsViewModel @Inject constructor(
    habitRepository: HabitRepository,
    private val friendsRepository: FriendsRepository,
    private val streakProtectionRepository: StreakProtectionRepository,
) : ViewModel() {

    val habits: StateFlow<List<Habit>> = habitRepository.getAllHabitsWithRecords()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    private val _challengeHabitIds = MutableStateFlow<Set<String>>(emptySet())
    val challengeHabitIds: StateFlow<Set<String>> = _challengeHabitIds.asStateFlow()

    private val _protection = MutableStateFlow(ProtectionReconcileResult(0, emptyList(), emptyList()))
    val protection: StateFlow<ProtectionReconcileResult> = _protection.asStateFlow()

    init {
        refreshChallengeHabits()
        viewModelScope.launch {
            habits.collect { current ->
                _protection.value = streakProtectionRepository.reconcile(current, java.time.LocalDate.now())
            }
        }
    }

    fun refreshChallengeHabits() {
        viewModelScope.launch {
            when (val result = friendsRepository.challengeHabitIds()) {
                is DataResult.Success -> _challengeHabitIds.value = result.data
                is DataResult.Error -> Unit
            }
        }
    }
}
