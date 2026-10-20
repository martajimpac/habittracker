package com.marta.habittracker.presentation.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.model.Habit
import com.marta.habittracker.domain.repository.AuthRepository
import com.marta.habittracker.domain.repository.FriendsRepository
import com.marta.habittracker.domain.repository.HabitRepository
import com.marta.habittracker.domain.repository.ProfileRepository
import com.marta.habittracker.presentation.utils.getCurrentWeekDays
import com.marta.habittracker.presentation.utils.toUserMessageRes
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
    private val profileRepository: ProfileRepository,
    private val friendsRepository: FriendsRepository,
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

    private val _actionErrorRes = MutableStateFlow<Int?>(null)
    val actionErrorRes: StateFlow<Int?> = _actionErrorRes.asStateFlow()

    private val _challengeHabitIds = MutableStateFlow<Set<String>>(emptySet())
    val challengeHabitIds: StateFlow<Set<String>> = _challengeHabitIds.asStateFlow()

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
        refreshDisplayName()
        refreshChallengeHabits()
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

    fun clearActionError() {
        _actionErrorRes.value = null
    }

    fun toggleComplete(habit: Habit) {
        if (_selectedDate.value != _today.value) return
        viewModelScope.launch {
            when (val result = repository.toggleHabitCompletion(habit, _selectedDate.value)) {
                is DataResult.Success -> _actionErrorRes.value = null
                is DataResult.Error -> _actionErrorRes.value = result.error.toUserMessageRes()
            }
        }
    }

}
