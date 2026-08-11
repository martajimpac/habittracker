package com.marta.habittracker.presentation.screens.friends

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marta.habittracker.R
import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.model.AppError
import com.marta.habittracker.domain.model.ChallengeCard
import com.marta.habittracker.domain.model.ChallengeCriteria
import com.marta.habittracker.domain.model.FriendListItem
import com.marta.habittracker.domain.model.Friendship
import com.marta.habittracker.domain.model.Habit
import com.marta.habittracker.domain.model.Profile
import com.marta.habittracker.domain.repository.FriendsRepository
import com.marta.habittracker.domain.repository.HabitRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FriendsUiState(
    val isLoading: Boolean = true,
    val friends: List<FriendListItem> = emptyList(),
    val challenges: List<ChallengeCard> = emptyList(),
    val pendingRequests: List<Friendship> = emptyList(),
    val sheet: FriendsSheet = FriendsSheet.None,
    val searchQuery: String = "",
    val searchResults: List<Profile> = emptyList(),
    val selectedFriendHabits: List<Habit> = emptyList(),
    val myHabits: List<Habit> = emptyList(),
    @StringRes val errorRes: Int? = null,
    val requestSent: Boolean = false,
)

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
) : ViewModel() {

    private val _uiState = MutableStateFlow(FriendsUiState())
    val uiState: StateFlow<FriendsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            habitRepository.getAllHabitsWithRecords().collect { habits ->
                _uiState.update { it.copy(myHabits = habits) }
            }
        }
        refresh()
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
                requestSent = false,
                errorRes = null,
            )
        }
    }

    fun onViewFriendClicked(friendId: String) {
        openFriendSheet(FriendsSheet.ViewFriend(friendId), friendId)
    }

    fun onCreateChallengeClicked(friendId: String) {
        openFriendSheet(FriendsSheet.CreateChallenge(friendId), friendId)
    }

    fun onDismissSheet() {
        _uiState.update {
            it.copy(
                sheet = FriendsSheet.None,
                searchQuery = "",
                searchResults = emptyList(),
                selectedFriendHabits = emptyList(),
                requestSent = false,
                errorRes = null,
            )
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update {
            it.copy(
                searchQuery = query,
                searchResults = if (query.isBlank()) emptyList() else it.searchResults,
                requestSent = false,
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
                is DataResult.Success -> _uiState.update {
                    it.copy(requestSent = true, errorRes = null)
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
        challengerHabitId: String,
        challengedHabitId: String,
        criteria: ChallengeCriteria,
        durationDays: Int,
    ) {
        viewModelScope.launch {
            when (
                val result = friendsRepository.createChallenge(
                    challengedId = challengedId,
                    challengerHabitId = challengerHabitId,
                    challengedHabitId = challengedHabitId,
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

    fun clearRequestSent() {
        _uiState.update { it.copy(requestSent = false) }
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
