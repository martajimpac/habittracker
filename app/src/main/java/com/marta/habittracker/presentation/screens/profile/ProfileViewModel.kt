package com.marta.habittracker.presentation.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.repository.AuthRepository
import com.marta.habittracker.domain.repository.HabitRepository
import com.marta.habittracker.presentation.screens.home.calculateStreak
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class ProfileUiState(
    val displayName: String = "",
    val email: String = "",
    val dayStreak: Int = 0,
    val completedCount: Int = 0,
    val habitsCount: Int = 0,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    habitRepository: HabitRepository,
) : ViewModel() {

    private val authFlow = flow {
        emit(
            authRepository.getCurrentUserDisplayName() to authRepository.getCurrentUserEmail(),
        )
    }

    private val _navigateToLogin = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val navigateToLogin: SharedFlow<Unit> = _navigateToLogin.asSharedFlow()

    val uiState: StateFlow<ProfileUiState> = combine(
        authFlow,
        habitRepository.getAllHabitsWithRecords(),
    ) { (displayName, email), habits ->
        val today = LocalDate.now()
        ProfileUiState(
            displayName = displayName,
            email = email,
            dayStreak = habits.maxOfOrNull { calculateStreak(it.records, today) } ?: 0,
            completedCount = habits.sumOf { habit -> habit.records.count { it.isCompleted } },
            habitsCount = habits.size,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ProfileUiState(),
    )

    fun onSignOutClicked() {
        viewModelScope.launch {
            when (authRepository.signOut()) {
                is DataResult.Success -> _navigateToLogin.emit(Unit)
                is DataResult.Error -> Unit
            }
        }
    }
}
