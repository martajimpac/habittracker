package com.marta.habittracker.presentation.screens.auth.login

import android.util.Patterns
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.BuildConfig
import com.marta.habittracker.R
import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.usecase.LoginUseCase
import com.marta.habittracker.domain.usecase.SyncHabits
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
    private val syncHabits: SyncHabits,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

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
            when (loginUseCase(_uiState.value.email, _uiState.value.password)) {
                is DataResult.Success -> {
                    syncHabits()
                    _uiState.update { it.copy(isUserLogged = true, isLoading = false) }
                }
                is DataResult.Error -> {
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
    val email: String = if(BuildConfig.DEBUG) "martajimpac@gmail.com" else "",
    val password: String = if(BuildConfig.DEBUG) "nalskd1A*" else "",
    val isLoading: Boolean = false,
    val isLoginEnabled: Boolean = false,
    val isUserLogged: Boolean = false,
    val isPasswordVisible: Boolean = false,
    @StringRes val errorMessageRes: Int? = null,
) {
    override fun toString(): String {
        return "LoginUiState(isLoading=$isLoading, isLoginEnabled=$isLoginEnabled, " +
            "isUserLogged=$isUserLogged, isPasswordVisible=$isPasswordVisible, " +
            "hasError=${errorMessageRes != null})"
    }
}
