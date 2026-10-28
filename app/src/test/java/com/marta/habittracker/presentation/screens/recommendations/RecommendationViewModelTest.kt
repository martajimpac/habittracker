package com.marta.habittracker.presentation.screens.recommendations

import com.marta.habittracker.domain.DataResult
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
    fun `does not request a recommendation when there are no habits`() = runTest {
        val remote = FakeRecommendationRepository()
        val viewModel = viewModel(habits = emptyList(), remote = remote)
        advanceUntilIdle()

        viewModel.request()
        advanceUntilIdle()

        assertEquals(RecommendationPhase.Empty, viewModel.uiState.value.phase)
        assertEquals(0, remote.calls)
    }

    @Test
    fun `second request is ignored while the first is loading`() = runTest {
        val remote = FakeRecommendationRepository(block = CompletableDeferred())
        val viewModel = viewModel(habits = listOf(habit()), remote = remote)
        advanceUntilIdle()

        viewModel.request()
        advanceUntilIdle()
        viewModel.request()
        advanceUntilIdle()

        assertEquals(RecommendationPhase.Loading, viewModel.uiState.value.phase)
        assertEquals(1, remote.calls)
    }

    private fun viewModel(
        habits: List<Habit>,
        remote: FakeRecommendationRepository,
    ) = RecommendationViewModel(
        habitRepository = FakeHabitRepository(allHabitsWithRecords = habits),
        recommendations = remote,
        today = { LocalDate.parse("2026-10-28") },
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
) : RecommendationRepository {
    var calls: Int = 0
        private set

    override suspend fun request(stats: List<HabitWeekStat>): DataResult<String, RecommendationError> {
        calls++
        block?.await()
        return DataResult.Success("Sugerencia")
    }
}
