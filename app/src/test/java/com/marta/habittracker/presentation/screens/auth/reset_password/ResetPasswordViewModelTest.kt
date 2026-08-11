package com.marta.habittracker.presentation.screens.auth.reset_password

import android.app.Application
import com.marta.habittracker.R
import com.marta.habittracker.domain.usecase.FakeAuthRepository
import com.marta.habittracker.domain.usecase.UpdatePassword
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
class ResetPasswordViewModelTest {

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
    fun `matching valid passwords update password then emit navigation`() = runTest {
        val repository = FakeAuthRepository()
        val viewModel = ResetPasswordViewModel(UpdatePassword(repository))
        val navigation = async { viewModel.navigateToLogin.first() }

        viewModel.onNewPasswordChanged("ValidPass1")
        viewModel.onConfirmPasswordChanged("ValidPass1")
        viewModel.submit()
        advanceUntilIdle()

        assertEquals(Unit, navigation.await())
        assertEquals(1, repository.updatePasswordCalls)
        assertEquals(1, repository.signOutCalls)
        assertFalse(viewModel.uiState.value.isLoading)
        assertFalse(
            ResetPasswordUiState::class.memberProperties.any { it.name == "shouldNavigateToLogin" },
        )
    }

    @Test
    fun `mismatched passwords remain disabled and show validation error`() {
        val viewModel = ResetPasswordViewModel(UpdatePassword(FakeAuthRepository()))

        viewModel.onNewPasswordChanged("ValidPass1")
        viewModel.onConfirmPasswordChanged("Different1")

        assertFalse(viewModel.uiState.value.isSubmitEnabled)
        assertEquals(
            R.string.reset_password_error_passwords_do_not_match,
            viewModel.uiState.value.errorMessageRes,
        )
    }
}
