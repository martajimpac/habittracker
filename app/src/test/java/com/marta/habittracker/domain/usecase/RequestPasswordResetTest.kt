package com.marta.habittracker.domain.usecase

import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.model.AppError
import com.marta.habittracker.domain.model.RegisterError
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RequestPasswordResetTest {

    @Test
    fun `invoke returns InvalidEmail when email is blank`() = runTest {
        val repository = FakeAuthRepository()
        val useCase = RequestPasswordReset(repository)

        val result = useCase("   ")

        assertEquals(DataResult.Error(RegisterError.InvalidEmail), result)
        assertEquals(0, repository.requestPasswordResetCalls)
    }

    @Test
    fun `invoke returns InvalidEmail when email format is invalid`() = runTest {
        val repository = FakeAuthRepository()
        val useCase = RequestPasswordReset(repository)

        val result = useCase("not-an-email")

        assertEquals(DataResult.Error(RegisterError.InvalidEmail), result)
        assertEquals(0, repository.requestPasswordResetCalls)
    }

    @Test
    fun `invoke trims email and calls repository`() = runTest {
        val repository = FakeAuthRepository()
        val useCase = RequestPasswordReset(repository)

        val result = useCase("  user@example.com  ")

        assertTrue(result is DataResult.Success)
        assertEquals(1, repository.requestPasswordResetCalls)
        assertEquals("user@example.com", repository.lastPasswordResetEmail)
    }

    @Test
    fun `invoke returns repository error`() = runTest {
        val repository = FakeAuthRepository(
            requestPasswordResetResult = DataResult.Error(AppError.Common.Network),
        )
        val useCase = RequestPasswordReset(repository)

        val result = useCase("user@example.com")

        assertEquals(DataResult.Error(AppError.Common.Network), result)
    }
}
