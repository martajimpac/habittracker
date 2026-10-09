package com.marta.habittracker.presentation.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.repository.AuthRepository
import com.marta.habittracker.domain.repository.HabitRepository
import com.marta.habittracker.domain.repository.ProfileRepository
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
    val email: String = "",
    val avatarUrl: String? = null,
    val avatarColor: String = "",
    val dayStreak: Int = 0,
    val completedCount: Int = 0,
    val habitsCount: Int = 0,
)

private data class ProfileIdentity(
    val displayName: String,
    val email: String,
    val avatarUrl: String?,
    val avatarColor: String,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    habitRepository: HabitRepository,
    private val profileRepository: ProfileRepository,
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
                        email = email,
                        avatarUrl = result.data.avatarUrl,
                        avatarColor = result.data.avatarColor,
                    )
                    is DataResult.Error -> ProfileIdentity(
                        displayName = fallbackDisplayNameFromEmail(email),
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

    val uiState: StateFlow<ProfileUiState> = combine(
        identityFlow,
        habitRepository.getAllHabitsWithRecords(),
    ) { identity, habits ->
        val today = LocalDate.now()
        ProfileUiState(
            displayName = identity.displayName,
            email = identity.email,
            avatarUrl = identity.avatarUrl,
            avatarColor = identity.avatarColor,
            dayStreak = habits.maxOfOrNull { calculateStreak(it.records, today) } ?: 0,
            completedCount = habits.sumOf { habit -> habit.records.count { it.isCompleted } },
            habitsCount = habits.size,
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
