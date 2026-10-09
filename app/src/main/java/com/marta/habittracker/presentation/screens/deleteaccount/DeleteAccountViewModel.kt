package com.marta.habittracker.presentation.screens.deleteaccount

import android.content.Context
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marta.habittracker.R
import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.repository.ProfileRepository
import com.marta.habittracker.presentation.utils.toUserMessageRes
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface DeleteAccountEvent {
    data object AccountDeleted : DeleteAccountEvent
    data class ShowError(@StringRes val messageRes: Int) : DeleteAccountEvent
}

data class DeleteAccountUiState(
    val confirmation: String = "",
    val isDeleteEnabled: Boolean = false,
    val isDeleting: Boolean = false,
)

@HiltViewModel
class DeleteAccountViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    private val confirmToken: String =
        appContext.getString(R.string.preferences_delete_confirm_token)

    private val _uiState = MutableStateFlow(DeleteAccountUiState())
    val uiState: StateFlow<DeleteAccountUiState> = _uiState.asStateFlow()

    private val _events = Channel<DeleteAccountEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun onConfirmationChanged(value: String) {
        _uiState.update {
            it.copy(
                confirmation = value,
                isDeleteEnabled = value.trim() == confirmToken,
            )
        }
    }

    fun onDeleteConfirmed() {
        val state = _uiState.value
        if (!state.isDeleteEnabled || state.isDeleting) return

        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true) }
            when (val result = profileRepository.deleteAccount()) {
                is DataResult.Success -> {
                    _uiState.update { it.copy(isDeleting = false) }
                    _events.send(DeleteAccountEvent.AccountDeleted)
                }

                is DataResult.Error -> {
                    _uiState.update { it.copy(isDeleting = false) }
                    _events.send(DeleteAccountEvent.ShowError(result.error.toUserMessageRes()))
                }
            }
        }
    }
}
