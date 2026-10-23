package com.marta.habittracker.presentation.screens.profile

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.model.DailyCheckInSettings
import com.marta.habittracker.domain.model.ThemeMode
import com.marta.habittracker.domain.repository.AuthRepository
import com.marta.habittracker.domain.repository.DailyCheckInRepository
import com.marta.habittracker.domain.repository.HabitRepository
import com.marta.habittracker.domain.repository.ProfileRepository
import com.marta.habittracker.domain.repository.ThemeRepository
import com.marta.habittracker.domain.model.countsInWeeks
import com.marta.habittracker.presentation.screens.home.calculateStreak
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class ProfileUiState(
    val displayName: String = "",
    val username: String = "",
    val email: String = "",
    val avatarUrl: String? = null,
    val avatarColor: String = "",
    val dayStreak: Int = 0,
    val streakInWeeks: Boolean = false,
    val completedCount: Int = 0,
    val habitsCount: Int = 0,
    val isReady: Boolean = false,
)

private data class ProfileIdentity(
    val displayName: String,
    val username: String,
    val email: String,
    val avatarUrl: String?,
    val avatarColor: String,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    habitRepository: HabitRepository,
    private val profileRepository: ProfileRepository,
    private val themeRepository: ThemeRepository,
    private val dailyCheckInRepository: DailyCheckInRepository,
) : ViewModel() {

    private val profileRefresh = MutableStateFlow(0)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val identityFlow = profileRefresh.flatMapLatest {
        flow {
            val email = authRepository.getCurrentUserEmail()
            emit(
                when (val result = profileRepository.getMyProfile()) {
                    is DataResult.Success -> ProfileIdentity(
                        displayName = result.data.displayName,
                        username = result.data.username,
                        email = email,
                        avatarUrl = result.data.avatarUrl,
                        avatarColor = result.data.avatarColor,
                    )
                    is DataResult.Error -> ProfileIdentity(
                        displayName = fallbackDisplayNameFromEmail(email),
                        username = "",
                        email = email,
                        avatarUrl = null,
                        avatarColor = "",
                    )
                },
            )
        }
    }

    private val _navigateToLogin = Channel<Unit>(Channel.BUFFERED)
    val navigateToLogin = _navigateToLogin.receiveAsFlow()

    private val _reminderDenied = Channel<Int>(Channel.BUFFERED)
    val reminderDenied = _reminderDenied.receiveAsFlow()

    private val _dailyCheckInActivated = Channel<Unit>(Channel.BUFFERED)
    val dailyCheckInActivated = _dailyCheckInActivated.receiveAsFlow()

    val dailyCheckIn: StateFlow<DailyCheckInSettings> = dailyCheckInRepository.settings()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DailyCheckInSettings(),
        )

    fun onDailyCheckInEnabled(enabled: Boolean) {
        viewModelScope.launch {
            dailyCheckInRepository.setEnabled(enabled)
            if (enabled) _dailyCheckInActivated.send(Unit)
        }
    }

    fun onDailyCheckInTime(time: String) {
        viewModelScope.launch { dailyCheckInRepository.setTime(time) }
    }

    fun onReminderDenied(@StringRes messageRes: Int) {
        viewModelScope.launch { _reminderDenied.send(messageRes) }
    }

    val themeMode: StateFlow<ThemeMode> = themeRepository.themeMode()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ThemeMode.System,
        )

    fun onDarkModeChanged(enabled: Boolean) {
        viewModelScope.launch {
            themeRepository.setThemeMode(if (enabled) ThemeMode.Dark else ThemeMode.Light)
        }
    }

    val uiState: StateFlow<ProfileUiState> = combine(
        identityFlow,
        habitRepository.getAllHabitsWithRecords(),
    ) { identity, habits ->
        val today = LocalDate.now()
        ProfileUiState(
            displayName = identity.displayName,
            username = identity.username,
            email = identity.email,
            avatarUrl = identity.avatarUrl,
            avatarColor = identity.avatarColor,
            dayStreak = run {
                val daily = habits.filter { !it.countsInWeeks() }
                    .maxOfOrNull { calculateStreak(it, today = today) } ?: 0
                if (daily > 0) {
                    daily
                } else {
                    habits.maxOfOrNull { calculateStreak(it, today = today) } ?: 0
                }
            },
            streakInWeeks = habits.none { !it.countsInWeeks() && calculateStreak(it, today = today) > 0 } &&
                habits.any { it.countsInWeeks() && calculateStreak(it, today = today) > 0 },
            completedCount = habits.sumOf { habit -> habit.records.count { it.isCompleted } },
            habitsCount = habits.size,
            isReady = true,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ProfileUiState(),
    )

    fun refresh() {
        profileRefresh.update { it + 1 }
    }

    fun onSignOutClicked() {
        viewModelScope.launch {
            when (authRepository.signOut()) {
                is DataResult.Success -> _navigateToLogin.send(Unit)
                is DataResult.Error -> Unit
            }
        }
    }
}

private fun fallbackDisplayNameFromEmail(email: String): String =
    email.substringBefore("@").takeIf { email.contains("@") }.orEmpty()
