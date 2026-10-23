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
import com.marta.habittracker.domain.model.ownChallengeHabitId
import com.marta.habittracker.domain.push.ChallengeDeclineNotifier
import com.marta.habittracker.domain.push.SilentChallengeDeclineNotifier
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
    val canEditName: Boolean = false,
    val canDelete: Boolean = false,
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
    private val challengeDeclineNotifier: ChallengeDeclineNotifier = SilentChallengeDeclineNotifier,
) : ViewModel() {

    private val challengeId: String = checkNotNull(savedStateHandle["challengeId"])

    private val _uiState = MutableStateFlow(ChallengeDetailUiState())
    val uiState: StateFlow<ChallengeDetailUiState> = _uiState.asStateFlow()

    private val _events = Channel<ChallengeDetailEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        loadChallenge()
        viewModelScope.launch {
            challengeDeclineNotifier.declinedIds.collect { declinedId ->
                if (declinedId == challengeId) {
                    _events.send(ChallengeDetailEvent.NavigateBack)
                }
            }
        }
    }

    fun onAccept() {
        respond(accept = true)
    }

    fun onDecline() {
        respond(accept = false)
    }

    fun onRename(name: String) {
        val state = _uiState.value
        if (!state.canEditName || state.isSubmitting) return
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            _uiState.update { it.copy(errorRes = R.string.challenge_name_required) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorRes = null) }
            when (val result = friendsRepository.renameChallenge(challengeId, trimmed)) {
                is DataResult.Success -> applyCard(result.data)
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

    fun onDelete() {
        val state = _uiState.value
        if (!state.canDelete || state.isSubmitting) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorRes = null) }
            when (val result = friendsRepository.cancelChallenge(challengeId)) {
                is DataResult.Success -> {
                    _uiState.update { it.copy(isSubmitting = false) }
                    _events.send(ChallengeDetailEvent.NavigateBack)
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
                            canEditName = result.data.canEditName(userId),
                            canDelete = result.data.canDelete(),
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
                    applyCard(result.data)
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

    private suspend fun applyCard(card: ChallengeCard) {
        val userId = authRepository.getCurrentUserId()
        _uiState.update {
            it.copy(
                isLoading = false,
                isSubmitting = false,
                card = card,
                canRespond = card.canRespond(userId),
                canEditName = card.canEditName(userId),
                canDelete = card.canDelete(),
                errorRes = null,
            )
        }
    }

    private fun ChallengeCard.canRespond(userId: String?): Boolean =
        challenge.status == ChallengeStatus.Pending && challenge.challengedId == userId

    private fun ChallengeCard.canEditName(userId: String?): Boolean {
        if (userId == null) return false
        return ownChallengeHabitId(
            userId = userId,
            challengerId = challenge.challengerId,
            challengedId = challenge.challengedId,
            challengerHabitId = challenge.challengerHabitId,
            challengedHabitId = challenge.challengedHabitId,
        ) != null
    }

    private fun ChallengeCard.canDelete(): Boolean =
        challenge.status == ChallengeStatus.Pending || challenge.status == ChallengeStatus.Active
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
