package com.marta.habittracker.presentation.screens.auth.reset_password

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marta.habittracker.R
import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.PasswordValidator
import com.marta.habittracker.domain.model.AppError
import com.marta.habittracker.domain.usecase.UpdatePassword
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
class ResetPasswordViewModel @Inject constructor(
    private val updatePassword: UpdatePassword,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ResetPasswordUiState())
    val uiState: StateFlow<ResetPasswordUiState> = _uiState.asStateFlow()

    private val _navigateToLogin = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val navigateToLogin: SharedFlow<Unit> = _navigateToLogin.asSharedFlow()

    fun onNewPasswordChanged(password: String) {
        _uiState.update {
            it.copy(
                newPassword = password,
                errorMessageRes = null,
            ).withValidation()
        }
    }

    fun onConfirmPasswordChanged(password: String) {
        _uiState.update {
            it.copy(
                confirmPassword = password,
                errorMessageRes = null,
            ).withValidation()
        }
    }

    fun submit() {
        val state = _uiState.value
        if (state.isLoading || !state.isSubmitEnabled) return

        _uiState.update { it.copy(isLoading = true, errorMessageRes = null) }
        viewModelScope.launch {
            when (val result = updatePassword(state.newPassword)) {
                is DataResult.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _navigateToLogin.emit(Unit)
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
}

data class ResetPasswordUiState(
    val newPassword: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val isSubmitEnabled: Boolean = false,
    @StringRes val errorMessageRes: Int? = null,
)

private fun ResetPasswordUiState.withValidation(): ResetPasswordUiState {
    val validPassword = PasswordValidator.isValid(newPassword)
    val passwordsMatch = newPassword == confirmPassword
    val validationError = when {
        confirmPassword.isNotEmpty() && !passwordsMatch ->
            R.string.reset_password_error_passwords_do_not_match

        newPassword.isNotEmpty() && !validPassword ->
            R.string.error_register_weak_password

        else -> null
    }
    return copy(
        isSubmitEnabled = validPassword && passwordsMatch,
        errorMessageRes = validationError,
    )
}

private fun AppError.toUserMessageRes(): Int = when (this) {
    AppError.Common.Network -> R.string.error_common_network
    AppError.Common.Unauthorized -> R.string.error_common_unauthorized
    AppError.Common.Unknown -> R.string.error_common_unknown
    else -> R.string.error_common_unknown
}
