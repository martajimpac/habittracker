package com.marta.habittracker.presentation.screens.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.model.errors.AppError
import com.marta.habittracker.domain.model.Habit
import com.marta.habittracker.domain.repository.AuthRepository
import com.marta.habittracker.domain.repository.FriendsRepository
import com.marta.habittracker.domain.repository.HabitRepository
import com.marta.habittracker.domain.repository.ProfileRepository
import com.marta.habittracker.domain.repository.StreakProtectionRepository
import com.marta.habittracker.domain.usecase.ProtectionEventType
import com.marta.habittracker.domain.usecase.ProtectionReconcileResult
import com.marta.habittracker.domain.widget.AppWidgetRefresher
import com.marta.habittracker.presentation.utils.getCurrentWeekDays
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: HabitRepository,
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository,
    private val friendsRepository: FriendsRepository,
    private val streakProtectionRepository: StreakProtectionRepository,
    private val widgetRefresher: AppWidgetRefresher,
) : ViewModel() {

    internal var currentDate: () -> LocalDate = { LocalDate.now() }

    private val _today = MutableStateFlow(currentDate())
    val today: StateFlow<LocalDate> = _today.asStateFlow()

    private val _weekDays = MutableStateFlow(getCurrentWeekDays(_today.value))
    val weekDays: StateFlow<List<LocalDate>> = _weekDays.asStateFlow()

    private val _selectedDate = MutableStateFlow(_today.value)
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _userDisplayName = MutableStateFlow("")
    val userDisplayName: StateFlow<String> = _userDisplayName.asStateFlow()

    private val _avatarUrl = MutableStateFlow<String?>(null)
    val avatarUrl: StateFlow<String?> = _avatarUrl.asStateFlow()

    private val _avatarColor = MutableStateFlow("")
    val avatarColor: StateFlow<String> = _avatarColor.asStateFlow()

    private val _challengeHabitIds = MutableStateFlow<Set<String>>(emptySet())
    val challengeHabitIds: StateFlow<Set<String>> = _challengeHabitIds.asStateFlow()

    private val _protection = MutableStateFlow(ProtectionReconcileResult(0, emptyList(), emptyList()))
    val protection: StateFlow<ProtectionReconcileResult> = _protection.asStateFlow()

    /** (habitId, day) pairs with a save in flight; taps on them are ignored until it finishes. */
    private val _savingToggles = MutableStateFlow<Set<Pair<String, LocalDate>>>(emptySet())
    val savingToggles: StateFlow<Set<Pair<String, LocalDate>>> = _savingToggles.asStateFlow()

    private val _events = Channel<HomeEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    // null until Room emits for the first time, so the UI can show a loader instead of "0 done".
    private val allHabitsOrNull: StateFlow<List<Habit>?> = repository.getAllHabitsWithRecords()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null,
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    private val habitsOrNull: StateFlow<List<Habit>?> = _selectedDate
        .flatMapLatest { date -> repository.getHabitsWithStatus(date) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null,
        )

    val allHabits: StateFlow<List<Habit>> = allHabitsOrNull
        .map { it.orEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val habits: StateFlow<List<Habit>> = habitsOrNull
        .map { it.orEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isLoading: StateFlow<Boolean> = combine(habitsOrNull, allHabitsOrNull) { day, all ->
        day == null || all == null
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    init {
        refreshDisplayName()
        refreshChallengeHabits()
        viewModelScope.launch {
            allHabitsOrNull.filterNotNull().collect { habits ->
                val result = streakProtectionRepository.reconcile(habits, currentDate())
                _protection.value = result
                if (result.newEvents.any { it.type == ProtectionEventType.GRANT }) {
                    _events.send(HomeEvent.ProtectionEarned)
                }
            }
        }
    }

    fun refreshDisplayName() {
        viewModelScope.launch {
            when (val result = profileRepository.getMyProfile()) {
                is DataResult.Success -> {
                    _userDisplayName.value = result.data.displayName
                    _avatarUrl.value = result.data.avatarUrl
                    _avatarColor.value = result.data.avatarColor
                }
                is DataResult.Error -> {
                    val email = authRepository.getCurrentUserEmail()
                    _userDisplayName.value =
                        email.substringBefore("@").takeIf { email.contains("@") }.orEmpty()
                    _avatarUrl.value = null
                    _avatarColor.value = ""
                }
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

    fun refreshDay() {
        val now = currentDate()
        val previousToday = _today.value
        if (now == previousToday) return
        if (_selectedDate.value == previousToday) {
            _selectedDate.value = now
        }
        _today.value = now
        _weekDays.value = getCurrentWeekDays(now)
    }

    fun onDateSelected(date: LocalDate) {
        _selectedDate.value = date
    }

    /**
     * Marks [habit] as done on the selected day, or as not done if it already was. Today and
     * past days can be edited; future days are ignored. The day is captured on tap, so changing
     * the selected day while saving does not change which day is saved.
     */
    fun toggleComplete(habit: Habit) {
        val date = _selectedDate.value
        if (date.isAfter(_today.value)) return
        val key = habit.id to date
        if (key in _savingToggles.value) return
        _savingToggles.update { it + key }
        viewModelScope.launch {
            try {
                val result = repository.toggleHabitCompletion(habit.id, habit.isCompleted, date)
                if (result is DataResult.Error) {
                    Log.e(TAG, "toggleComplete failed date=$date error=${result.error}")
                }
                _events.send(HomeEvent.HabitToggle(result))
            } finally {
                _savingToggles.update { it - key }
            }
        }
    }

    companion object {
        private const val TAG = "HomeViewModel"
    }
}

sealed interface HomeEvent {
    data object ProtectionEarned : HomeEvent
    data class HabitToggle(
        val result: DataResult<Unit, AppError>,
    ) : HomeEvent
}
