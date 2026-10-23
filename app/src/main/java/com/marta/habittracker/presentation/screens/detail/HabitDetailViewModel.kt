package com.marta.habittracker.presentation.screens.detail

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marta.habittracker.R
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

private val ReminderTimePattern = Regex("""^([01]\d|2[0-3]):([0-5]\d)$""")
private const val DefaultReminderTime = "08:00"

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

    private val _reminderEnabled = MutableStateFlow(false)
    val reminderEnabled: StateFlow<Boolean> = _reminderEnabled.asStateFlow()

    private val _reminderTime = MutableStateFlow(DefaultReminderTime)
    val reminderTime: StateFlow<String> = _reminderTime.asStateFlow()

    private val _actionErrorRes = MutableStateFlow<Int?>(null)
    val actionErrorRes: StateFlow<Int?> = _actionErrorRes.asStateFlow()

    private val _inChallenge = MutableStateFlow(false)
    val inChallenge: StateFlow<Boolean> = _inChallenge.asStateFlow()

    private val _protectionProgress = MutableStateFlow<HabitProtectionProgress?>(null)
    val protectionProgress: StateFlow<HabitProtectionProgress?> = _protectionProgress.asStateFlow()

    private val _habitDeleted = Channel<Unit>(Channel.BUFFERED)
    val habitDeleted = _habitDeleted.receiveAsFlow()

    private var loadedHabit: Habit? = null
    private var reminderDraftInitialized = false

    init {
        refreshChallengeMembership()
        viewModelScope.launch {
            repository.getHabitById(habitId).collect { loaded ->
                loadedHabit = loaded
                if (loaded != null && !reminderDraftInitialized) {
                    reminderDraftInitialized = true
                    _reminderEnabled.value = loaded.reminderEnabled
                    _reminderTime.value = loaded.reminderTime ?: DefaultReminderTime
                }
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

    fun onReminderEnabled(enabled: Boolean) {
        _reminderEnabled.value = enabled
        _actionErrorRes.value = null
    }

    fun onReminderTimeChanged(reminderTime: String) {
        _reminderTime.value = reminderTime
        _actionErrorRes.value = null
    }

    fun onReminderDenied(@StringRes messageRes: Int) {
        _reminderEnabled.value = false
        _actionErrorRes.value = messageRes
    }

    fun saveReminder() {
        val current = loadedHabit ?: return
        val enabled = _reminderEnabled.value
        val time = _reminderTime.value.trim()
        if (enabled && time.isNotBlank() && !ReminderTimePattern.matches(time)) {
            _actionErrorRes.value = R.string.add_habit_error_reminder_format
            return
        }
        viewModelScope.launch {
            val reminderTime = time.takeIf { ReminderTimePattern.matches(it) }
            val updated = current.copy(
                reminderTime = reminderTime,
                reminderEnabled = enabled && reminderTime != null,
            )
            when (val result = repository.updateHabit(updated)) {
                is DataResult.Success -> {
                    loadedHabit = updated
                    _actionErrorRes.value = null
                }
                is DataResult.Error -> _actionErrorRes.value = result.error.toUserMessageRes()
            }
        }
    }

    fun deleteHabit() {
        viewModelScope.launch {
            val current = habit.value ?: loadedHabit ?: return@launch
            when (val result = repository.deleteHabit(current)) {
                is DataResult.Success -> {
                    _actionErrorRes.value = null
                    _habitDeleted.send(Unit)
                }
                is DataResult.Error -> {
                    _actionErrorRes.value = result.error.toUserMessageRes()
                }
            }
        }
    }

    val completionPercentage: StateFlow<Float> = history.map { records ->
        completionPercent(records)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)
}
