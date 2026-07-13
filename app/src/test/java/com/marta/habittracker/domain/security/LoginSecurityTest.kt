package com.marta.habittracker.domain.security

import com.marta.habittracker.domain.models.User
import com.marta.habittracker.domain.models.UserMode
import com.marta.habittracker.domain.repository.AuthRepository
import com.marta.habittracker.domain.usecase.LoginUseCase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LoginSecurityTest {

    @Test
    fun weakPasswordsAreRejectedBeforeCallingRepository() = runBlocking {
        val repository = FakeAuthRepository()
        val useCase = LoginUseCase(repository)

        val weakPasswords = listOf(
            "",
            "123456",
            "password",
            "qwerty12",
            "abcdefg",
            "11111111"
        )

        weakPasswords.forEach { weakPassword ->
            val result = useCase("secure.user@example.com", weakPassword)

            assertNull("Weak password must not authenticate: $weakPassword", result)
        }

        assertEquals(
            "Weak credentials should be rejected locally and never sent to network/backend",
            0,
            repository.calls
        )
    }

    private class FakeAuthRepository : AuthRepository {
        var calls = 0

        override suspend fun doLogin(user: String, password: String): List<User> {
            calls++
            return listOf(
                User(
                    userId = "user-1",
                    name = "Secure User",
                    nickname = "secure",
                    followers = 0,
                    following = emptyList(),
                    userMode = UserMode.REGULAR_USER,
                    verified = false
                )
            )
        }
    }
}
