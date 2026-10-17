package com.marta.habittracker.presentation.screens.auth.register

import android.app.Application
import com.marta.habittracker.R
import com.marta.habittracker.core.push.DeviceTokenRegistration
import com.marta.habittracker.domain.usecase.FakeAuthRepository
import com.marta.habittracker.domain.usecase.RegisterUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.resetMain
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
    fun `register enables when password has 8 characters a letter and a digit`() {
        val viewModel = viewModel()

        fillForm(viewModel, password = "abcdefg1")

        assertTrue(viewModel.uiState.value.isRegisterEnabled)
        assertNull(viewModel.uiState.value.passwordErrorRes)
    }

    @Test
    fun `password shows the policy error while it is still empty`() {
        val viewModel = viewModel()

        assertEquals(R.string.error_register_weak_password, viewModel.uiState.value.passwordErrorRes)
    }

    private fun viewModel(): RegisterViewModel {
        return RegisterViewModel(
            registerUseCase = RegisterUseCase(FakeAuthRepository()),
            deviceTokenRegistration = object : DeviceTokenRegistration {
                override suspend fun registerCurrentToken() = Unit
                override suspend fun clearCurrentToken() = Unit
            },
        )
    }

    private fun fillForm(viewModel: RegisterViewModel, password: String) {
        viewModel.onNameChanged("Marta")
        viewModel.onEmailChanged("user@example.com")
        viewModel.onTermsChecked(true)
        viewModel.onPasswordChanged(password)
    }
}
