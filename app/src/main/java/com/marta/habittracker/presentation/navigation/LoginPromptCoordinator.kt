package com.marta.habittracker.presentation.navigation

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LoginPromptCoordinator @Inject constructor() {

    private val pending = AtomicReference<LoginArrival?>(null)

    fun show(
        message: LoginMessage? = null,
        email: String = "",
        password: String = "",
    ) {
        pending.set(LoginArrival(message = message, email = email, password = password))
    }

    fun consume(): LoginArrival? = pending.getAndSet(null)
}

data class LoginArrival(
    val message: LoginMessage? = null,
    val email: String = "",
    val password: String = "",
) {
    override fun toString(): String = "LoginArrival(message=$message, hasCredentials=${email.isNotEmpty()})"
}

@HiltViewModel
class LoginPromptSender @Inject constructor(
    val coordinator: LoginPromptCoordinator,
) : ViewModel()
