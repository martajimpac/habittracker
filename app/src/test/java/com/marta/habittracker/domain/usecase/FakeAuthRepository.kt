package com.marta.habittracker.domain.usecase

import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.model.AppError
import com.marta.habittracker.domain.model.User
import com.marta.habittracker.domain.model.UserMode
import com.marta.habittracker.domain.repository.AuthRepository

class FakeAuthRepository(
    private val loginResult: DataResult<User, AppError> = DataResult.Success(defaultUser),
    private val registerResult: DataResult<User, AppError> = DataResult.Success(defaultUser),
    private val requestPasswordResetResult: DataResult<Unit, AppError> = DataResult.Success(Unit),
    private val updatePasswordResult: DataResult<Unit, AppError> = DataResult.Success(Unit),
    private val signOutResult: DataResult<Unit, AppError> = DataResult.Success(Unit),
    private val loggedIn: Boolean = false,
    private val displayName: String = defaultUser.name,
    private val email: String = defaultUser.nickname,
) : AuthRepository {

    var loginCalls: Int = 0
        private set
    var registerCalls: Int = 0
        private set
    var requestPasswordResetCalls: Int = 0
        private set
    var updatePasswordCalls: Int = 0
        private set
    var signOutCalls: Int = 0
        private set
    var lastLoginEmail: String? = null
        private set
    var lastLoginPassword: String? = null
        private set
    var lastRegisterEmail: String? = null
        private set
    var lastRegisterPassword: String? = null
        private set
    var lastPasswordResetEmail: String? = null
        private set
    var lastUpdatedPassword: String? = null
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

    override suspend fun requestPasswordReset(email: String): DataResult<Unit, AppError> {
        requestPasswordResetCalls++
        lastPasswordResetEmail = email
        return requestPasswordResetResult
    }

    override suspend fun updatePassword(newPassword: String): DataResult<Unit, AppError> {
        updatePasswordCalls++
        lastUpdatedPassword = newPassword
        return updatePasswordResult
    }

    override suspend fun isLoggedIn(): Boolean = loggedIn

    override suspend fun getCurrentUserDisplayName(): String = displayName

    override suspend fun getCurrentUserEmail(): String = email

    override suspend fun signOut(): DataResult<Unit, AppError> {
        signOutCalls++
        return signOutResult
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
