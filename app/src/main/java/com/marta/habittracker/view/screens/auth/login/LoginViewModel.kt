package com.marta.habittracker.view.screens.auth.login

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.usecase.LoginUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEmailChanged(email: String) {
        _uiState.update { it.copy(email = email, errorMessage = null) }
        verifyLogin()
    }

    fun onPasswordChanged(password: String) {
        _uiState.update { it.copy(password = password, errorMessage = null) }
        verifyLogin()
    }

    fun onTogglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun onClickSelected() {
        if (!_uiState.value.isLoginEnabled || _uiState.value.isLoading) return
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            when (val response = loginUseCase(_uiState.value.email, _uiState.value.password)) {
                is DataResult.Success -> {
                    _uiState.update { it.copy(isUserLogged = true, isLoading = false) }
                }
                is DataResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Unable to sign in. Check your credentials.",
                        )
                    }
                }
            }
        }
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
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val isLoginEnabled: Boolean = false,
    val isUserLogged: Boolean = false,
    val isPasswordVisible: Boolean = false,
    val errorMessage: String? = null,
) {
    override fun toString(): String {
        return "LoginUiState(isLoading=$isLoading, isLoginEnabled=$isLoginEnabled, " +
            "isUserLogged=$isUserLogged, isPasswordVisible=$isPasswordVisible, " +
            "hasError=${errorMessage != null})"
    }
}
