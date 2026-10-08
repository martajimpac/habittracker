package com.marta.habittracker.presentation.auth

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class AuthDeeplinkViewModel @Inject constructor(
    coordinator: AuthDeeplinkCoordinator,
) : ViewModel() {
    val events: Flow<AuthDeeplinkEvent> = coordinator.events
}
