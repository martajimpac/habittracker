package com.marta.habittracker.presentation.screens.recommendations

import com.marta.habittracker.R
import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.model.AssistantRole
import com.marta.habittracker.domain.model.AssistantTurn
import com.marta.habittracker.domain.model.Habit
import com.marta.habittracker.domain.model.RecommendationError
import com.marta.habittracker.domain.repository.RecommendationRepository
import com.marta.habittracker.domain.usecase.FakeHabitRepository
import com.marta.habittracker.domain.usecase.HabitWeekStat
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RecommendationViewModelTest {

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
    fun `does not send when there are no habits`() = runTest {
        val remote = FakeRecommendationRepository()
        val viewModel = viewModel(habits = emptyList(), remote = remote)
        advanceUntilIdle()

        viewModel.onDraftChange("Hello")
        viewModel.send()
        advanceUntilIdle()

        assertEquals(ChatPhase.Empty, viewModel.uiState.value.phase)
        assertEquals(0, remote.calls)
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
    fun `follow up sends the earlier messages and the current counts`() = runTest {
        val remote = FakeRecommendationRepository(
            results = listOf(
                DataResult.Success("Walk after lunch."),
                DataResult.Success("Keep the same time."),
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
        assertEquals("Read", remote.statsSnapshots[1].single().name)
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
                DataResult.Success("Try a shorter walk."),
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
        assertEquals(
            listOf(AssistantTurn(AssistantRole.User, "How is my week?")),
            remote.sent[1],
        )
        assertEquals(
            listOf(
                AssistantTurn(AssistantRole.User, "How is my week?"),
                AssistantTurn(AssistantRole.Assistant, "Try a shorter walk."),
            ),
            viewModel.uiState.value.messages,
        )
        assertEquals(null, viewModel.uiState.value.errorRes)
    }

    private fun viewModel(
        habits: List<Habit>,
        remote: FakeRecommendationRepository,
        language: String = "en",
    ) = RecommendationViewModel(
        habitRepository = FakeHabitRepository(allHabitsWithRecords = habits),
        recommendations = remote,
        today = { LocalDate.parse("2026-10-28") },
        language = { language },
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
    private val results: List<DataResult<String, RecommendationError>> = listOf(DataResult.Success("Reply")),
) : RecommendationRepository {
    var calls: Int = 0
        private set
    val sent = mutableListOf<List<AssistantTurn>>()
    val languages = mutableListOf<String>()
    val statsSnapshots = mutableListOf<List<HabitWeekStat>>()

    override suspend fun request(stats: List<HabitWeekStat>): DataResult<String, RecommendationError> {
        error("The chat does not call recommendations")
    }

    override suspend fun send(
        stats: List<HabitWeekStat>,
        language: String,
        messages: List<AssistantTurn>,
    ): DataResult<String, RecommendationError> {
        calls++
        sent += messages
        languages += language
        statsSnapshots += stats
        block?.await()
        return results[calls - 1]
    }
}
