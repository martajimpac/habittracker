package com.marta.habittracker.presentation.screens.profile

import com.marta.habittracker.domain.usecase.FakeAuthRepository
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

        val viewModel = ProfileViewModel(authRepository)
        advanceUntilIdle()

        assertEquals("Alex Rivera", viewModel.displayName.value)
        assertEquals("alex@example.com", viewModel.email.value)
    }
}
