package com.marta.habittracker.presentation.screens.profile

import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.model.AppError
import com.marta.habittracker.domain.model.Habit
import com.marta.habittracker.domain.model.HabitRecord
import com.marta.habittracker.domain.model.DailyCheckInSettings
import com.marta.habittracker.domain.repository.AuthRepository
import com.marta.habittracker.domain.repository.DailyCheckInRepository
import com.marta.habittracker.domain.repository.HabitRepository
import com.marta.habittracker.domain.model.ThemeMode
import com.marta.habittracker.domain.repository.ProfileRepository
import com.marta.habittracker.domain.repository.ThemeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import com.marta.habittracker.domain.usecase.FakeAuthRepository
import com.marta.habittracker.domain.usecase.FakeHabitRepository
import com.marta.habittracker.domain.usecase.FakeProfileRepository
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
    fun `profile content stays hidden until profile and habits have loaded`() = runTest {
        val viewModel = createViewModel(
            authRepository = FakeAuthRepository(email = "alex@example.com"),
            profileRepository = FakeProfileRepository(
                myProfileResult = DataResult.Success(
                    FakeProfileRepository.defaultProfile.copy(displayName = "Alex Rivera"),
                ),
            ),
        )

        assertEquals(false, viewModel.uiState.value.isReady)

        val collectJob = backgroundScope.launch {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()

        assertEquals(true, viewModel.uiState.value.isReady)
        assertEquals("Alex Rivera", viewModel.uiState.value.displayName)
        assertEquals("marta", viewModel.uiState.value.username)
        collectJob.cancel()
    }

    @Test
    fun `loads display name from profile repository and email from auth`() = runTest {
        val authRepository = FakeAuthRepository(
            displayName = "Should Not Appear",
            email = "alex@example.com",
        )
        val profileRepository = FakeProfileRepository(
            myProfileResult = DataResult.Success(
                FakeProfileRepository.defaultProfile.copy(
                    displayName = "Alex Rivera",
                    avatarUrl = "https://cdn.example/me.jpg?v=1",
                    avatarColor = "#059669",
                ),
            ),
        )

        val viewModel = createViewModel(
            authRepository = authRepository,
            profileRepository = profileRepository,
        )
        val collectJob = backgroundScope.launch {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()

        assertEquals("Alex Rivera", viewModel.uiState.value.displayName)
        assertEquals("marta", viewModel.uiState.value.username)
        assertEquals("alex@example.com", viewModel.uiState.value.email)
        assertEquals("https://cdn.example/me.jpg?v=1", viewModel.uiState.value.avatarUrl)
        assertEquals("#059669", viewModel.uiState.value.avatarColor)
        collectJob.cancel()
    }

    @Test
    fun `profile load error leaves avatar url null and color default`() = runTest {
        val viewModel = createViewModel(
            authRepository = FakeAuthRepository(email = "alex@example.com"),
            profileRepository = FakeProfileRepository(
                myProfileResult = DataResult.Error(AppError.Common.Network),
            ),
        )
        val collectJob = backgroundScope.launch {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()

        assertEquals("alex", viewModel.uiState.value.displayName)
        assertEquals("", viewModel.uiState.value.username)
        assertEquals(null, viewModel.uiState.value.avatarUrl)
        assertEquals("", viewModel.uiState.value.avatarColor)
        collectJob.cancel()
    }

    @Test
    fun `exposes real streak completed and habits counts from habit repository`() = runTest {
        val today = LocalDate.now()
        val habitA = Habit(
            id = "a",
            name = "Run",
            description = null,
            daysOfWeek = DayOfWeek.entries.toSet(),
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

        val viewModel = createViewModel(
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
        val viewModel = createViewModel(authRepository = authRepository)

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
        val viewModel = createViewModel(authRepository = authRepository)

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

    @Test
    fun `enabling daily check-in confirms the reminder is on`() = runTest {
        val dailyCheckInRepository = FakeDailyCheckInRepository()
        val viewModel = createViewModel(dailyCheckInRepository = dailyCheckInRepository)

        val confirmation = async { viewModel.dailyCheckInActivated.first() }
        viewModel.onDailyCheckInEnabled(true)
        advanceUntilIdle()

        confirmation.await()
        assertEquals(true, dailyCheckInRepository.settings().first().enabled)
    }

    @Test
    fun `disabling daily check-in does not confirm the reminder`() = runTest {
        val viewModel = createViewModel()
        var confirmed = false
        val collectJob = backgroundScope.launch {
            viewModel.dailyCheckInActivated.collect { confirmed = true }
        }

        viewModel.onDailyCheckInEnabled(false)
        advanceUntilIdle()

        assertEquals(false, confirmed)
        collectJob.cancel()
    }

    @Test
    fun `dark mode switch stores light or dark`() = runTest {
        val themeRepository = FakeThemeRepository()
        val viewModel = createViewModel(themeRepository = themeRepository)

        viewModel.onDarkModeChanged(true)
        advanceUntilIdle()
        assertEquals(ThemeMode.Dark, themeRepository.saved.last())

        viewModel.onDarkModeChanged(false)
        advanceUntilIdle()
        assertEquals(ThemeMode.Light, themeRepository.saved.last())
    }

    private fun createViewModel(
        authRepository: AuthRepository = FakeAuthRepository(),
        habitRepository: HabitRepository = FakeHabitRepository(),
        profileRepository: ProfileRepository = FakeProfileRepository(),
        themeRepository: ThemeRepository = FakeThemeRepository(),
        dailyCheckInRepository: DailyCheckInRepository = FakeDailyCheckInRepository(),
    ) = ProfileViewModel(
        authRepository = authRepository,
        habitRepository = habitRepository,
        profileRepository = profileRepository,
        themeRepository = themeRepository,
        dailyCheckInRepository = dailyCheckInRepository,
    )

    private fun toKotlinDate(date: LocalDate): KotlinLocalDate =
        KotlinLocalDate(date.year, date.monthValue, date.dayOfMonth)
}

private class FakeThemeRepository : ThemeRepository {
    private val mode = MutableStateFlow(ThemeMode.System)
    val saved = mutableListOf<ThemeMode>()

    override fun themeMode() = mode

    override suspend fun setThemeMode(mode: ThemeMode) {
        saved += mode
        this.mode.value = mode
    }
}

private class FakeDailyCheckInRepository : DailyCheckInRepository {
    private val settings = MutableStateFlow(DailyCheckInSettings())

    override fun settings(): Flow<DailyCheckInSettings> = settings

    override suspend fun setEnabled(enabled: Boolean) {
        settings.value = settings.value.copy(enabled = enabled)
    }

    override suspend fun setTime(time: String) {
        settings.value = settings.value.copy(time = time)
    }

    override suspend fun reschedule() = Unit
}
