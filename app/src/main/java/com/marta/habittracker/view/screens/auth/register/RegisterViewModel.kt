package com.marta.habittracker.view.screens.auth.register

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    private val _navigateToHome = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val navigateToHome: SharedFlow<Unit> = _navigateToHome.asSharedFlow()

    fun onNameChanged(name: String) {
        _uiState.update { it.copy(name = name, errorMessage = null) }
        verifyForm()
    }

    fun onEmailChanged(email: String) {
        _uiState.update { it.copy(email = email, errorMessage = null) }
        verifyForm()
    }

    fun onPasswordChanged(password: String) {
        _uiState.update { it.copy(password = password, errorMessage = null) }
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
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            when (
                val result = authRepository.doRegister(
                    name = state.name.trim(),
                    email = state.email.trim(),
                    password = state.password,
                )
            ) {
                is DataResult.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _navigateToHome.emit(Unit)
                }
                is DataResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Unable to create account. Try again.",
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
            state.password.length >= 8 &&
            state.termsAccepted
        _uiState.update { it.copy(isRegisterEnabled = enabled) }
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
    val errorMessage: String? = null,
) {
    override fun toString(): String {
        return "RegisterUiState(isLoading=$isLoading, isRegisterEnabled=$isRegisterEnabled, " +
            "isPasswordVisible=$isPasswordVisible, termsAccepted=$termsAccepted, " +
            "hasError=${errorMessage != null})"
    }
}
