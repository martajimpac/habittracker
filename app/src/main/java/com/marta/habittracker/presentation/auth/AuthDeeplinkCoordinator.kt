package com.marta.habittracker.presentation.auth

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthDeeplinkCoordinator @Inject constructor() {

    private val _events = Channel<AuthDeeplinkEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun onResetLinkHandled() {
        _events.trySend(AuthDeeplinkEvent.NavigateToResetPassword)
    }

    fun onResetLinkFailed() {
        _events.trySend(AuthDeeplinkEvent.ResetLinkFailed)
    }

    fun onEmailConfirmed(signedIn: Boolean) {
        _events.trySend(
            if (signedIn) {
                AuthDeeplinkEvent.NavigateHome
            } else {
                AuthDeeplinkEvent.EmailConfirmed
            },
        )
    }

    fun onEmailConfirmFailed() {
        _events.trySend(AuthDeeplinkEvent.EmailConfirmFailed)
    }
}

sealed interface AuthDeeplinkEvent {
    data object NavigateToResetPassword : AuthDeeplinkEvent
    data object ResetLinkFailed : AuthDeeplinkEvent
    data object NavigateHome : AuthDeeplinkEvent
    data object EmailConfirmed : AuthDeeplinkEvent
    data object EmailConfirmFailed : AuthDeeplinkEvent
}
