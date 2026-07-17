package com.marta.habittracker.view.screens.auth.register

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.PasswordValidator
import com.marta.habittracker.domain.model.RegisterError
import com.marta.habittracker.domain.model.User
import com.marta.habittracker.domain.model.toUserMessage
import com.marta.habittracker.domain.usecase.RegisterUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val registerUseCase: RegisterUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun onInputChanged(value: String) {
        _uiState.update { state ->
            state.copy(inputValue = value, errorMessage = null, infoMessage = null)
        }
        verifyRegister()
    }

    fun onPasswordChanged(password: String) {
        _uiState.update { state ->
            state.copy(password = password, errorMessage = null, infoMessage = null)
        }
        verifyRegister()
    }

    fun onChangeMode() {
        _uiState.update {
            it.copy(
                mode = if (it.mode == MY_MODE.EMAIL) MY_MODE.PHONE else MY_MODE.EMAIL,
                inputValue = "",
                password = "",
                errorMessage = null,
                infoMessage = null,
            )
        }
        verifyRegister()
    }

    fun onRegisterClicked() {
        if (_uiState.value.mode != MY_MODE.EMAIL) return

        _uiState.update { it.copy(isLoading = true, errorMessage = null, infoMessage = null) }
        viewModelScope.launch {
            when (
                val result = registerUseCase(
                    _uiState.value.inputValue,
                    _uiState.value.password,
                )
            ) {
                is DataResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isUserRegistered = true,
                            registeredUser = result.data,
                            errorMessage = null,
                            infoMessage = null,
                        )
                    }
                }

                is DataResult.Error -> {
                    val needsEmailConfirmation = result.error == RegisterError.EmailConfirmationRequired
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isUserRegistered = needsEmailConfirmation,
                            registeredUser = null,
                            errorMessage = if (needsEmailConfirmation) null else result.error.toUserMessage(),
                            infoMessage = if (needsEmailConfirmation) result.error.toUserMessage() else null,
                        )
                    }
                }
            }
        }
    }

    private fun verifyRegister() {
        val state = _uiState.value
        val enabled = when (state.mode) {
            MY_MODE.EMAIL -> isEmailValid(state.inputValue) && PasswordValidator.isValid(state.password)
            MY_MODE.PHONE -> false
        }
        _uiState.update { it.copy(isRegisterEnabled = enabled) }
    }

    private fun isEmailValid(email: String): Boolean =
        Patterns.EMAIL_ADDRESS.matcher(email).matches()
}

data class RegisterUiState(
    val inputValue: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val isRegisterEnabled: Boolean = false,
    val isUserRegistered: Boolean = false,
    val registeredUser: User? = null,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val mode: MY_MODE = MY_MODE.EMAIL,
) {
    override fun toString(): String =
        "RegisterUiState(isLoading=$isLoading, isRegisterEnabled=$isRegisterEnabled, " +
            "isUserRegistered=$isUserRegistered, registeredUser=${registeredUser?.userId}, " +
            "errorMessage=$errorMessage, infoMessage=$infoMessage, mode=$mode)"
}

enum class MY_MODE {
    PHONE,
    EMAIL,
}
