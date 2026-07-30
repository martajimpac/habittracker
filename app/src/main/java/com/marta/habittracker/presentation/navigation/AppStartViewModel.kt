package com.marta.habittracker.presentation.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marta.habittracker.domain.repository.AuthRepository
import com.marta.habittracker.domain.repository.OnboardingRepository
import com.marta.habittracker.domain.usecase.SyncHabits
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppStartViewModel @Inject constructor(
    private val onboardingRepository: OnboardingRepository,
    private val authRepository: AuthRepository,
    private val syncHabits: SyncHabits,
) : ViewModel() {

    private val _startDestination = MutableStateFlow<AppStartDestination?>(null)
    val startDestination: StateFlow<AppStartDestination?> = _startDestination.asStateFlow()

    init {
        viewModelScope.launch {
            val onboardingCompleted = onboardingRepository.isCompleted()
            val isLoggedIn = authRepository.isLoggedIn()
            _startDestination.value = resolveAppStartDestination(
                onboardingCompleted = onboardingCompleted,
                isLoggedIn = isLoggedIn,
            )
            if (isLoggedIn) {
                syncHabits()
            }
        }
    }
}
