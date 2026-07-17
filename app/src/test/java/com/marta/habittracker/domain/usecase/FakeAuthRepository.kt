package com.marta.habittracker.domain.usecase

import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.model.AppError
import com.marta.habittracker.domain.model.User
import com.marta.habittracker.domain.model.UserMode
import com.marta.habittracker.domain.repository.AuthRepository

class FakeAuthRepository(
    private val loginResult: DataResult<User, AppError> = DataResult.Success(defaultUser),
    private val registerResult: DataResult<User, AppError> = DataResult.Success(defaultUser),
) : AuthRepository {

    var loginCalls: Int = 0
        private set
    var registerCalls: Int = 0
        private set
    var lastLoginEmail: String? = null
        private set
    var lastLoginPassword: String? = null
        private set
    var lastRegisterEmail: String? = null
        private set
    var lastRegisterPassword: String? = null
        private set

    override suspend fun doLogin(
        email: String,
        password: String,
    ): DataResult<User, AppError> {
        loginCalls++
        lastLoginEmail = email
        lastLoginPassword = password
        return loginResult
    }

    override suspend fun doRegister(
        email: String,
        password: String,
    ): DataResult<User, AppError> {
        registerCalls++
        lastRegisterEmail = email
        lastRegisterPassword = password
        return registerResult
    }

    companion object {
        val defaultUser = User(
            userId = "user-1",
            name = "Test User",
            nickname = "test@example.com",
            followers = 0,
            following = emptyList(),
            userMode = UserMode.RegularUser,
            verified = true,
        )
    }
}
