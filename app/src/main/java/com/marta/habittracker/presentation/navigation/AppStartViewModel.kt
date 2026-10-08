package com.marta.habittracker.presentation.navigation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marta.habittracker.BuildConfig
import com.marta.habittracker.domain.repository.AuthRepository
import com.marta.habittracker.domain.repository.OnboardingRepository
import com.marta.habittracker.domain.usecase.SyncHabits
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
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
            Log.d(TAG, "AppStartViewModel init: checking app start destination")
            
            // Esperar un poco a que Supabase recupere la sesión guardada
            delay(500)
            
            val isLoggedIn = authRepository.isLoggedIn()
            Log.d(TAG, "isLoggedIn: $isLoggedIn, BuildConfig.DEBUG: ${BuildConfig.DEBUG}")
            
            val onboardingCompleted = onboardingRepository.isCompleted()

            /* Código para testear el onboarding
                if (BuildConfig.DEBUG) {
                Log.d(TAG, "DEBUG mode: forcing onboarding off (but respecting session)")
                false
            } else {
                onboardingRepository.isCompleted()
            }*/
            
            Log.d(TAG, "onboardingCompleted: $onboardingCompleted")
            
            _startDestination.value = resolveAppStartDestination(
                onboardingCompleted = onboardingCompleted,
                isLoggedIn = isLoggedIn,
            )
            
            Log.d(TAG, "startDestination resolved: ${_startDestination.value}")
            
            if (isLoggedIn) {
                Log.d(TAG, "User is logged in, syncing habits...")
                syncHabits()
            }
        }
    }

    companion object {
        private const val TAG = "AppStartViewModel"
    }
}
