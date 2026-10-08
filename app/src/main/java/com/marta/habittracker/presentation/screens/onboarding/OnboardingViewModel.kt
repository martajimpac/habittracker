package com.marta.habittracker.presentation.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marta.habittracker.domain.repository.OnboardingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val onboardingRepository: OnboardingRepository,
) : ViewModel() {

    private val _navigateToLogin = Channel<Unit>(Channel.BUFFERED)
    val navigateToLogin = _navigateToLogin.receiveAsFlow()

    fun completeOnboarding() {
        viewModelScope.launch {
            onboardingRepository.setCompleted()
            _navigateToLogin.send(Unit)
        }
    }
}
