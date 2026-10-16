package com.marta.habittracker.presentation.screens.challenge

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marta.habittracker.R
import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.model.AppError
import com.marta.habittracker.domain.model.ChallengeCard
import com.marta.habittracker.domain.model.ChallengeStatus
import com.marta.habittracker.domain.repository.AuthRepository
import com.marta.habittracker.domain.repository.FriendsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChallengeDetailUiState(
    val isLoading: Boolean = true,
    val card: ChallengeCard? = null,
    @StringRes val errorRes: Int? = null,
    val canRespond: Boolean = false,
    val isSubmitting: Boolean = false,
)

sealed interface ChallengeDetailEvent {
    data object NavigateBack : ChallengeDetailEvent
}

@HiltViewModel
class ChallengeDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val friendsRepository: FriendsRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val challengeId: String = checkNotNull(savedStateHandle["challengeId"])

    private val _uiState = MutableStateFlow(ChallengeDetailUiState())
    val uiState: StateFlow<ChallengeDetailUiState> = _uiState.asStateFlow()

    private val _events = Channel<ChallengeDetailEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        loadChallenge()
    }

    fun onAccept() {
        respond(accept = true)
    }

    fun onDecline() {
        respond(accept = false)
    }

    private fun loadChallenge() {
        viewModelScope.launch {
            when (val result = friendsRepository.getChallenge(challengeId)) {
                is DataResult.Success -> {
                    val userId = authRepository.getCurrentUserId()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            card = result.data,
                            canRespond = result.data.canRespond(userId),
                            errorRes = null,
                        )
                    }
                }
                is DataResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorRes = result.error.toLoadErrorRes(),
                        )
                    }
                }
            }
        }
    }

    private fun respond(accept: Boolean) {
        val state = _uiState.value
        if (!state.canRespond || state.isSubmitting) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorRes = null) }
            when (val result = friendsRepository.respondToChallenge(challengeId, accept)) {
                is DataResult.Success -> {
                    val userId = authRepository.getCurrentUserId()
                    val card = result.data
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            card = card,
                            canRespond = card.canRespond(userId),
                        )
                    }
                    if (!accept) {
                        _events.send(ChallengeDetailEvent.NavigateBack)
                    }
                }
                is DataResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            errorRes = result.error.toRespondErrorRes(),
                        )
                    }
                }
            }
        }
    }

    private fun ChallengeCard.canRespond(userId: String?): Boolean =
        challenge.status == ChallengeStatus.Pending && challenge.challengedId == userId
}

@StringRes
private fun AppError.toLoadErrorRes(): Int = when (this) {
    AppError.Common.Network -> R.string.error_no_internet
    else -> R.string.challenge_detail_error
}

@StringRes
private fun AppError.toRespondErrorRes(): Int = when (this) {
    AppError.Common.Network -> R.string.error_no_internet
    else -> R.string.challenge_respond_error
}
