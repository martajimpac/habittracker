package com.marta.habittracker.presentation.screens.detail

import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marta.habittracker.core.toJava
import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.model.Habit
import com.marta.habittracker.domain.model.HabitRecord
import com.marta.habittracker.domain.repository.FriendsRepository
import com.marta.habittracker.domain.repository.HabitRepository
import com.marta.habittracker.domain.repository.StreakProtectionRepository
import com.marta.habittracker.presentation.utils.toUserMessageRes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

sealed interface HabitDetailEvent {
    data class ToggleFailed(@StringRes val messageRes: Int) : HabitDetailEvent
}

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

    /** Overridable in tests. */
    internal var clock: () -> LocalDate = { LocalDate.now() }

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

    private val _events = Channel<HabitDetailEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    /** Days with a save in flight; taps on them are ignored until the save finishes. */
    private val _savingDates = MutableStateFlow<Set<LocalDate>>(emptySet())
    val savingDates: StateFlow<Set<LocalDate>> = _savingDates.asStateFlow()

    init {
        refreshChallengeMembership()
        viewModelScope.launch {
            // Recompute whenever the habit or any of its records change (e.g. a past day toggled).
            combine(repository.getHabitById(habitId), repository.getRecordsForHabit(habitId)) { loaded, records ->
                loaded?.copy(records = records)
            }.collectLatest { loaded ->
                loadedHabit = loaded
                _protectionProgress.value = loaded?.let { habit ->
                    val status = streakProtectionRepository
                        .reconcile(listOf(habit), currentDate())
                        .habits
                        .firstOrNull { it.habitId == habit.id }
                    status?.let { HabitProtectionProgress(it.progress, it.milestone) }
                }
            }
        }
    }

    /**
     * Marks [date] as done, or as not done if it already was. Any day up to today can be edited;
     * future days are ignored. Room is only updated after Supabase accepts the change, and every
     * stat on this screen is derived from the records flow, so it refreshes on its own.
     */
    fun toggleDay(date: LocalDate) {
        if (date.isAfter(currentDate())) return
        if (date in _savingDates.value) return
        val wasCompleted = history.value.any { it.isCompleted && it.date.toJava() == date }
        _savingDates.update { it + date }
        viewModelScope.launch {
            try {
                when (val result = repository.toggleHabitCompletion(habitId, wasCompleted, date)) {
                    is DataResult.Success -> Unit
                    is DataResult.Error -> {
                        Log.e(TAG, "toggleDay failed date=$date error=${result.error}")
                        _events.send(HabitDetailEvent.ToggleFailed(result.error.toUserMessageRes()))
                    }
                }
            } finally {
                _savingDates.update { it - date }
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

    private fun currentDate(): LocalDate = clock()

    companion object {
        private const val TAG = "HabitDetailViewModel"
    }
}
