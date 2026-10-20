package com.marta.habittracker.presentation.screens.auth.register

import android.util.Patterns
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marta.habittracker.R
import com.marta.habittracker.core.push.DeviceTokenRegistration
import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.PasswordValidator
import com.marta.habittracker.domain.model.RegisterError
import com.marta.habittracker.domain.model.UsernameRules
import com.marta.habittracker.domain.repository.UsernameAvailabilityRepository
import com.marta.habittracker.domain.usecase.RegisterUseCase
import com.marta.habittracker.presentation.utils.toUserMessageRes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class RegisterField {
    Name,
    Username,
    Email,
    Password,
    Terms,
}

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val registerUseCase: RegisterUseCase,
    private val usernameAvailability: UsernameAvailabilityRepository,
    private val deviceTokenRegistration: DeviceTokenRegistration,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    private val _navigateToHome = Channel<Unit>(Channel.BUFFERED)
    val navigateToHome = _navigateToHome.receiveAsFlow()

    private val _accountCreated = Channel<Unit>(Channel.BUFFERED)
    val accountCreated = _accountCreated.receiveAsFlow()

    private val _focusField = Channel<RegisterField>(Channel.BUFFERED)
    val focusField = _focusField.receiveAsFlow()

    private var usernameCheckJob: Job? = null

    fun onNameChanged(name: String) {
        _uiState.update { it.copy(name = name, errorMessageRes = null, nameErrorRes = null) }
        verifyForm()
    }

    fun onUsernameChanged(username: String) {
        val normalized = UsernameRules.normalize(username)
        usernameCheckJob?.cancel()
        _uiState.update {
            it.copy(
                username = normalized,
                usernameAvailable = false,
                usernameSuggestions = emptyList(),
                usernameErrorRes = null,
                isCheckingUsername = false,
                errorMessageRes = null,
            )
        }
        verifyForm()
        if (!UsernameRules.isValid(normalized)) {
            if (normalized.isNotEmpty()) {
                _uiState.update { it.copy(usernameErrorRes = R.string.error_register_username_format) }
            }
            return
        }
        usernameCheckJob = viewModelScope.launch {
            _uiState.update { it.copy(isCheckingUsername = true) }
            delay(USERNAME_CHECK_DELAY_MS)
            when (val result = usernameAvailability.check(normalized)) {
                is DataResult.Success -> {
                    if (_uiState.value.username != normalized) return@launch
                    _uiState.update {
                        it.copy(
                            isCheckingUsername = false,
                            usernameAvailable = result.data.available,
                            usernameSuggestions = if (result.data.available) {
                                emptyList()
                            } else {
                                result.data.suggestions
                            },
                            usernameErrorRes = if (result.data.available) {
                                null
                            } else {
                                R.string.error_register_username_taken
                            },
                        )
                    }
                    verifyForm()
                }
                is DataResult.Error -> {
                    if (_uiState.value.username != normalized) return@launch
                    _uiState.update {
                        it.copy(
                            isCheckingUsername = false,
                            usernameAvailable = false,
                            usernameErrorRes = result.error.toUserMessageRes(),
                        )
                    }
                    verifyForm()
                }
            }
        }
    }

    fun onUsernameSuggestionSelected(suggestion: String) {
        onUsernameChanged(suggestion)
    }

    fun onEmailChanged(email: String) {
        _uiState.update { it.copy(email = email, errorMessageRes = null, emailErrorRes = null) }
        verifyForm()
    }

    fun onPasswordChanged(password: String) {
        _uiState.update {
            it.copy(
                password = password,
                errorMessageRes = null,
                passwordErrorRes = passwordError(password, reportEmpty = false),
            )
        }
        verifyForm()
    }

    fun onTogglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun onTermsChecked(checked: Boolean) {
        _uiState.update { it.copy(termsAccepted = checked, termsErrorRes = null) }
        verifyForm()
    }

    fun onRegisterClicked() {
        val state = _uiState.value
        if (state.isLoading) return
        if (!state.isRegisterEnabled) {
            showMissingFields(state)
            return
        }
        _uiState.update { it.copy(isLoading = true, errorMessageRes = null) }
        viewModelScope.launch {
            when (val result = registerUseCase(state.email.trim(), state.password, state.username)) {
                is DataResult.Success -> {
                    deviceTokenRegistration.registerCurrentToken()
                    _uiState.update { it.copy(isLoading = false) }
                    _navigateToHome.send(Unit)
                }
                is DataResult.Error -> {
                    if (result.error is RegisterError.EmailConfirmationRequired) {
                        _uiState.update { it.copy(isLoading = false, errorMessageRes = null) }
                        _accountCreated.send(Unit)
                    } else {
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

    private fun showMissingFields(state: RegisterUiState) {
        val nameError = if (state.name.isBlank()) R.string.error_register_name_required else null
        val emailError = if (!Patterns.EMAIL_ADDRESS.matcher(state.email).matches()) {
            R.string.error_register_invalid_email
        } else {
            null
        }
        val termsError = if (!state.termsAccepted) R.string.error_register_terms_required else null
        val usernameError = when {
            state.username.isBlank() -> R.string.error_register_username_required
            !UsernameRules.isValid(state.username) || !state.usernameAvailable ->
                state.usernameErrorRes ?: R.string.error_register_username_taken
            else -> null
        }
        _uiState.update {
            it.copy(
                nameErrorRes = nameError,
                usernameErrorRes = usernameError,
                emailErrorRes = emailError,
                passwordErrorRes = passwordError(state.password, reportEmpty = true),
                termsErrorRes = termsError,
            )
        }
        val field = when {
            nameError != null -> RegisterField.Name
            usernameError != null -> RegisterField.Username
            emailError != null -> RegisterField.Email
            !PasswordValidator.isRegistrationValid(state.password) -> RegisterField.Password
            termsError != null -> RegisterField.Terms
            else -> return
        }
        viewModelScope.launch { _focusField.send(field) }
    }

    private fun verifyForm() {
        val state = _uiState.value
        val enabled = state.name.isNotBlank() &&
            state.usernameAvailable &&
            Patterns.EMAIL_ADDRESS.matcher(state.email).matches() &&
            PasswordValidator.isRegistrationValid(state.password) &&
            state.termsAccepted
        _uiState.update { it.copy(isRegisterEnabled = enabled) }
    }

    private fun passwordError(password: String, reportEmpty: Boolean): Int? =
        when {
            password.isEmpty() -> if (reportEmpty) R.string.error_register_weak_password else null
            PasswordValidator.isRegistrationValid(password) -> null
            else -> R.string.error_register_weak_password
        }

    private companion object {
        const val USERNAME_CHECK_DELAY_MS = 350L
    }
}

data class RegisterUiState(
    val name: String = "",
    val username: String = "",
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val isRegisterEnabled: Boolean = false,
    val isPasswordVisible: Boolean = false,
    val termsAccepted: Boolean = false,
    val usernameAvailable: Boolean = false,
    val isCheckingUsername: Boolean = false,
    val usernameSuggestions: List<String> = emptyList(),
    @StringRes val errorMessageRes: Int? = null,
    @StringRes val nameErrorRes: Int? = null,
    @StringRes val usernameErrorRes: Int? = null,
    @StringRes val emailErrorRes: Int? = null,
    @StringRes val termsErrorRes: Int? = null,
    @StringRes val passwordErrorRes: Int? = null,
) {
    override fun toString(): String {
        return "RegisterUiState(isLoading=$isLoading, isRegisterEnabled=$isRegisterEnabled, " +
            "isPasswordVisible=$isPasswordVisible, termsAccepted=$termsAccepted, " +
            "usernameAvailable=$usernameAvailable, isCheckingUsername=$isCheckingUsername, " +
            "suggestionCount=${usernameSuggestions.size}, " +
            "hasError=${errorMessageRes != null}, hasPasswordError=${passwordErrorRes != null}, " +
            "hasNameError=${nameErrorRes != null}, hasUsernameError=${usernameErrorRes != null}, " +
            "hasEmailError=${emailErrorRes != null}, hasTermsError=${termsErrorRes != null})"
    }
}
