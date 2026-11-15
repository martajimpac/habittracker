package com.marta.habittracker.presentation.screens.home

import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.model.errors.AppError
import com.marta.habittracker.domain.usecase.FakeAuthRepository
import com.marta.habittracker.domain.usecase.FakeFriendsRepository
import com.marta.habittracker.domain.usecase.FakeHabitRepository
import com.marta.habittracker.domain.repository.StreakProtectionRepository
import com.marta.habittracker.domain.usecase.FakeProfileRepository
import com.marta.habittracker.domain.usecase.ProtectionReconcileResult
import com.marta.habittracker.domain.widget.AppWidgetRefresher
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
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

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

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
    fun `greeting name comes from profile display name not auth`() = runTest {
        val viewModel = HomeViewModel(
            repository = FakeHabitRepository(),
            authRepository = FakeAuthRepository(
                email = "alex@example.com",
            ),
            profileRepository = FakeProfileRepository(
                myProfileResult = DataResult.Success(
                    FakeProfileRepository.defaultProfile.copy(
                        displayName = "Profile Name",
                        avatarUrl = "https://cdn.example/me.jpg?v=1",
                        avatarColor = "#059669",
                    ),
                ),
            ),
            friendsRepository = FakeFriendsRepository(),
            streakProtectionRepository = FakeStreakProtectionRepository(),
            widgetRefresher = FakeAppWidgetRefresher(),
        )

        advanceUntilIdle()

        assertEquals("Profile Name", viewModel.userDisplayName.value)
        assertEquals("https://cdn.example/me.jpg?v=1", viewModel.avatarUrl.value)
        assertEquals("#059669", viewModel.avatarColor.value)
    }

    @Test
    fun `falls back to email local part when profile load fails`() = runTest {
        val viewModel = HomeViewModel(
            repository = FakeHabitRepository(),
            authRepository = FakeAuthRepository(
                email = "alex@example.com",
            ),
            profileRepository = FakeProfileRepository(
                myProfileResult = DataResult.Error(AppError.Common.Network),
            ),
            friendsRepository = FakeFriendsRepository(),
            streakProtectionRepository = FakeStreakProtectionRepository(),
            widgetRefresher = FakeAppWidgetRefresher(),
        )

        advanceUntilIdle()

        assertEquals("alex", viewModel.userDisplayName.value)
        assertEquals(null, viewModel.avatarUrl.value)
        assertEquals("", viewModel.avatarColor.value)
    }

    @Test
    fun `midnight moves the marked day when it was still today`() {
        val monday = java.time.LocalDate.of(2026, 10, 19)
        val tuesday = monday.plusDays(1)
        val viewModel = HomeViewModel(
            repository = FakeHabitRepository(),
            authRepository = FakeAuthRepository(),
            profileRepository = FakeProfileRepository(),
            friendsRepository = FakeFriendsRepository(),
            streakProtectionRepository = FakeStreakProtectionRepository(),
            widgetRefresher = FakeAppWidgetRefresher(),
        )
        viewModel.currentDate = { monday }
        viewModel.refreshDay()
        viewModel.onDateSelected(monday)

        viewModel.currentDate = { tuesday }
        viewModel.refreshDay()

        assertEquals(tuesday, viewModel.today.value)
        assertEquals(tuesday, viewModel.selectedDate.value)
    }

    @Test
    fun `midnight keeps a day the user picked that was not today`() {
        val monday = java.time.LocalDate.of(2026, 10, 19)
        val sunday = monday.minusDays(1)
        val tuesday = monday.plusDays(1)
        val viewModel = HomeViewModel(
            repository = FakeHabitRepository(),
            authRepository = FakeAuthRepository(),
            profileRepository = FakeProfileRepository(),
            friendsRepository = FakeFriendsRepository(),
            streakProtectionRepository = FakeStreakProtectionRepository(),
            widgetRefresher = FakeAppWidgetRefresher(),
        )
        viewModel.currentDate = { monday }
        viewModel.refreshDay()
        viewModel.onDateSelected(sunday)

        viewModel.currentDate = { tuesday }
        viewModel.refreshDay()

        assertEquals(tuesday, viewModel.today.value)
        assertEquals(sunday, viewModel.selectedDate.value)
    }

    @Test
    fun `challenge habit ids stay after a later failure`() = runTest {
        val friends = FakeFriendsRepository()
        friends.challengeHabitIdsResult = DataResult.Success(setOf("habit-1"))
        val viewModel = HomeViewModel(
            repository = FakeHabitRepository(),
            authRepository = FakeAuthRepository(),
            profileRepository = FakeProfileRepository(),
            friendsRepository = friends,
            streakProtectionRepository = FakeStreakProtectionRepository(),
            widgetRefresher = FakeAppWidgetRefresher(),
        )
        advanceUntilIdle()
        assertEquals(setOf("habit-1"), viewModel.challengeHabitIds.value)

        friends.challengeHabitIdsResult = DataResult.Error(AppError.Common.Network)
        viewModel.refreshChallengeHabits()
        advanceUntilIdle()

        assertEquals(setOf("habit-1"), viewModel.challengeHabitIds.value)
    }

    @Test
    fun `is loading until habits are first emitted`() = runTest {
        val viewModel = HomeViewModel(
            repository = FakeHabitRepository(),
            authRepository = FakeAuthRepository(),
            profileRepository = FakeProfileRepository(),
            friendsRepository = FakeFriendsRepository(),
            streakProtectionRepository = FakeStreakProtectionRepository(),
            widgetRefresher = FakeAppWidgetRefresher(),
        )
        val collector = backgroundScope.launch { viewModel.isLoading.collect {} }

        assertTrue(viewModel.isLoading.value)

        advanceUntilIdle()

        assertFalse(viewModel.isLoading.value)
        collector.cancel()
    }

    @Test
    fun `toggle on a past day saves that day`() = runTest {
        val repository = FakeHabitRepository()
        val viewModel = toggleViewModel(repository)
        viewModel.onDateSelected(WEDNESDAY.minusDays(2))

        viewModel.toggleComplete(toggleHabit(isCompleted = false))
        advanceUntilIdle()

        assertEquals(listOf(Triple("h1", false, WEDNESDAY.minusDays(2))), repository.toggledHabits)
        assertTrue(viewModel.savingToggles.value.isEmpty())
    }

    @Test
    fun `toggle on a past day that was done unmarks it`() = runTest {
        val repository = FakeHabitRepository()
        val viewModel = toggleViewModel(repository)
        viewModel.onDateSelected(WEDNESDAY.minusDays(1))

        viewModel.toggleComplete(toggleHabit(isCompleted = true))
        advanceUntilIdle()

        assertEquals(listOf(Triple("h1", true, WEDNESDAY.minusDays(1))), repository.toggledHabits)
    }

    @Test
    fun `toggle today still saves`() = runTest {
        val repository = FakeHabitRepository()
        val viewModel = toggleViewModel(repository)

        viewModel.toggleComplete(toggleHabit(isCompleted = false))
        advanceUntilIdle()

        assertEquals(listOf(Triple("h1", false, WEDNESDAY)), repository.toggledHabits)
    }

    @Test
    fun `toggle on a future day is ignored`() = runTest {
        val repository = FakeHabitRepository()
        val viewModel = toggleViewModel(repository)
        viewModel.onDateSelected(WEDNESDAY.plusDays(1))

        viewModel.toggleComplete(toggleHabit(isCompleted = false))
        advanceUntilIdle()

        assertTrue(repository.toggledHabits.isEmpty())
    }

    @Test
    fun `second tap while saving is ignored`() = runTest {
        val repository = FakeHabitRepository()
        val viewModel = toggleViewModel(repository)
        viewModel.onDateSelected(WEDNESDAY.minusDays(1))

        viewModel.toggleComplete(toggleHabit(isCompleted = false))
        assertEquals(setOf("h1" to WEDNESDAY.minusDays(1)), viewModel.savingToggles.value)
        viewModel.toggleComplete(toggleHabit(isCompleted = false))
        advanceUntilIdle()

        assertEquals(1, repository.toggledHabits.size)
        assertTrue(viewModel.savingToggles.value.isEmpty())
    }

    @Test
    fun `toggle failure emits the error`() = runTest {
        val repository = FakeHabitRepository()
        repository.toggleResult = DataResult.Error(AppError.Common.Network)
        val viewModel = toggleViewModel(repository)
        viewModel.onDateSelected(WEDNESDAY.minusDays(1))
        val events = mutableListOf<HomeEvent>()
        val collector = backgroundScope.launch { viewModel.events.collect { events += it } }

        viewModel.toggleComplete(toggleHabit(isCompleted = false))
        advanceUntilIdle()

        val event = events.filterIsInstance<HomeEvent.HabitToggle>().single()
        assertTrue(event.result is DataResult.Error)
        assertTrue(viewModel.savingToggles.value.isEmpty())
        collector.cancel()
    }

    private fun toggleViewModel(repository: FakeHabitRepository): HomeViewModel {
        val viewModel = HomeViewModel(
            repository = repository,
            authRepository = FakeAuthRepository(),
            profileRepository = FakeProfileRepository(),
            friendsRepository = FakeFriendsRepository(),
            streakProtectionRepository = FakeStreakProtectionRepository(),
            widgetRefresher = FakeAppWidgetRefresher(),
        )
        viewModel.currentDate = { WEDNESDAY }
        viewModel.refreshDay()
        viewModel.onDateSelected(WEDNESDAY)
        return viewModel
    }

    private fun toggleHabit(isCompleted: Boolean) = com.marta.habittracker.domain.model.Habit(
        id = "h1",
        name = "Test",
        description = null,
        daysOfWeek = emptySet(),
        icon = "water_drop",
        colorHex = "#6750A4",
        reminderTime = null,
        createdAt = java.time.Instant.parse("2026-01-01T00:00:00Z"),
        records = emptyList(),
        isCompleted = isCompleted,
    )

    private companion object {
        val WEDNESDAY: LocalDate = LocalDate.of(2026, 11, 11)
    }
}

private class FakeStreakProtectionRepository : StreakProtectionRepository {
    override suspend fun reconcile(
        habits: List<com.marta.habittracker.domain.model.Habit>,
        today: LocalDate,
    ) = ProtectionReconcileResult(balance = 0, habits = emptyList(), newEvents = emptyList())
}

private class FakeAppWidgetRefresher : AppWidgetRefresher {
    override suspend fun refreshTodayWidget(habits: List<com.marta.habittracker.domain.model.Habit>) {}
}
