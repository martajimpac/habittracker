package com.marta.habittracker.presentation.screens.auth.forgot_password

import android.app.Application
import com.marta.habittracker.R
import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.model.AppError
import com.marta.habittracker.domain.usecase.FakeAuthRepository
import com.marta.habittracker.domain.usecase.RequestPasswordReset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
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
class ForgotPasswordViewModelTest {

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
    fun `successful request emits navigation event and has no sticky navigation state`() = runTest {
        val repository = FakeAuthRepository()
        val viewModel = ForgotPasswordViewModel(
            requestPasswordReset = RequestPasswordReset(repository),
        )
        val navigation = async { viewModel.navigateToLogin.first() }

        viewModel.onEmailChanged("user@example.com")
        viewModel.submit()
        advanceUntilIdle()

        assertEquals(Unit, navigation.await())
        assertEquals(1, repository.requestPasswordResetCalls)
        assertFalse(viewModel.uiState.value.isLoading)
        assertFalse(
            ForgotPasswordUiState::class.memberProperties.any { it.name == "shouldNavigateToLogin" },
        )
    }

    @Test
    fun `failed request shows an error and does not navigate`() = runTest {
        val viewModel = ForgotPasswordViewModel(
            requestPasswordReset = RequestPasswordReset(
                FakeAuthRepository(
                    requestPasswordResetResult = DataResult.Error(AppError.Common.Network),
                ),
            ),
        )

        viewModel.onEmailChanged("user@example.com")
        viewModel.submit()
        advanceUntilIdle()

        assertEquals(
            R.string.error_common_network,
            viewModel.uiState.value.errorMessageRes,
        )
    }
}
