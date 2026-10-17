package com.marta.habittracker.presentation.screens.auth.register

import android.util.Patterns
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marta.habittracker.R
import com.marta.habittracker.core.push.DeviceTokenRegistration
import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.PasswordValidator
import com.marta.habittracker.domain.usecase.RegisterUseCase
import com.marta.habittracker.presentation.utils.toUserMessageRes
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
class RegisterViewModel @Inject constructor(
    private val registerUseCase: RegisterUseCase,
    private val deviceTokenRegistration: DeviceTokenRegistration,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    private val _navigateToHome = Channel<Unit>(Channel.BUFFERED)
    val navigateToHome = _navigateToHome.receiveAsFlow()

    fun onNameChanged(name: String) {
        _uiState.update { it.copy(name = name, errorMessageRes = null) }
        verifyForm()
    }

    fun onEmailChanged(email: String) {
        _uiState.update { it.copy(email = email, errorMessageRes = null) }
        verifyForm()
    }

    fun onPasswordChanged(password: String) {
        _uiState.update {
            it.copy(
                password = password,
                errorMessageRes = null,
                passwordErrorRes = passwordError(password),
            )
        }
        verifyForm()
    }

    fun onTogglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun onTermsChecked(checked: Boolean) {
        _uiState.update { it.copy(termsAccepted = checked) }
        verifyForm()
    }

    fun onRegisterClicked() {
        val state = _uiState.value
        if (!state.isRegisterEnabled || state.isLoading) return
        _uiState.update { it.copy(isLoading = true, errorMessageRes = null) }
        viewModelScope.launch {
            when (val result = registerUseCase(state.email.trim(), state.password)) {
                is DataResult.Success -> {
                    deviceTokenRegistration.registerCurrentToken()
                    _uiState.update { it.copy(isLoading = false) }
                    _navigateToHome.send(Unit)
                }
                is DataResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessageRes = result.error.toUserMessageRes(),
                        )
                    }
                }
            }
        }
    }

    private fun verifyForm() {
        val state = _uiState.value
        val enabled = state.name.isNotBlank() &&
            Patterns.EMAIL_ADDRESS.matcher(state.email).matches() &&
            PasswordValidator.isRegistrationValid(state.password) &&
            state.termsAccepted
        _uiState.update { it.copy(isRegisterEnabled = enabled) }
    }

    private fun passwordError(password: String): Int? =
        if (PasswordValidator.isRegistrationValid(password)) {
            null
        } else {
            R.string.error_register_weak_password
        }
}

data class RegisterUiState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val isRegisterEnabled: Boolean = false,
    val isPasswordVisible: Boolean = false,
    val termsAccepted: Boolean = false,
    @StringRes val errorMessageRes: Int? = null,
    @StringRes val passwordErrorRes: Int? = R.string.error_register_weak_password,
) {
    override fun toString(): String {
        return "RegisterUiState(isLoading=$isLoading, isRegisterEnabled=$isRegisterEnabled, " +
            "isPasswordVisible=$isPasswordVisible, termsAccepted=$termsAccepted, " +
            "hasError=${errorMessageRes != null}, hasPasswordError=${passwordErrorRes != null})"
    }
}
