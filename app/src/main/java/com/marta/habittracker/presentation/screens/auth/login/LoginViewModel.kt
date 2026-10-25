package com.marta.habittracker.presentation.screens.auth.login

import android.util.Patterns
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.BuildConfig
import com.marta.habittracker.R
import com.marta.habittracker.core.push.DeviceTokenRegistration
import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.model.LoginError
import com.marta.habittracker.domain.usecase.LoginUseCase
import com.marta.habittracker.domain.usecase.SyncHabits
import com.marta.habittracker.presentation.navigation.LoginArrival
import com.marta.habittracker.presentation.navigation.LoginMessage
import com.marta.habittracker.presentation.navigation.LoginPromptCoordinator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val syncHabits: SyncHabits,
    private val deviceTokenRegistration: DeviceTokenRegistration,
    promptCoordinator: LoginPromptCoordinator,
) : ViewModel() {

    private val arrival = promptCoordinator.consume()

    private val _uiState = MutableStateFlow(loginStateFrom(arrival))
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _prompt = MutableStateFlow(arrival?.message)
    val prompt: StateFlow<LoginMessage?> = _prompt.asStateFlow()

    fun dismissPrompt() {
        _prompt.value = null
    }

    private val _navigateToHome = Channel<Unit>(Channel.BUFFERED)
    val navigateToHome = _navigateToHome.receiveAsFlow()

    private val _events = Channel<LoginEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun onEmailChanged(email: String) {
        _uiState.update { it.copy(email = email, errorMessageRes = null) }
        verifyLogin()
    }

    fun onPasswordChanged(password: String) {
        _uiState.update { it.copy(password = password, errorMessageRes = null) }
        verifyLogin()
    }

    fun onTogglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun onClickSelected() {
        if (!_uiState.value.isLoginEnabled || _uiState.value.isLoading) return
        _uiState.update { it.copy(isLoading = true, errorMessageRes = null) }
        viewModelScope.launch {
            when (val result = loginUseCase(_uiState.value.email, _uiState.value.password)) {
                is DataResult.Success -> {
                    syncHabits()
                    deviceTokenRegistration.registerCurrentToken()
                    _uiState.update { it.copy(isLoading = false) }
                    _navigateToHome.send(Unit)
                }
                is DataResult.Error -> {
                    if (result.error is LoginError.EmailNotVerified) {
                        _uiState.update { it.copy(isLoading = false, errorMessageRes = null) }
                        _events.send(LoginEvent.AccountNotConfirmed)
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessageRes = R.string.login_error_unable_to_sign_in,
                            )
                        }
                    }
                }
            }
        }
    }

    private fun loginStateFrom(arrival: LoginArrival?): LoginUiState {
        val state = LoginUiState()
        if (arrival == null || (arrival.email.isEmpty() && arrival.password.isEmpty())) return state
        return state.copy(
            email = arrival.email,
            password = arrival.password,
            isLoginEnabled = isEmailValid(arrival.email) && isPasswordValid(arrival.password),
        )
    }

    private fun verifyLogin() {
        val enabled =
            isEmailValid(_uiState.value.email) && isPasswordValid(_uiState.value.password)
        _uiState.update { it.copy(isLoginEnabled = enabled) }
    }

    private fun isEmailValid(email: String): Boolean =
        Patterns.EMAIL_ADDRESS.matcher(email).matches()

    private fun isPasswordValid(password: String): Boolean = password.length >= 6
}

data class LoginUiState(
    val email: String = if (BuildConfig.DEBUG) "martajimpac@gmail.com" else "",
    val password: String = if (BuildConfig.DEBUG) "nalskd1A*" else "",
    val isLoading: Boolean = false,
    val isLoginEnabled: Boolean = false,
    val isPasswordVisible: Boolean = false,
    @StringRes val errorMessageRes: Int? = null,
) {
    override fun toString(): String {
        return "LoginUiState(isLoading=$isLoading, isLoginEnabled=$isLoginEnabled, " +
            "isPasswordVisible=$isPasswordVisible, hasError=${errorMessageRes != null})"
    }
}

sealed interface LoginEvent {
    data object AccountNotConfirmed : LoginEvent
}
