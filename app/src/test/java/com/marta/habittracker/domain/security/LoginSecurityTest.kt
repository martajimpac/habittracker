package com.marta.habittracker.domain.security

import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.model.LoginError
import com.marta.habittracker.domain.usecase.FakeAuthRepository
import com.marta.habittracker.domain.usecase.LoginUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LoginSecurityTest {

    @Test
    fun weakPasswordsAreRejectedBeforeCallingRepository() = runTest {
        val repository = FakeAuthRepository()
        val useCase = LoginUseCase(repository)

        val weakPasswords = listOf(
            "",
            "12345",
            "pass",
            "qwert",
            "abc",
        )

        weakPasswords.forEach { weakPassword ->
            val result = useCase("secure.user@example.com", weakPassword)

            assertTrue(result is DataResult.Error)
            assertEquals(LoginError.InvalidCredentials, (result as DataResult.Error).error)
        }

        assertEquals(
            "Weak credentials should be rejected locally and never sent to network/backend",
            0,
            repository.loginCalls,
        )
    }
}
