package com.marta.habittracker.view.core.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.marta.habittracker.data.local.preferences.OnboardingPreferencesKeys
import com.marta.habittracker.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppStartViewModel @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _startDestination = MutableStateFlow<AppStartDestination?>(null)
    val startDestination: StateFlow<AppStartDestination?> = _startDestination.asStateFlow()

    init {
        viewModelScope.launch {
            val onboardingCompleted = dataStore.data
                .map { prefs -> prefs[OnboardingPreferencesKeys.ONBOARDING_COMPLETED] == true }
                .first()
            val isLoggedIn = authRepository.isLoggedIn()
            _startDestination.value = resolveAppStartDestination(
                onboardingCompleted = onboardingCompleted,
                isLoggedIn = isLoggedIn,
            )
        }
    }
}
