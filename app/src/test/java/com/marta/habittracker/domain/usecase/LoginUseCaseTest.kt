package com.marta.habittracker.domain.usecase

import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.model.AppError
import com.marta.habittracker.domain.model.LoginError
import com.marta.habittracker.domain.model.User
import com.marta.habittracker.domain.model.UserMode
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class LoginUseCaseTest {

    @Test
    fun `invoke returns InvalidCredentials when email is blank`() = runTest {
        val repository = FakeAuthRepository()
        val useCase = LoginUseCase(repository)

        val result = useCase(email = "   ", password = "Secret123")

        assertEquals(DataResult.Error(LoginError.InvalidCredentials), result)
        assertEquals(0, repository.loginCalls)
    }

    @Test
    fun `invoke returns InvalidCredentials when email format is invalid`() = runTest {
        val repository = FakeAuthRepository()
        val useCase = LoginUseCase(repository)

        val result = useCase(email = "not-an-email", password = "Secret123")

        assertEquals(DataResult.Error(LoginError.InvalidCredentials), result)
        assertEquals(0, repository.loginCalls)
    }

    @Test
    fun `invoke returns InvalidCredentials when password is shorter than 6 characters`() = runTest {
        val repository = FakeAuthRepository()
        val useCase = LoginUseCase(repository)

        val result = useCase(email = "user@example.com", password = "Sec12")

        assertEquals(DataResult.Error(LoginError.InvalidCredentials), result)
        assertEquals(0, repository.loginCalls)
    }

    @Test
    fun `invoke returns InvalidCredentials when password lacks uppercase`() = runTest {
        val repository = FakeAuthRepository()
        val useCase = LoginUseCase(repository)

        val result = useCase(email = "user@example.com", password = "secret123")

        assertEquals(DataResult.Error(LoginError.InvalidCredentials), result)
        assertEquals(0, repository.loginCalls)
    }

    @Test
    fun `invoke returns InvalidCredentials when password lacks digit`() = runTest {
        val repository = FakeAuthRepository()
        val useCase = LoginUseCase(repository)

        val result = useCase(email = "user@example.com", password = "SecretPassword")

        assertEquals(DataResult.Error(LoginError.InvalidCredentials), result)
        assertEquals(0, repository.loginCalls)
    }

    @Test
    fun `invoke returns Success when credentials are valid`() = runTest {
        val expectedUser = User(
            userId = "auth-user",
            name = "Marta",
            nickname = "marta@example.com",
            followers = 3,
            following = listOf("friend-1"),
            userMode = UserMode.ContentCreatorUser,
            verified = true,
        )
        val repository = FakeAuthRepository(loginResult = DataResult.Success(expectedUser))
        val useCase = LoginUseCase(repository)

        val result = useCase(email = "marta@example.com", password = "Secret123")

        assertEquals(DataResult.Success(expectedUser), result)
        assertEquals(1, repository.loginCalls)
    }

    @Test
    fun `invoke returns repository error when authentication fails`() = runTest {
        val repository = FakeAuthRepository(
            loginResult = DataResult.Error(LoginError.EmailNotVerified),
        )
        val useCase = LoginUseCase(repository)

        val result = useCase(email = "user@example.com", password = "Secret123")

        assertEquals(DataResult.Error(LoginError.EmailNotVerified), result)
        assertEquals(1, repository.loginCalls)
    }

    @Test
    fun `invoke returns network error from repository`() = runTest {
        val repository = FakeAuthRepository(
            loginResult = DataResult.Error(AppError.Common.Network),
        )
        val useCase = LoginUseCase(repository)

        val result = useCase(email = "user@example.com", password = "Secret123")

        assertEquals(DataResult.Error(AppError.Common.Network), result)
        assertEquals(1, repository.loginCalls)
    }

    @Test
    fun `invoke trims email before calling repository`() = runTest {
        val repository = FakeAuthRepository()
        val useCase = LoginUseCase(repository)

        useCase(email = "  user@example.com  ", password = "Secret123")

        assertEquals("user@example.com", repository.lastLoginEmail)
        assertEquals("Secret123", repository.lastLoginPassword)
    }
}
