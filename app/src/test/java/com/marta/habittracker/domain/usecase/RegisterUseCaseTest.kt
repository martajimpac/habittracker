package com.marta.habittracker.domain.usecase

import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.model.AppError
import com.marta.habittracker.domain.model.RegisterError
import com.marta.habittracker.domain.model.User
import com.marta.habittracker.domain.model.UserMode
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RegisterUseCaseTest {

    @Test
    fun `invoke returns InvalidEmail when email is blank`() = runTest {
        val repository = FakeAuthRepository()
        val useCase = RegisterUseCase(repository)

        val result = useCase(email = "   ", password = "Secret12")

        assertEquals(DataResult.Error(RegisterError.InvalidEmail), result)
        assertEquals(0, repository.registerCalls)
    }

    @Test
    fun `invoke returns InvalidEmail when email format is invalid`() = runTest {
        val repository = FakeAuthRepository()
        val useCase = RegisterUseCase(repository)

        val result = useCase(email = "invalid-email", password = "Secret12")

        assertEquals(DataResult.Error(RegisterError.InvalidEmail), result)
        assertEquals(0, repository.registerCalls)
    }

    @Test
    fun `invoke returns WeakPassword when password is too short`() = runTest {
        val repository = FakeAuthRepository()
        val useCase = RegisterUseCase(repository)

        val result = useCase(email = "user@example.com", password = "Ab1")

        assertEquals(DataResult.Error(RegisterError.WeakPassword), result)
        assertEquals(0, repository.registerCalls)
    }

    @Test
    fun `invoke returns WeakPassword when password has no digits`() = runTest {
        val repository = FakeAuthRepository()
        val useCase = RegisterUseCase(repository)

        val result = useCase(email = "user@example.com", password = "Password")

        assertEquals(DataResult.Error(RegisterError.WeakPassword), result)
        assertEquals(0, repository.registerCalls)
    }

    @Test
    fun `invoke returns WeakPassword when password has no letters`() = runTest {
        val repository = FakeAuthRepository()
        val useCase = RegisterUseCase(repository)

        val result = useCase(email = "user@example.com", password = "12345678")

        assertEquals(DataResult.Error(RegisterError.WeakPassword), result)
        assertEquals(0, repository.registerCalls)
    }

    @Test
    fun `invoke returns Success when credentials are valid`() = runTest {
        val expectedUser = User(
            userId = "new-user",
            name = "Marta",
            nickname = "marta@example.com",
            followers = 0,
            following = emptyList(),
            userMode = UserMode.RegularUser,
            verified = false,
        )
        val repository = FakeAuthRepository(registerResult = DataResult.Success(expectedUser))
        val useCase = RegisterUseCase(repository)

        val result = useCase(email = "marta@example.com", password = "Secret12")

        assertEquals(DataResult.Success(expectedUser), result)
        assertEquals(1, repository.registerCalls)
    }

    @Test
    fun `invoke returns repository error when email is already registered`() = runTest {
        val repository = FakeAuthRepository(
            registerResult = DataResult.Error(RegisterError.EmailAlreadyRegistered),
        )
        val useCase = RegisterUseCase(repository)

        val result = useCase(email = "user@example.com", password = "Secret12")

        assertEquals(DataResult.Error(RegisterError.EmailAlreadyRegistered), result)
        assertEquals(1, repository.registerCalls)
    }

    @Test
    fun `invoke trims email before calling repository`() = runTest {
        val repository = FakeAuthRepository()
        val useCase = RegisterUseCase(repository)

        useCase(email = "  user@example.com  ", password = "Secret12")

        assertEquals("user@example.com", repository.lastRegisterEmail)
        assertEquals("Secret12", repository.lastRegisterPassword)
    }

    @Test
    fun `invoke accepts password with letters and digits at minimum length`() = runTest {
        val repository = FakeAuthRepository()
        val useCase = RegisterUseCase(repository)

        val result = useCase(email = "user@example.com", password = "Abcdef1g")

        assertTrue(result is DataResult.Success)
        assertEquals(1, repository.registerCalls)
    }
}
