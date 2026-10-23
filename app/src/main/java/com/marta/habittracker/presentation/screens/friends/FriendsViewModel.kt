package com.marta.habittracker.presentation.screens.friends

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marta.habittracker.R
import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.model.AppError
import com.marta.habittracker.domain.model.ChallengeCard
import com.marta.habittracker.domain.model.ChallengeCriteria
import com.marta.habittracker.domain.model.ChallengeHabitDraft
import com.marta.habittracker.domain.model.FriendListItem
import com.marta.habittracker.domain.model.PendingFriendRequest
import com.marta.habittracker.domain.model.Habit
import com.marta.habittracker.domain.model.Profile
import com.marta.habittracker.domain.push.ChallengeDeclineNotifier
import com.marta.habittracker.domain.push.SilentChallengeDeclineNotifier
import com.marta.habittracker.domain.repository.FriendsRepository
import com.marta.habittracker.domain.repository.HabitRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FriendsUiState(
    val isLoading: Boolean = true,
    val friends: List<FriendListItem> = emptyList(),
    val challenges: List<ChallengeCard> = emptyList(),
    val pendingRequests: List<PendingFriendRequest> = emptyList(),
    val sheet: FriendsSheet = FriendsSheet.None,
    val searchQuery: String = "",
    val searchResults: List<Profile> = emptyList(),
    val selectedFriendHabits: List<Habit> = emptyList(),
    val myHabits: List<Habit> = emptyList(),
    @StringRes val errorRes: Int? = null,
)

sealed interface FriendsEvent {
    data object RequestSent : FriendsEvent
}

sealed interface FriendsSheet {
    data object None : FriendsSheet
    data object AddFriend : FriendsSheet
    data class ViewFriend(val friendId: String) : FriendsSheet
    data class CreateChallenge(val friendId: String) : FriendsSheet
}

@HiltViewModel
class FriendsViewModel @Inject constructor(
    private val friendsRepository: FriendsRepository,
    habitRepository: HabitRepository,
    private val challengeDeclineNotifier: ChallengeDeclineNotifier = SilentChallengeDeclineNotifier,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FriendsUiState())
    val uiState: StateFlow<FriendsUiState> = _uiState.asStateFlow()

    private val _events = Channel<FriendsEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            habitRepository.getAllHabitsWithRecords().collect { habits ->
                _uiState.update { it.copy(myHabits = habits) }
            }
        }
        refresh()
        viewModelScope.launch {
            challengeDeclineNotifier.declinedIds.collect { challengeId ->
                _uiState.update { state ->
                    state.copy(
                        challenges = state.challenges.filter { it.challenge.id != challengeId },
                    )
                }
                loadContent()
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            loadContent()
        }
    }

    fun onAddFriendClicked() {
        _uiState.update {
            it.copy(
                sheet = FriendsSheet.AddFriend,
                searchQuery = "",
                searchResults = emptyList(),
                errorRes = null,
            )
        }
    }

    fun onViewFriendClicked(friendId: String) {
        openFriendSheet(FriendsSheet.ViewFriend(friendId), friendId)
    }

    fun onCreateChallengeClicked(friendId: String) {
        _uiState.update {
            it.copy(
                sheet = FriendsSheet.CreateChallenge(friendId),
                errorRes = null,
            )
        }
    }

    fun onDismissSheet() {
        _uiState.update {
            it.copy(
                sheet = FriendsSheet.None,
                searchQuery = "",
                searchResults = emptyList(),
                selectedFriendHabits = emptyList(),
                errorRes = null,
            )
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update {
            it.copy(
                searchQuery = query,
                searchResults = if (query.isBlank()) emptyList() else it.searchResults,
                errorRes = null,
            )
        }
        if (query.isBlank()) return

        viewModelScope.launch {
            when (val result = friendsRepository.searchProfiles(query.trim())) {
                is DataResult.Success -> {
                    if (_uiState.value.searchQuery == query) {
                        _uiState.update { it.copy(searchResults = result.data) }
                    }
                }
                is DataResult.Error -> setError(result.error)
            }
        }
    }

    fun sendFriendRequest(addresseeId: String) {
        viewModelScope.launch {
            when (val result = friendsRepository.sendFriendRequest(addresseeId)) {
                is DataResult.Success -> {
                    _uiState.update { it.copy(errorRes = null) }
                    _events.send(FriendsEvent.RequestSent)
                }
                is DataResult.Error -> setError(result.error)
            }
        }
    }

    fun respondToRequest(
        friendshipId: String,
        accept: Boolean,
    ) {
        viewModelScope.launch {
            when (val result = friendsRepository.respondToFriendRequest(friendshipId, accept)) {
                is DataResult.Success -> loadContent()
                is DataResult.Error -> setError(result.error)
            }
        }
    }

    fun createChallenge(
        challengedId: String,
        draft: ChallengeHabitDraft,
        criteria: ChallengeCriteria,
        durationDays: Int,
    ) {
        viewModelScope.launch {
            when (
                val result = friendsRepository.createChallenge(
                    challengedId = challengedId,
                    draft = draft,
                    criteria = criteria,
                    durationDays = durationDays,
                )
            ) {
                is DataResult.Success -> {
                    _uiState.update { it.copy(sheet = FriendsSheet.None, errorRes = null) }
                    loadContent()
                }
                is DataResult.Error -> setError(result.error)
            }
        }
    }

    private suspend fun loadContent() {
        _uiState.update { it.copy(isLoading = true, errorRes = null) }

        val friends = when (val result = friendsRepository.getAcceptedFriends()) {
            is DataResult.Success -> result.data
            is DataResult.Error -> return finishLoadingWithError(result.error)
        }
        val challenges = when (val result = friendsRepository.getActiveChallenges()) {
            is DataResult.Success -> result.data
            is DataResult.Error -> return finishLoadingWithError(result.error)
        }
        val pendingRequests = when (val result = friendsRepository.getPendingFriendRequests()) {
            is DataResult.Success -> result.data
            is DataResult.Error -> return finishLoadingWithError(result.error)
        }

        _uiState.update {
            it.copy(
                isLoading = false,
                friends = friends,
                challenges = challenges,
                pendingRequests = pendingRequests,
            )
        }
    }

    private fun finishLoadingWithError(error: AppError) {
        _uiState.update { it.copy(isLoading = false, errorRes = error.toStringRes()) }
    }

    private fun openFriendSheet(
        sheet: FriendsSheet,
        friendId: String,
    ) {
        _uiState.update {
            it.copy(
                sheet = sheet,
                selectedFriendHabits = emptyList(),
                errorRes = null,
            )
        }
        viewModelScope.launch {
            when (val result = friendsRepository.getPublicHabitsForFriend(friendId)) {
                is DataResult.Success -> {
                    if (_uiState.value.sheet.friendIdOrNull() == friendId) {
                        _uiState.update { it.copy(selectedFriendHabits = result.data) }
                    }
                }
                is DataResult.Error -> {
                    if (_uiState.value.sheet.friendIdOrNull() == friendId) {
                        setError(result.error)
                    }
                }
            }
        }
    }

    private fun setError(error: AppError) {
        _uiState.update { it.copy(errorRes = error.toStringRes()) }
    }
}

private fun FriendsSheet.friendIdOrNull(): String? = when (this) {
    is FriendsSheet.ViewFriend -> friendId
    is FriendsSheet.CreateChallenge -> friendId
    else -> null
}

@StringRes
private fun AppError.toStringRes(): Int = when (this) {
    AppError.Common.Network -> R.string.error_no_internet
    AppError.Common.Unauthorized -> R.string.error_common_unauthorized
    AppError.Common.Unknown -> R.string.error_common_unknown
    else -> R.string.error_common_unknown
}
