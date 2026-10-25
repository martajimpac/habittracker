package com.marta.habittracker.presentation.screens.auth.register

import android.app.Application
import com.marta.habittracker.R
import com.marta.habittracker.core.push.DeviceTokenRegistration
import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.model.RegisterError
import com.marta.habittracker.domain.model.UsernameAvailability
import com.marta.habittracker.domain.repository.UsernameAvailabilityRepository
import com.marta.habittracker.domain.usecase.FakeAuthRepository
import com.marta.habittracker.domain.usecase.RegisterUseCase
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26], application = Application::class)
class RegisterViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(kotlinx.coroutines.test.StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `register stays disabled when password has digits but no letter`() {
        val viewModel = viewModel()

        fillForm(viewModel, password = "12345678")

        assertFalse(viewModel.uiState.value.isRegisterEnabled)
        assertEquals(R.string.error_register_weak_password, viewModel.uiState.value.passwordErrorRes)
    }

    @Test
    fun `register stays disabled when password has letters but no digit`() {
        val viewModel = viewModel()

        fillForm(viewModel, password = "abcdefgh")

        assertFalse(viewModel.uiState.value.isRegisterEnabled)
        assertEquals(R.string.error_register_weak_password, viewModel.uiState.value.passwordErrorRes)
    }

    @Test
    fun `register enables when password has 8 characters a letter and a digit`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModel = viewModel()

        fillForm(viewModel, password = "abcdefg1")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isRegisterEnabled)
        assertNull(viewModel.uiState.value.passwordErrorRes)
    }

    @Test
    fun `password stays neutral until the user types`() {
        val viewModel = viewModel()

        assertNull(viewModel.uiState.value.passwordErrorRes)

        viewModel.onPasswordChanged("")

        assertNull(viewModel.uiState.value.passwordErrorRes)
    }

    @Test
    fun `tapping create while disabled marks every missing field and focuses the first`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModel = viewModel()
        val focused = mutableListOf<RegisterField>()
        val collectJob = launch { viewModel.focusField.collect { focused.add(it) } }

        viewModel.onRegisterClicked()
        advanceUntilIdle()

        assertEquals(R.string.error_register_name_required, viewModel.uiState.value.nameErrorRes)
        assertEquals(R.string.error_register_invalid_email, viewModel.uiState.value.emailErrorRes)
        assertEquals(R.string.error_register_weak_password, viewModel.uiState.value.passwordErrorRes)
        assertEquals(R.string.error_register_terms_required, viewModel.uiState.value.termsErrorRes)
        assertEquals(listOf(RegisterField.Name), focused)
        assertFalse(viewModel.uiState.value.isLoading)

        viewModel.onNameChanged("Marta")
        viewModel.onEmailChanged("user@example.com")
        viewModel.onRegisterClicked()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.nameErrorRes)
        assertNull(viewModel.uiState.value.emailErrorRes)
        assertEquals(R.string.error_register_terms_required, viewModel.uiState.value.termsErrorRes)
        assertEquals(RegisterField.Username, focused.last())
        collectJob.cancel()
    }

    @Test
    fun `taken username shows alternatives and keeps register disabled`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModel = viewModel(usernames = TakenThenFreeUsernames("marta"))

        viewModel.onUsernameChanged("Marta!")
        advanceUntilIdle()

        assertEquals("marta", viewModel.uiState.value.username)
        assertEquals(R.string.error_register_username_taken, viewModel.uiState.value.usernameErrorRes)
        assertEquals(listOf("marta1", "marta2", "marta_bloom"), viewModel.uiState.value.usernameSuggestions)
        assertFalse(viewModel.uiState.value.isRegisterEnabled)

        viewModel.onUsernameSuggestionSelected("marta1")
        advanceUntilIdle()

        assertEquals("marta1", viewModel.uiState.value.username)
        assertNull(viewModel.uiState.value.usernameErrorRes)
        assertTrue(viewModel.uiState.value.usernameSuggestions.isEmpty())
        assertTrue(viewModel.uiState.value.usernameAvailable)
    }

    @Test
    fun `confirmation required returns to login without an error`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModel = RegisterViewModel(
            registerUseCase = RegisterUseCase(
                FakeAuthRepository(
                    registerResult = DataResult.Error(RegisterError.EmailConfirmationRequired),
                ),
            ),
            usernameAvailability = AvailableUsernames(),
            deviceTokenRegistration = object : DeviceTokenRegistration {
                override suspend fun registerCurrentToken() = Unit
                override suspend fun clearCurrentToken() = Unit
            },
        )
        val created = mutableListOf<RegisteredAccount>()
        val collectJob = launch { viewModel.accountCreated.collect { created.add(it) } }

        viewModel.onNameChanged("Marta")
        viewModel.onUsernameChanged("marta")
        viewModel.onEmailChanged("user@example.com")
        viewModel.onPasswordChanged("abcdefg1")
        viewModel.onTermsChecked(true)
        advanceUntilIdle()
        viewModel.onRegisterClicked()
        advanceUntilIdle()

        assertEquals(listOf(RegisteredAccount("user@example.com", "abcdefg1")), created)
        assertNull(viewModel.uiState.value.errorMessageRes)
        assertFalse(viewModel.uiState.value.isLoading)
        collectJob.cancel()
    }

    private fun viewModel(
        usernames: UsernameAvailabilityRepository = AvailableUsernames(),
    ): RegisterViewModel {
        return RegisterViewModel(
            registerUseCase = RegisterUseCase(FakeAuthRepository()),
            usernameAvailability = usernames,
            deviceTokenRegistration = object : DeviceTokenRegistration {
                override suspend fun registerCurrentToken() = Unit
                override suspend fun clearCurrentToken() = Unit
            },
        )
    }

    private fun fillForm(viewModel: RegisterViewModel, password: String) {
        viewModel.onNameChanged("Marta")
        viewModel.onUsernameChanged("marta")
        viewModel.onEmailChanged("user@example.com")
        viewModel.onTermsChecked(true)
        viewModel.onPasswordChanged(password)
    }
}

private class AvailableUsernames : UsernameAvailabilityRepository {
    override suspend fun check(raw: String): DataResult<UsernameAvailability, com.marta.habittracker.domain.model.AppError> =
        DataResult.Success(UsernameAvailability(normalized = raw, available = true))
}

private class TakenThenFreeUsernames(
    private val taken: String,
) : UsernameAvailabilityRepository {
    override suspend fun check(raw: String): DataResult<UsernameAvailability, com.marta.habittracker.domain.model.AppError> {
        return if (raw == taken) {
            DataResult.Success(
                UsernameAvailability(
                    normalized = raw,
                    available = false,
                    suggestions = listOf("${raw}1", "${raw}2", "${raw}_bloom"),
                ),
            )
        } else {
            DataResult.Success(UsernameAvailability(normalized = raw, available = true))
        }
    }
}
