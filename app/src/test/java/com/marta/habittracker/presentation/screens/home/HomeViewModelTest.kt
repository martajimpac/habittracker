package com.marta.habittracker.presentation.screens.home

import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.model.AppError
import com.marta.habittracker.domain.usecase.FakeAuthRepository
import com.marta.habittracker.domain.usecase.FakeFriendsRepository
import com.marta.habittracker.domain.usecase.FakeHabitRepository
import com.marta.habittracker.domain.usecase.FakeProfileRepository
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
                displayName = "Auth Name",
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
                displayName = "Auth Name",
                email = "alex@example.com",
            ),
            profileRepository = FakeProfileRepository(
                myProfileResult = DataResult.Error(AppError.Common.Network),
            ),
            friendsRepository = FakeFriendsRepository(),
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
        )
        advanceUntilIdle()
        assertEquals(setOf("habit-1"), viewModel.challengeHabitIds.value)

        friends.challengeHabitIdsResult = DataResult.Error(AppError.Common.Network)
        viewModel.refreshChallengeHabits()
        advanceUntilIdle()

        assertEquals(setOf("habit-1"), viewModel.challengeHabitIds.value)
    }
}
