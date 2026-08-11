package com.marta.habittracker.presentation.screens.friends

import com.marta.habittracker.R
import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.model.AppError
import com.marta.habittracker.domain.model.Challenge
import com.marta.habittracker.domain.model.ChallengeCard
import com.marta.habittracker.domain.model.ChallengeCriteria
import com.marta.habittracker.domain.model.ChallengeStatus
import com.marta.habittracker.domain.model.FriendListItem
import com.marta.habittracker.domain.model.Friendship
import com.marta.habittracker.domain.model.FriendshipStatus
import com.marta.habittracker.domain.model.Habit
import com.marta.habittracker.domain.model.Profile
import com.marta.habittracker.domain.repository.FriendsRepository
import com.marta.habittracker.domain.usecase.FakeFriendsRepository
import com.marta.habittracker.domain.usecase.FakeHabitRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.DayOfWeek
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class FriendsViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init loads friends challenges pending requests and own habits`() = runTest {
        val profile = profile("friend")
        val friend = FriendListItem(profile, bestStreak = 4, publicHabitsCount = 2, activeChallengeCount = 1)
        val challenge = challengeCard(profile)
        val request = Friendship("request", "requester", "me", FriendshipStatus.Pending)
        val myHabit = habit("mine")
        val friendsRepository = FakeFriendsRepository(
            acceptedFriendsResult = DataResult.Success(listOf(friend)),
            activeChallengesResult = DataResult.Success(listOf(challenge)),
            pendingFriendRequestsResult = DataResult.Success(listOf(request)),
        )

        val viewModel = FriendsViewModel(
            friendsRepository = friendsRepository,
            habitRepository = FakeHabitRepository(allHabitsWithRecords = listOf(myHabit)),
        )
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(listOf(friend), viewModel.uiState.value.friends)
        assertEquals(listOf(challenge), viewModel.uiState.value.challenges)
        assertEquals(listOf(request), viewModel.uiState.value.pendingRequests)
        assertEquals(listOf(myHabit), viewModel.uiState.value.myHabits)
        assertEquals(1, friendsRepository.getAcceptedFriendsCalls)
        assertEquals(1, friendsRepository.getActiveChallengesCalls)
        assertEquals(1, friendsRepository.getPendingFriendRequestsCalls)
    }

    @Test
    fun `network error exposes no internet error resource`() = runTest {
        val viewModel = FriendsViewModel(
            friendsRepository = FakeFriendsRepository(
                acceptedFriendsResult = DataResult.Error(AppError.Common.Network),
            ),
            habitRepository = FakeHabitRepository(),
        )
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(R.string.error_no_internet, viewModel.uiState.value.errorRes)
    }

    @Test
    fun `add friend click opens add friend sheet`() = runTest {
        val viewModel = FriendsViewModel(FakeFriendsRepository(), FakeHabitRepository())
        advanceUntilIdle()

        viewModel.onAddFriendClicked()

        assertEquals(FriendsSheet.AddFriend, viewModel.uiState.value.sheet)
    }

    @Test
    fun `send friend request calls repository and marks request sent`() = runTest {
        val repository = FakeFriendsRepository()
        val viewModel = FriendsViewModel(repository, FakeHabitRepository())
        advanceUntilIdle()

        viewModel.sendFriendRequest("profile-id")
        advanceUntilIdle()

        assertEquals(1, repository.sendFriendRequestCalls)
        assertEquals("profile-id", repository.lastAddresseeId)
        assertTrue(viewModel.uiState.value.requestSent)
    }

    @Test
    fun `respond to request calls repository and refreshes content`() = runTest {
        val repository = FakeFriendsRepository()
        val viewModel = FriendsViewModel(repository, FakeHabitRepository())
        advanceUntilIdle()

        viewModel.respondToRequest(friendshipId = "request-id", accept = true)
        advanceUntilIdle()

        assertEquals(1, repository.respondToFriendRequestCalls)
        assertEquals("request-id", repository.lastFriendshipId)
        assertEquals(true, repository.lastRespondAccept)
        assertEquals(2, repository.getAcceptedFriendsCalls)
        assertEquals(2, repository.getActiveChallengesCalls)
        assertEquals(2, repository.getPendingFriendRequestsCalls)
    }

    @Test
    fun `view and create challenge actions open sheets and load public habits`() = runTest {
        val publicHabit = habit("public")
        val repository = FakeFriendsRepository(
            publicHabitsResult = DataResult.Success(listOf(publicHabit)),
        )
        val viewModel = FriendsViewModel(repository, FakeHabitRepository())
        advanceUntilIdle()

        viewModel.onViewFriendClicked("friend-id")
        advanceUntilIdle()

        assertEquals(FriendsSheet.ViewFriend("friend-id"), viewModel.uiState.value.sheet)
        assertEquals(listOf(publicHabit), viewModel.uiState.value.selectedFriendHabits)

        viewModel.onCreateChallengeClicked("friend-id")
        advanceUntilIdle()

        assertEquals(FriendsSheet.CreateChallenge("friend-id"), viewModel.uiState.value.sheet)
        assertEquals(2, repository.getPublicHabitsForFriendCalls)
        assertEquals("friend-id", repository.lastFriendUserId)
    }

    @Test
    fun `stale public habits response is ignored when friend sheet changes`() = runTest {
        val habitA = habit("habit-a")
        val habitB = habit("habit-b")
        val repository = DelayedPublicHabitsFriendsRepository(
            habitsByFriend = mapOf(
                "friend-a" to listOf(habitA),
                "friend-b" to listOf(habitB),
            ),
            slowFriendIds = setOf("friend-a"),
        )
        val viewModel = FriendsViewModel(repository, FakeHabitRepository())
        advanceUntilIdle()

        viewModel.onViewFriendClicked("friend-a")
        viewModel.onViewFriendClicked("friend-b")
        advanceUntilIdle()

        assertEquals(FriendsSheet.ViewFriend("friend-b"), viewModel.uiState.value.sheet)
        assertEquals(listOf(habitB), viewModel.uiState.value.selectedFriendHabits)
    }

    @Test
    fun `manual refresh loads content again`() = runTest {
        val repository = FakeFriendsRepository()
        val viewModel = FriendsViewModel(repository, FakeHabitRepository())
        advanceUntilIdle()

        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(2, repository.getAcceptedFriendsCalls)
        assertEquals(2, repository.getActiveChallengesCalls)
        assertEquals(2, repository.getPendingFriendRequestsCalls)
    }

    private fun profile(id: String) = Profile(
        id = id,
        username = id,
        displayName = "Friend",
        avatarColor = "#6750A4",
    )

    private fun habit(id: String) = Habit(
        id = id,
        name = "Read",
        description = null,
        daysOfWeek = setOf(DayOfWeek.MONDAY),
        icon = "menu_book",
        colorHex = "#6750A4",
        reminderTime = null,
        createdAt = Instant.EPOCH,
        records = emptyList(),
        isPublic = true,
    )

    private class DelayedPublicHabitsFriendsRepository(
        private val habitsByFriend: Map<String, List<Habit>>,
        private val slowFriendIds: Set<String>,
    ) : FriendsRepository by FakeFriendsRepository() {

        override suspend fun getPublicHabitsForFriend(
            friendUserId: String,
        ): DataResult<List<Habit>, AppError> {
            if (friendUserId in slowFriendIds) {
                delay(100)
            }
            return DataResult.Success(habitsByFriend[friendUserId].orEmpty())
        }
    }

    private fun challengeCard(opponent: Profile): ChallengeCard {
        val challenge = Challenge(
            id = "challenge",
            challengerId = "me",
            challengedId = opponent.id,
            challengerHabitId = "mine",
            challengedHabitId = "theirs",
            criteria = ChallengeCriteria.Streak,
            startsAt = Instant.EPOCH,
            endsAt = Instant.EPOCH.plusSeconds(86_400),
            status = ChallengeStatus.Active,
        )
        return ChallengeCard(
            challenge = challenge,
            opponent = opponent,
            myProgress = 40,
            theirProgress = 30,
            daysLeft = 1,
            habitName = "Read",
            habitIcon = "menu_book",
            habitColorHex = "#6750A4",
        )
    }
}
