package com.marta.habittracker.presentation.screens.preferences

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marta.habittracker.R
import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.coroutines.DispatchersProvider
import com.marta.habittracker.domain.model.AppError
import com.marta.habittracker.domain.model.Profile
import com.marta.habittracker.domain.repository.ProfileRepository
import com.marta.habittracker.domain.usecase.UpdateDisplayName
import com.marta.habittracker.presentation.utils.AvatarJpegEncoder
import com.marta.habittracker.presentation.utils.toUserMessageRes
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

sealed interface PreferencesEvent {
    data object NavigateBack : PreferencesEvent
    data class ShowError(@StringRes val messageRes: Int) : PreferencesEvent
}

data class PreferencesUiState(
    val displayName: String = "",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val avatarUrl: String? = null,
    val avatarColor: String = "",
)

@HiltViewModel
class PreferencesViewModel @Inject constructor(
    private val updateDisplayName: UpdateDisplayName,
    private val profileRepository: ProfileRepository,
    private val dispatchers: DispatchersProvider,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PreferencesUiState())
    val uiState: StateFlow<PreferencesUiState> = _uiState.asStateFlow()

    private val _events = Channel<PreferencesEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        refresh()
    }

    fun refresh() {
        loadProfile()
    }

    fun onDisplayNameChanged(displayName: String) {
        _uiState.update { it.copy(displayName = displayName) }
    }

    fun save() {
        if (_uiState.value.isSaving) return

        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            when (val result = updateDisplayName(_uiState.value.displayName)) {
                is DataResult.Success -> {
                    _uiState.update { it.copy(isSaving = false) }
                    _events.send(PreferencesEvent.NavigateBack)
                }

                is DataResult.Error -> {
                    _uiState.update { it.copy(isSaving = false) }
                    _events.send(PreferencesEvent.ShowError(result.error.toUserMessageRes()))
                }
            }
        }
    }

    fun onAvatarPicked(uri: Uri) {
        if (_uiState.value.isSaving) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                val bytes = withContext(dispatchers.io) {
                    AvatarJpegEncoder.encode(appContext.contentResolver, uri)
                }
                applyAvatarResult(profileRepository.uploadAvatar(bytes))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "encode avatar jpeg failed", e)
                _uiState.update { it.copy(isSaving = false) }
                _events.send(PreferencesEvent.ShowError(R.string.error_common_unknown))
            }
        }
    }

    fun onAvatarBytes(jpegBytes: ByteArray) {
        if (_uiState.value.isSaving) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            applyAvatarResult(profileRepository.uploadAvatar(jpegBytes))
        }
    }

    fun onRemovePhoto() {
        if (_uiState.value.isSaving) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            applyAvatarResult(profileRepository.clearAvatar())
        }
    }

    private suspend fun applyAvatarResult(result: DataResult<Profile, AppError>) {
        when (result) {
            is DataResult.Success -> {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        avatarUrl = result.data.avatarUrl,
                        avatarColor = result.data.avatarColor,
                    )
                }
            }

            is DataResult.Error -> {
                _uiState.update { it.copy(isSaving = false) }
                _events.send(PreferencesEvent.ShowError(result.error.toUserMessageRes()))
            }
        }
    }

    private fun loadProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = profileRepository.getMyProfile()) {
                is DataResult.Success -> {
                    _uiState.update {
                        it.copy(
                            displayName = result.data.displayName,
                            avatarUrl = result.data.avatarUrl,
                            avatarColor = result.data.avatarColor,
                            isLoading = false,
                        )
                    }
                }

                is DataResult.Error -> {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    companion object {
        private const val TAG = "PreferencesVM"
    }
}
