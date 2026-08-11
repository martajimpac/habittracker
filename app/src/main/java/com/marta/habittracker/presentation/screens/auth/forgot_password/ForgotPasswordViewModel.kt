package com.marta.habittracker.presentation.screens.auth.forgot_password

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marta.habittracker.R
import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.EmailValidator
import com.marta.habittracker.domain.model.AppError
import com.marta.habittracker.domain.usecase.RequestPasswordReset
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
class ForgotPasswordViewModel @Inject constructor(
    private val requestPasswordReset: RequestPasswordReset,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ForgotPasswordUiState())
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState.asStateFlow()

    private val _navigateToLogin = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val navigateToLogin: SharedFlow<Unit> = _navigateToLogin.asSharedFlow()

    fun onEmailChanged(email: String) {
        _uiState.update {
            it.copy(
                email = email,
                isSubmitEnabled = EmailValidator.isValid(email),
                errorMessageRes = null,
            )
        }
    }

    fun submit() {
        val email = _uiState.value.email
        if (_uiState.value.isLoading || !EmailValidator.isValid(email)) return

        _uiState.update { it.copy(isLoading = true, errorMessageRes = null) }
        viewModelScope.launch {
            when (val result = requestPasswordReset(email)) {
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

data class ForgotPasswordUiState(
    val email: String = "",
    val isLoading: Boolean = false,
    val isSubmitEnabled: Boolean = false,
    @StringRes val errorMessageRes: Int? = null,
)

private fun AppError.toUserMessageRes(): Int = when (this) {
    AppError.Common.Network -> R.string.error_common_network
    AppError.Common.Unauthorized -> R.string.error_common_unauthorized
    AppError.Common.Unknown -> R.string.error_common_unknown
    else -> R.string.error_common_unknown
}
