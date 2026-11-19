package com.marta.habittracker.presentation.screens.recommendations

import com.marta.habittracker.R
import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.model.AssistantAction
import com.marta.habittracker.domain.model.AssistantActionStatus
import com.marta.habittracker.domain.model.AssistantRole
import com.marta.habittracker.domain.model.AssistantTurn
import com.marta.habittracker.domain.model.ChallengeDiscardReason
import com.marta.habittracker.domain.model.ChatReply
import com.marta.habittracker.domain.model.FriendListItem
import com.marta.habittracker.domain.model.Profile
import com.marta.habittracker.domain.model.Habit
import com.marta.habittracker.domain.model.HabitCatalog
import com.marta.habittracker.domain.model.ProposedAction
import com.marta.habittracker.domain.model.errors.AppError
import com.marta.habittracker.domain.model.errors.RecommendationError
import com.marta.habittracker.domain.repository.RecommendationRepository
import com.marta.habittracker.domain.usecase.ChatContext
import com.marta.habittracker.domain.usecase.ExecuteAssistantAction
import com.marta.habittracker.domain.usecase.FakeFriendsRepository
import com.marta.habittracker.domain.usecase.FakeHabitRepository
import com.marta.habittracker.domain.usecase.ResolveChallengeProposal
import com.marta.habittracker.domain.usecase.SaveHabit
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RecommendationViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val catalog = HabitCatalog(icons = listOf("water_drop", "menu_book"), colorHexes = listOf("#294536"))
    private val createProposal = ProposedAction.CreateHabit("Gym", null, "menu_book", "#294536", "daily", emptyList(), null)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // CA-22
    @Test
    fun `without habits the chat can still send and says so in the greeting state`() = runTest {
        val remote = FakeRecommendationRepository()
        val viewModel = viewModel(habits = emptyList(), remote = remote)
        advanceUntilIdle()

        assertEquals(ChatPhase.Ready, viewModel.uiState.value.phase)
        assertEquals(false, viewModel.uiState.value.hasHabits)

        viewModel.onDraftChange("Create a reading habit")
        viewModel.send()
        advanceUntilIdle()

        assertEquals(1, remote.calls)
        assertEquals(emptyList<Any>(), remote.contexts.single().habits)
    }

    @Test
    fun `does not send a blank draft`() = runTest {
        val remote = FakeRecommendationRepository()
        val viewModel = viewModel(habits = listOf(habit()), remote = remote)
        advanceUntilIdle()

        viewModel.onDraftChange("   ")
        viewModel.send()
        advanceUntilIdle()

        assertEquals(ChatPhase.Ready, viewModel.uiState.value.phase)
        assertEquals(emptyList<AssistantTurn>(), viewModel.uiState.value.messages)
        assertEquals(0, remote.calls)
    }

    @Test
    fun `second send is ignored while the first is in flight`() = runTest {
        val remote = FakeRecommendationRepository(block = CompletableDeferred())
        val viewModel = viewModel(habits = listOf(habit()), remote = remote)
        advanceUntilIdle()

        viewModel.onDraftChange("How is my week?")
        viewModel.send()
        advanceUntilIdle()
        viewModel.onDraftChange("And tomorrow?")
        viewModel.send()
        advanceUntilIdle()

        assertEquals(ChatPhase.Sending, viewModel.uiState.value.phase)
        assertEquals(1, remote.calls)
        assertEquals(1, viewModel.uiState.value.messages.size)
    }

    @Test
    fun `follow up sends the earlier messages, today, the catalog and the current counts`() = runTest {
        val remote = FakeRecommendationRepository(
            results = listOf(
                DataResult.Success(ChatReply("Walk after lunch.")),
                DataResult.Success(ChatReply("Keep the same time.")),
            ),
        )
        val viewModel = viewModel(habits = listOf(habit()), remote = remote, language = "es")
        advanceUntilIdle()

        viewModel.onDraftChange("How is my week?")
        viewModel.send()
        advanceUntilIdle()
        viewModel.onDraftChange("Why?")
        viewModel.send()
        advanceUntilIdle()

        val second = remote.sent[1]
        assertEquals("es", remote.languages[1])
        assertEquals("Read", remote.contexts[1].habits.single().name)
        assertEquals("h", remote.contexts[1].habits.single().id)
        assertEquals(kotlinx.datetime.LocalDate(2026, 10, 28), remote.contexts[1].today)
        assertEquals(catalog, remote.contexts[1].catalog)
        assertEquals(
            listOf(
                AssistantTurn(AssistantRole.User, "How is my week?"),
                AssistantTurn(AssistantRole.Assistant, "Walk after lunch."),
                AssistantTurn(AssistantRole.User, "Why?"),
            ),
            second,
        )
        assertEquals(second + AssistantTurn(AssistantRole.Assistant, "Keep the same time."), viewModel.uiState.value.messages)
        assertEquals("", viewModel.uiState.value.draft)
    }

    @Test
    fun `failure keeps the user message and retry does not duplicate it`() = runTest {
        val remote = FakeRecommendationRepository(
            results = listOf(
                DataResult.Error(RecommendationError.Unavailable),
                DataResult.Success(ChatReply("Try a shorter walk.")),
            ),
        )
        val viewModel = viewModel(habits = listOf(habit()), remote = remote)
        advanceUntilIdle()

        viewModel.onDraftChange("How is my week?")
        viewModel.send()
        advanceUntilIdle()

        assertEquals(
            listOf(AssistantTurn(AssistantRole.User, "How is my week?")),
            viewModel.uiState.value.messages,
        )
        assertEquals(R.string.recommendation_error_unavailable, viewModel.uiState.value.errorRes)

        viewModel.retry()
        advanceUntilIdle()

        assertEquals(2, remote.calls)
        assertEquals(listOf(AssistantTurn(AssistantRole.User, "How is my week?")), remote.sent[1])
        assertEquals(
            listOf(
                AssistantTurn(AssistantRole.User, "How is my week?"),
                AssistantTurn(AssistantRole.Assistant, "Try a shorter walk."),
            ),
            viewModel.uiState.value.messages,
        )
        assertEquals(null, viewModel.uiState.value.errorRes)
    }

    @Test
    fun `an expired session shows the sign in again message`() = runTest {
        val remote = FakeRecommendationRepository(
            results = listOf(DataResult.Error(RecommendationError.SessionExpired)),
        )
        val viewModel = viewModel(habits = listOf(habit()), remote = remote)
        advanceUntilIdle()

        viewModel.onDraftChange("How is my week?")
        viewModel.send()
        advanceUntilIdle()

        assertEquals(ChatPhase.Ready, viewModel.uiState.value.phase)
        assertEquals(R.string.recommendation_error_session, viewModel.uiState.value.errorRes)
    }

    // CA-02, CA-23
    @Test
    fun `a valid proposal becomes a pending card and an invalid one shows the notice`() = runTest {
        val remote = FakeRecommendationRepository(
            results = listOf(
                DataResult.Success(ChatReply("Here:", createProposal)),
                DataResult.Success(ChatReply("Done?", ProposedAction.SetCompletion("h", "2026-10-29", true))),
            ),
        )
        val viewModel = viewModel(habits = listOf(habit()), remote = remote)
        advanceUntilIdle()

        viewModel.chat("create gym")
        viewModel.chat("mark read tomorrow")

        val card = viewModel.uiState.value.messages[1]
        assertEquals(AssistantActionStatus.Pending, card.actionStatus)
        assertEquals("fixed-id", (card.action as AssistantAction.CreateHabit).habitId)
        val invalid = viewModel.uiState.value.messages[3]
        assertEquals(
            AssistantTurn(
                role = AssistantRole.Assistant,
                text = "Done?",
                actionStatus = AssistantActionStatus.Discarded,
                invalidProposal = true,
            ),
            invalid,
        )
    }

    @Test
    fun `a reply without proposal stays plain text`() = runTest {
        val remote = FakeRecommendationRepository(results = listOf(DataResult.Success(ChatReply("Hi"))))
        val viewModel = viewModel(habits = listOf(habit()), remote = remote)
        advanceUntilIdle()

        viewModel.chat("hello")

        assertEquals(AssistantTurn(AssistantRole.Assistant, "Hi"), viewModel.uiState.value.messages[1])
    }

    @Test
    fun `a proposal rejected by the api is discarded and Bloom is told next time`() = runTest {
        val remote = FakeRecommendationRepository(
            results = listOf(DataResult.Success(ChatReply("Here:", proposalRejected = true))),
        )
        val viewModel = viewModel(habits = listOf(habit()), remote = remote)
        advanceUntilIdle()

        viewModel.chat("create a habit that suits me")

        val turn = viewModel.uiState.value.messages[1]
        assertEquals(AssistantActionStatus.Discarded, turn.actionStatus)
        assertEquals(null, turn.action)
        assertTrue(turn.invalidProposal)

        viewModel.chat("ok")
        assertTrue(remote.sent.last()[1].invalidProposal)
    }

    // CA-03, CA-20
    @Test
    fun `confirming saves the habit, marks the card done and tells Bloom next time`() = runTest {
        val habits = FakeHabitRepository(allHabitsWithRecords = listOf(habit()))
        val remote = FakeRecommendationRepository(
            results = listOf(
                DataResult.Success(ChatReply("Here:", createProposal)),
                DataResult.Success(ChatReply("It is created.")),
            ),
        )
        val viewModel = viewModel(habits = habits, remote = remote)
        advanceUntilIdle()
        viewModel.chat("create gym")

        viewModel.confirmAction(1)
        advanceUntilIdle()

        assertEquals("fixed-id", habits.insertedHabits.single().id)
        assertEquals(AssistantActionStatus.Done, viewModel.uiState.value.messages[1].actionStatus)
        assertEquals(emptySet<Int>(), viewModel.uiState.value.savingActions)

        viewModel.chat("what did you create?")
        assertEquals(AssistantActionStatus.Done, remote.sent[1][1].actionStatus)
    }

    // CA-04
    @Test
    fun `cancelling saves nothing and later confirm is ignored`() = runTest {
        val habits = FakeHabitRepository(allHabitsWithRecords = listOf(habit()))
        val remote = FakeRecommendationRepository(results = listOf(DataResult.Success(ChatReply("Here:", createProposal))))
        val viewModel = viewModel(habits = habits, remote = remote)
        advanceUntilIdle()
        viewModel.chat("create gym")

        viewModel.cancelAction(1)
        viewModel.confirmAction(1)
        advanceUntilIdle()

        assertEquals(AssistantActionStatus.Cancelled, viewModel.uiState.value.messages[1].actionStatus)
        assertEquals(0, habits.insertCalls)
    }

    // CA-05
    @Test
    fun `double confirm saves once`() = runTest {
        val habits = FakeHabitRepository(allHabitsWithRecords = listOf(habit()))
        val remote = FakeRecommendationRepository(results = listOf(DataResult.Success(ChatReply("Here:", createProposal))))
        val viewModel = viewModel(habits = habits, remote = remote)
        advanceUntilIdle()
        viewModel.chat("create gym")

        viewModel.confirmAction(1)
        viewModel.confirmAction(1)
        assertEquals(setOf(1), viewModel.uiState.value.savingActions)
        advanceUntilIdle()

        assertEquals(1, habits.insertCalls)
    }

    // CA-06
    @Test
    fun `a failed save shows an error and keeps the card pending for retry`() = runTest {
        val habits = FakeHabitRepository(allHabitsWithRecords = listOf(habit())).apply {
            insertResult = DataResult.Error(AppError.Common.Network)
        }
        val remote = FakeRecommendationRepository(results = listOf(DataResult.Success(ChatReply("Here:", createProposal))))
        val viewModel = viewModel(habits = habits, remote = remote)
        advanceUntilIdle()
        viewModel.chat("create gym")

        viewModel.confirmAction(1)
        advanceUntilIdle()

        assertEquals(ChatEvent.ShowError(R.string.error_no_internet), viewModel.events.first())
        assertEquals(AssistantActionStatus.Pending, viewModel.uiState.value.messages[1].actionStatus)

        habits.insertResult = DataResult.Success(Unit)
        viewModel.confirmAction(1)
        advanceUntilIdle()

        assertEquals(AssistantActionStatus.Done, viewModel.uiState.value.messages[1].actionStatus)
        assertEquals("fixed-id", habits.insertedHabits.single().id)
    }

    // CA-11, CA-19
    @Test
    fun `confirming a mark card saves the day and an older card keeps its buttons`() = runTest {
        val habits = FakeHabitRepository(allHabitsWithRecords = listOf(habit()))
        val remote = FakeRecommendationRepository(
            results = listOf(
                DataResult.Success(ChatReply("Monday?", ProposedAction.SetCompletion("h", "2026-10-26", true))),
                DataResult.Success(ChatReply("Tuesday?", ProposedAction.SetCompletion("h", "2026-10-27", true))),
            ),
        )
        val viewModel = viewModel(habits = habits, remote = remote)
        advanceUntilIdle()
        viewModel.chat("mark read monday")
        viewModel.chat("no, tuesday")

        assertEquals(AssistantActionStatus.Pending, viewModel.uiState.value.messages[1].actionStatus)

        viewModel.confirmAction(3)
        advanceUntilIdle()

        assertEquals(listOf(Triple("h", false, LocalDate.of(2026, 10, 27))), habits.toggledHabits)
        assertEquals(AssistantActionStatus.Pending, viewModel.uiState.value.messages[1].actionStatus)
        assertEquals(AssistantActionStatus.Done, viewModel.uiState.value.messages[3].actionStatus)
    }

    // ---- Retos (SPEC chat-challenges) ----

    private val laura = Profile(id = "laura-id", username = "laurag", displayName = "Laura García", avatarColor = "#FF7775")

    private fun friendsOf(vararg profiles: Profile) = FakeFriendsRepository(
        acceptedFriendsResult = DataResult.Success(
            profiles.map { FriendListItem(it, bestStreak = 0, publicHabitsCount = 0, activeChallengeCount = 0) },
        ),
    )

    private fun challengeProposal(friendName: String = "Laura") = ProposedAction.CreateChallenge(
        friendName = friendName,
        name = "Meditate",
        icon = "menu_book",
        color = "#294536",
        scheduleMode = "daily",
        days = emptyList(),
        weeklyTarget = null,
        criteria = null,
        durationDays = null,
    )

    // CA-01, CA-03, CA-04
    @Test
    fun `challenge proposal shows a card and a double confirm creates one challenge`() = runTest {
        val friends = friendsOf(laura)
        val remote = FakeRecommendationRepository(
            results = listOf(DataResult.Success(ChatReply("Here it is", challengeProposal()))),
        )
        val viewModel = viewModel(FakeHabitRepository(), remote, friends = friends)
        advanceUntilIdle()
        viewModel.chat("challenge Laura to meditate")

        val turn = viewModel.uiState.value.messages[1]
        assertEquals(AssistantActionStatus.Pending, turn.actionStatus)
        assertEquals(laura, (turn.action as AssistantAction.CreateChallenge).friend)

        viewModel.confirmAction(1)
        viewModel.confirmAction(1)
        advanceUntilIdle()

        assertEquals(1, friends.createChallengeCalls)
        assertEquals("laura-id", friends.lastChallengedId)
        assertEquals(AssistantActionStatus.Done, viewModel.uiState.value.messages[1].actionStatus)
    }

    // CA-06
    @Test
    fun `cancelling a challenge card creates nothing`() = runTest {
        val friends = friendsOf(laura)
        val remote = FakeRecommendationRepository(
            results = listOf(DataResult.Success(ChatReply("Here it is", challengeProposal()))),
        )
        val viewModel = viewModel(FakeHabitRepository(), remote, friends = friends)
        advanceUntilIdle()
        viewModel.chat("challenge Laura")

        viewModel.cancelAction(1)
        advanceUntilIdle()

        assertEquals(0, friends.createChallengeCalls)
        assertEquals(AssistantActionStatus.Cancelled, viewModel.uiState.value.messages[1].actionStatus)
    }

    // CA-05
    @Test
    fun `failed challenge keeps the card pending and reports the error`() = runTest {
        val friends = friendsOf(laura)
        friends.createChallengeResult = DataResult.Error(AppError.Common.Network)
        val remote = FakeRecommendationRepository(
            results = listOf(DataResult.Success(ChatReply("Here it is", challengeProposal()))),
        )
        val viewModel = viewModel(FakeHabitRepository(), remote, friends = friends)
        advanceUntilIdle()
        viewModel.chat("challenge Laura")

        viewModel.confirmAction(1)
        advanceUntilIdle()

        assertEquals(AssistantActionStatus.Pending, viewModel.uiState.value.messages[1].actionStatus)
        assertEquals(ChatEvent.ShowError(R.string.error_no_internet), viewModel.events.first())
    }

    // CA-07, RF-12: the app replaces the card with its notice and Bloom learns why.
    @Test
    fun `ambiguous friend is discarded and the next request carries the reason`() = runTest {
        val other = laura.copy(id = "2", displayName = "Laura Martín", username = "lauram")
        val remote = FakeRecommendationRepository(
            results = listOf(DataResult.Success(ChatReply("Here it is", challengeProposal()))),
        )
        val viewModel = viewModel(FakeHabitRepository(), remote, friends = friendsOf(laura, other))
        advanceUntilIdle()
        viewModel.chat("challenge Laura")

        val turn = viewModel.uiState.value.messages[1]
        assertEquals(AssistantActionStatus.Discarded, turn.actionStatus)
        assertEquals(null, turn.action)
        assertEquals(ChallengeDiscardReason.FriendAmbiguous, turn.discard?.reason)
        assertEquals(listOf("Laura García", "Laura Martín"), turn.discard?.candidates)

        viewModel.chat("Laura García")
        val history = remote.sent.last()
        assertEquals(ChallengeDiscardReason.FriendAmbiguous, history[1].discard?.reason)
    }

    // CA-08
    @Test
    fun `no friends gives a discarded turn without card`() = runTest {
        val remote = FakeRecommendationRepository(
            results = listOf(DataResult.Success(ChatReply("Here it is", challengeProposal()))),
        )
        val viewModel = viewModel(FakeHabitRepository(), remote, friends = FakeFriendsRepository())
        advanceUntilIdle()
        viewModel.chat("challenge Laura")

        assertEquals(ChallengeDiscardReason.NoFriends, viewModel.uiState.value.messages[1].discard?.reason)
    }

    @Test
    fun `friends lookup failure shows Bloom text, no card and an error`() = runTest {
        val friends = FakeFriendsRepository(acceptedFriendsResult = DataResult.Error(AppError.Common.Network))
        val remote = FakeRecommendationRepository(
            results = listOf(DataResult.Success(ChatReply("Here it is", challengeProposal()))),
        )
        val viewModel = viewModel(FakeHabitRepository(), remote, friends = friends)
        advanceUntilIdle()
        viewModel.chat("challenge Laura")

        val turn = viewModel.uiState.value.messages[1]
        assertEquals("Here it is", turn.text)
        assertEquals(ChallengeDiscardReason.LookupFailed, turn.discard?.reason)
        assertEquals(ChatEvent.ShowError(R.string.error_no_internet), viewModel.events.first())
    }

    private suspend fun RecommendationViewModel.chat(text: String) {
        onDraftChange(text)
        send()
        dispatcher.scheduler.advanceUntilIdle()
    }

    private fun viewModel(
        habits: List<Habit>,
        remote: FakeRecommendationRepository,
        language: String = "en",
    ) = viewModel(FakeHabitRepository(allHabitsWithRecords = habits), remote, language)

    private fun viewModel(
        habits: FakeHabitRepository,
        remote: FakeRecommendationRepository,
        language: String = "en",
        friends: FakeFriendsRepository = FakeFriendsRepository(),
    ) = RecommendationViewModel(
        habitRepository = habits,
        recommendations = remote,
        executeAction = ExecuteAssistantAction(habits, SaveHabit(habits), friends),
        resolveChallenge = ResolveChallengeProposal(friends),
        today = { LocalDate.parse("2026-10-28") },
        language = { language },
        catalog = catalog,
        newId = { "fixed-id" },
    )

    private fun habit() = Habit(
        id = "h",
        name = "Read",
        description = null,
        daysOfWeek = emptySet(),
        icon = "book",
        colorHex = "#6750A4",
        reminderTime = null,
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        records = emptyList(),
    )
}

private class FakeRecommendationRepository(
    private val block: CompletableDeferred<Unit>? = null,
    private val results: List<DataResult<ChatReply, RecommendationError>> = listOf(DataResult.Success(ChatReply("Reply"))),
) : RecommendationRepository {
    var calls: Int = 0
        private set
    val sent = mutableListOf<List<AssistantTurn>>()
    val languages = mutableListOf<String>()
    val contexts = mutableListOf<ChatContext>()

    override suspend fun send(
        context: ChatContext,
        language: String,
        messages: List<AssistantTurn>,
    ): DataResult<ChatReply, RecommendationError> {
        calls++
        sent += messages
        languages += language
        contexts += context
        block?.await()
        return results.getOrElse(calls - 1) { DataResult.Success(ChatReply("Reply")) }
    }
}
