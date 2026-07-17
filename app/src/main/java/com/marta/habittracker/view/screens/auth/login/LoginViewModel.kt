package com.marta.habittracker.view.screens.auth.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Patterns
import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.model.User
import com.marta.habittracker.domain.model.toUserMessage
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
        _uiState.update { state ->
            state.copy(email = email, errorMessage = null)
        }
        verifyLogin()
    }

    fun onPasswordChanged(password: String) {
        _uiState.update { state ->
            state.copy(password = password, errorMessage = null)
        }
        verifyLogin()
    }

    fun onLoginClicked() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = loginUseCase(_uiState.value.email, _uiState.value.password)) {
                is DataResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isUserLogged = true,
                            loggedUser = result.data,
                            errorMessage = null,
                        )
                    }
                }

                is DataResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isUserLogged = false,
                            loggedUser = null,
                            errorMessage = result.error.toUserMessage(),
                        )
                    }
                }
            }
        }
    }

    private fun verifyLogin() {
        val enabledLogin =
            isEmailValid(_uiState.value.email) && isPasswordValid(_uiState.value.password)
        _uiState.update {
            it.copy(isLoginEnabled = enabledLogin)
        }
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
    val loggedUser: User? = null,
    val errorMessage: String? = null,
) {
    override fun toString(): String =
        "LoginUiState(isLoading=$isLoading, isLoginEnabled=$isLoginEnabled, " +
            "isUserLogged=$isUserLogged, loggedUser=${loggedUser?.userId}, errorMessage=$errorMessage)"
}
