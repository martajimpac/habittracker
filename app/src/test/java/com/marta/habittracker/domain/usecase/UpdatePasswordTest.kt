package com.marta.habittracker.domain.usecase

import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.model.AppError
import com.marta.habittracker.domain.model.RegisterError
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdatePasswordTest {

    @Test
    fun `invoke returns WeakPassword when password is invalid`() = runTest {
        val repository = FakeAuthRepository()
        val useCase = UpdatePassword(repository)

        val result = useCase("short")

        assertEquals(DataResult.Error(RegisterError.WeakPassword), result)
        assertEquals(0, repository.updatePasswordCalls)
        assertEquals(0, repository.signOutCalls)
    }

    @Test
    fun `invoke updates password then signs out on success`() = runTest {
        val repository = FakeAuthRepository()
        val useCase = UpdatePassword(repository)

        val result = useCase("Secret12")

        assertTrue(result is DataResult.Success)
        assertEquals(1, repository.updatePasswordCalls)
        assertEquals("Secret12", repository.lastUpdatedPassword)
        assertEquals(1, repository.signOutCalls)
    }

    @Test
    fun `invoke does not sign out when update fails`() = runTest {
        val repository = FakeAuthRepository(
            updatePasswordResult = DataResult.Error(AppError.Common.Network),
        )
        val useCase = UpdatePassword(repository)

        val result = useCase("Secret12")

        assertEquals(DataResult.Error(AppError.Common.Network), result)
        assertEquals(1, repository.updatePasswordCalls)
        assertEquals(0, repository.signOutCalls)
    }

    @Test
    fun `invoke returns signOut error after successful update`() = runTest {
        val repository = FakeAuthRepository(
            signOutResult = DataResult.Error(AppError.Common.Unknown),
        )
        val useCase = UpdatePassword(repository)

        val result = useCase("Secret12")

        assertEquals(DataResult.Error(AppError.Common.Unknown), result)
        assertEquals(1, repository.updatePasswordCalls)
        assertEquals(1, repository.signOutCalls)
    }
}
