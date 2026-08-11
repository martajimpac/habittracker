package com.marta.habittracker.domain.security

import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.model.AppError
import com.marta.habittracker.domain.model.LoginError
import com.marta.habittracker.domain.model.User
import com.marta.habittracker.domain.model.UserMode
import com.marta.habittracker.domain.repository.AuthRepository
import com.marta.habittracker.domain.usecase.LoginUseCase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LoginSecurityTest {

    @Test
    fun invalidEmailIsRejectedBeforeCallingRepository() = runBlocking {
        val repository = FakeAuthRepository()
        val useCase = LoginUseCase(repository)

        val result = useCase("not-an-email", "ValidPass1!")

        assertTrue(result is DataResult.Error)
        assertEquals(LoginError.InvalidCredentials, (result as DataResult.Error).error)
        assertEquals(0, repository.calls)
    }

    private class FakeAuthRepository : AuthRepository {
        var calls = 0

        override suspend fun doLogin(email: String, password: String): DataResult<User, AppError> {
            calls++
            return DataResult.Success(
                User(
                    userId = "user-1",
                    name = "Secure User",
                    nickname = "secure",
                    followers = 0,
                    following = emptyList(),
                    userMode = UserMode.RegularUser,
                    verified = false
                )
            )
        }

        override suspend fun doRegister(
            email: String,
            password: String,
        ): DataResult<User, AppError> {
            calls++
            return DataResult.Error(AppError.Common.Unknown)
        }

        override suspend fun requestPasswordReset(email: String): DataResult<Unit, AppError> =
            DataResult.Success(Unit)

        override suspend fun updatePassword(newPassword: String): DataResult<Unit, AppError> =
            DataResult.Success(Unit)

        override suspend fun isLoggedIn(): Boolean = false

        override suspend fun getCurrentUserDisplayName(): String = "Secure User"

        override suspend fun getCurrentUserEmail(): String = "secure@example.com"

        override suspend fun signOut(): DataResult<Unit, AppError> = DataResult.Success(Unit)
    }
}
