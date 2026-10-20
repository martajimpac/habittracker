package com.marta.habittracker.presentation.navigation

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LoginPromptCoordinator @Inject constructor() {

    private val pending = AtomicReference<LoginMessage?>(null)

    fun show(message: LoginMessage) {
        pending.set(message)
    }

    fun consume(): LoginMessage? = pending.getAndSet(null)
}

@HiltViewModel
class LoginPromptSender @Inject constructor(
    val coordinator: LoginPromptCoordinator,
) : ViewModel()
