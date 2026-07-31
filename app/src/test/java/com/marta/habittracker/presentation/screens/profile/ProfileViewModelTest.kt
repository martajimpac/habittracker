package com.marta.habittracker.presentation.screens.profile

import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.model.AppError
import com.marta.habittracker.domain.model.Habit
import com.marta.habittracker.domain.model.HabitRecord
import com.marta.habittracker.domain.usecase.FakeAuthRepository
import com.marta.habittracker.domain.usecase.FakeHabitRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate as KotlinLocalDate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

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
    fun `loads display name and email from auth repository`() = runTest {
        val authRepository = FakeAuthRepository(
            displayName = "Alex Rivera",
            email = "alex@example.com",
        )

        val viewModel = ProfileViewModel(authRepository, FakeHabitRepository())
        val collectJob = backgroundScope.launch {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()

        assertEquals("Alex Rivera", viewModel.uiState.value.displayName)
        assertEquals("alex@example.com", viewModel.uiState.value.email)
        collectJob.cancel()
    }

    @Test
    fun `exposes real streak completed and habits counts from habit repository`() = runTest {
        val today = LocalDate.now()
        val habitA = Habit(
            id = "a",
            name = "Run",
            description = null,
            daysOfWeek = setOf(DayOfWeek.MONDAY),
            icon = "directions_run",
            colorHex = "#059669",
            reminderTime = null,
            createdAt = Instant.EPOCH,
            records = listOf(
                HabitRecord("r1", "a", toKotlinDate(today), true),
                HabitRecord("r2", "a", toKotlinDate(today.minusDays(1)), true),
                HabitRecord("r3", "a", toKotlinDate(today.minusDays(2)), false),
            ),
        )
        val habitB = Habit(
            id = "b",
            name = "Read",
            description = null,
            daysOfWeek = setOf(DayOfWeek.TUESDAY),
            icon = "menu_book",
            colorHex = "#D97706",
            reminderTime = null,
            createdAt = Instant.EPOCH,
            records = listOf(
                HabitRecord("r4", "b", toKotlinDate(today), true),
            ),
        )

        val viewModel = ProfileViewModel(
            authRepository = FakeAuthRepository(),
            habitRepository = FakeHabitRepository(allHabitsWithRecords = listOf(habitA, habitB)),
        )
        val collectJob = backgroundScope.launch {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.dayStreak)
        assertEquals(3, state.completedCount)
        assertEquals(2, state.habitsCount)
        collectJob.cancel()
    }

    @Test
    fun `sign out success emits navigate to login`() = runTest {
        val authRepository = FakeAuthRepository()
        val viewModel = ProfileViewModel(authRepository, FakeHabitRepository())

        val navigation = async { viewModel.navigateToLogin.first() }
        viewModel.onSignOutClicked()
        advanceUntilIdle()

        navigation.await()
        assertEquals(1, authRepository.signOutCalls)
    }

    @Test
    fun `sign out error does not emit navigate to login`() = runTest {
        val authRepository = FakeAuthRepository(
            signOutResult = DataResult.Error(AppError.Common.Network),
        )
        val viewModel = ProfileViewModel(authRepository, FakeHabitRepository())

        var navigated = false
        val collectJob = backgroundScope.launch {
            viewModel.navigateToLogin.collect { navigated = true }
        }
        viewModel.onSignOutClicked()
        advanceUntilIdle()

        assertEquals(1, authRepository.signOutCalls)
        assertEquals(false, navigated)
        collectJob.cancel()
    }

    private fun toKotlinDate(date: LocalDate): KotlinLocalDate =
        KotlinLocalDate(date.year, date.monthValue, date.dayOfMonth)
}
