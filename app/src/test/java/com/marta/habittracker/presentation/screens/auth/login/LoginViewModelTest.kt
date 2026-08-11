package com.marta.habittracker.presentation.screens.auth.login

import android.app.Application
import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.model.AppError
import com.marta.habittracker.domain.usecase.FakeAuthRepository
import com.marta.habittracker.domain.usecase.FakeHabitRepository
import com.marta.habittracker.domain.usecase.LoginUseCase
import com.marta.habittracker.domain.usecase.SyncHabits
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
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.reflect.full.memberProperties

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26], application = Application::class)
class LoginViewModelTest {

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
    fun `successful login emits navigateToHome once and does not sticky-flag ui state`() = runTest {
        val viewModel = LoginViewModel(
            loginUseCase = LoginUseCase(FakeAuthRepository()),
            syncHabits = SyncHabits(FakeHabitRepository()),
        )
        val navigation = async { viewModel.navigateToHome.first() }

        viewModel.onEmailChanged("user@example.com")
        viewModel.onPasswordChanged("Secret12")
        viewModel.onClickSelected()
        advanceUntilIdle()

        assertEquals(Unit, navigation.await())
        assertFalse(viewModel.uiState.value.isLoading)
        assertFalse(
            "LoginUiState must not expose a sticky isUserLogged flag",
            LoginUiState::class.memberProperties.any { it.name == "isUserLogged" },
        )
    }

    @Test
    fun `failed login does not emit navigateToHome`() = runTest {
        val viewModel = LoginViewModel(
            loginUseCase = LoginUseCase(
                FakeAuthRepository(loginResult = DataResult.Error(AppError.Common.Unauthorized)),
            ),
            syncHabits = SyncHabits(FakeHabitRepository()),
        )
        var navigated = false
        val collector = launch {
            viewModel.navigateToHome.collect { navigated = true }
        }

        viewModel.onEmailChanged("user@example.com")
        viewModel.onPasswordChanged("Secret12")
        viewModel.onClickSelected()
        advanceUntilIdle()

        assertFalse(navigated)
        collector.cancel()
    }
}
