package com.marta.habittracker.presentation.auth

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthDeeplinkCoordinator @Inject constructor() {

    private val _events = MutableSharedFlow<AuthDeeplinkEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<AuthDeeplinkEvent> = _events.asSharedFlow()

    fun onResetLinkHandled() {
        _events.tryEmit(AuthDeeplinkEvent.NavigateToResetPassword)
    }

    fun onResetLinkFailed() {
        _events.tryEmit(AuthDeeplinkEvent.ResetLinkFailed)
    }
}

sealed interface AuthDeeplinkEvent {
    data object NavigateToResetPassword : AuthDeeplinkEvent
    data object ResetLinkFailed : AuthDeeplinkEvent
}
