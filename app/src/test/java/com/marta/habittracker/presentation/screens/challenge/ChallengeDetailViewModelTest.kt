package com.marta.habittracker.presentation.screens.challenge

import androidx.lifecycle.SavedStateHandle
import com.marta.habittracker.R
import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.model.AppError
import com.marta.habittracker.domain.model.Challenge
import com.marta.habittracker.domain.model.ChallengeCard
import com.marta.habittracker.domain.model.ChallengeCriteria
import com.marta.habittracker.domain.model.ChallengeStatus
import com.marta.habittracker.domain.model.Profile
import com.marta.habittracker.domain.repository.AuthRepository
import com.marta.habittracker.domain.usecase.FakeFriendsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class ChallengeDetailViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `load success for challenged user sets canRespond true and clears loading`() = runTest {
        val pendingCard = pendingChallengeCard()
        val viewModel = createViewModel(
            friendsRepository = FakeFriendsRepository(
                getChallengeResult = DataResult.Success(pendingCard),
            ),
            authRepository = FixedUserIdAuthRepository("me"),
        )
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(pendingCard, viewModel.uiState.value.card)
        assertTrue(viewModel.uiState.value.canRespond)
    }

    @Test
    fun `load success for challenger sets canRespond false`() = runTest {
        val pendingCard = pendingChallengeCard()
        val viewModel = createViewModel(
            friendsRepository = FakeFriendsRepository(
                getChallengeResult = DataResult.Success(pendingCard),
            ),
            authRepository = FixedUserIdAuthRepository("them"),
        )
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(pendingCard, viewModel.uiState.value.card)
        assertFalse(viewModel.uiState.value.canRespond)
    }

    @Test
    fun `onAccept success replaces card with active status and emits no events`() = runTest {
        val pendingCard = pendingChallengeCard()
        val activeCard = pendingCard.copy(
            challenge = pendingCard.challenge.copy(status = ChallengeStatus.Active),
        )
        val friendsRepository = FakeFriendsRepository(
            getChallengeResult = DataResult.Success(pendingCard),
            respondToChallengeResult = DataResult.Success(activeCard),
        )
        val viewModel = createViewModel(
            friendsRepository = friendsRepository,
            authRepository = FixedUserIdAuthRepository("me"),
        )
        advanceUntilIdle()

        val events = mutableListOf<ChallengeDetailEvent>()
        val collectJob = backgroundScope.launch {
            viewModel.events.collect { events.add(it) }
        }

        viewModel.onAccept()
        advanceUntilIdle()

        assertEquals(activeCard, viewModel.uiState.value.card)
        assertEquals(ChallengeStatus.Active, viewModel.uiState.value.card?.challenge?.status)
        assertFalse(viewModel.uiState.value.canRespond)
        assertTrue(events.isEmpty())
        collectJob.cancel()
    }

    @Test
    fun `onDecline success emits NavigateBack`() = runTest {
        val pendingCard = pendingChallengeCard()
        val declinedCard = pendingCard.copy(
            challenge = pendingCard.challenge.copy(status = ChallengeStatus.Declined),
        )
        val friendsRepository = FakeFriendsRepository(
            getChallengeResult = DataResult.Success(pendingCard),
            respondToChallengeResult = DataResult.Success(declinedCard),
        )
        val viewModel = createViewModel(
            friendsRepository = friendsRepository,
            authRepository = FixedUserIdAuthRepository("me"),
        )
        advanceUntilIdle()

        val event = async { viewModel.events.first() }
        viewModel.onDecline()
        advanceUntilIdle()

        assertEquals(ChallengeDetailEvent.NavigateBack, event.await())
    }

    @Test
    fun `onDecline network error sets error resource and does not navigate back`() = runTest {
        val pendingCard = pendingChallengeCard()
        val friendsRepository = FakeFriendsRepository(
            getChallengeResult = DataResult.Success(pendingCard),
            respondToChallengeResult = DataResult.Error(AppError.Common.Network),
        )
        val viewModel = createViewModel(
            friendsRepository = friendsRepository,
            authRepository = FixedUserIdAuthRepository("me"),
        )
        advanceUntilIdle()

        viewModel.onDecline()
        advanceUntilIdle()

        assertEquals(R.string.error_no_internet, viewModel.uiState.value.errorRes)
        assertEquals(1, friendsRepository.respondCalls)
    }

    @Test
    fun `onAccept when viewer is challenger does not call respondToChallenge`() = runTest {
        val pendingCard = pendingChallengeCard()
        val friendsRepository = FakeFriendsRepository(
            getChallengeResult = DataResult.Success(pendingCard),
            respondToChallengeResult = DataResult.Success(pendingCard),
        )
        val viewModel = createViewModel(
            friendsRepository = friendsRepository,
            authRepository = FixedUserIdAuthRepository("them"),
        )
        advanceUntilIdle()

        viewModel.onAccept()
        advanceUntilIdle()

        assertEquals(0, friendsRepository.respondCalls)
    }

    private fun createViewModel(
        friendsRepository: FakeFriendsRepository = FakeFriendsRepository(),
        authRepository: AuthRepository = FixedUserIdAuthRepository("me"),
        challengeId: String = CHALLENGE_ID,
    ): ChallengeDetailViewModel = ChallengeDetailViewModel(
        savedStateHandle = SavedStateHandle(mapOf("challengeId" to challengeId)),
        friendsRepository = friendsRepository,
        authRepository = authRepository,
    )

    private fun pendingChallengeCard(): ChallengeCard {
        val opponent = Profile(
            id = "them",
            username = "them",
            displayName = "Challenger",
            avatarColor = "#6750A4",
        )
        val challenge = Challenge(
            id = CHALLENGE_ID,
            challengerId = "them",
            challengedId = "me",
            challengerHabitId = "their-habit",
            challengedHabitId = "my-habit",
            criteria = ChallengeCriteria.Streak,
            startsAt = Instant.EPOCH,
            endsAt = Instant.EPOCH.plusSeconds(86_400),
            status = ChallengeStatus.Pending,
        )
        return ChallengeCard(
            challenge = challenge,
            opponent = opponent,
            myProgress = 0,
            theirProgress = 0,
            daysLeft = 7,
            habitName = "Read",
            habitIcon = "menu_book",
            habitColorHex = "#6750A4",
        )
    }

    private class FixedUserIdAuthRepository(
        private val userId: String?,
    ) : AuthRepository {
        override suspend fun doLogin(
            email: String,
            password: String,
        ) = error("not used")

        override suspend fun doRegister(
            email: String,
            password: String,
        ) = error("not used")

        override suspend fun requestPasswordReset(email: String) = error("not used")

        override suspend fun updatePassword(newPassword: String) = error("not used")

        override suspend fun isLoggedIn(): Boolean = userId != null

        override suspend fun getCurrentUserId(): String? = userId

        override suspend fun getCurrentUserDisplayName(): String = "Test"

        override suspend fun getCurrentUserEmail(): String = "test@example.com"

        override suspend fun signOut() = error("not used")
    }

    private companion object {
        const val CHALLENGE_ID = "challenge-id"
    }
}
