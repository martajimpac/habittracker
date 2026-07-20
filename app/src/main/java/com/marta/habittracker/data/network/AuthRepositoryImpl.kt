package com.marta.habittracker.data.network

import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.models.AppError
import com.marta.habittracker.domain.models.LoginError
import com.marta.habittracker.domain.models.RegisterError
import com.marta.habittracker.domain.models.User
import com.marta.habittracker.domain.models.UserMode
import com.marta.habittracker.domain.repository.AuthRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.auth.providers.builtin.Email
import java.io.IOException
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient,
) : AuthRepository {

    override suspend fun doLogin(
        email: String,
        password: String,
    ): DataResult<User, AppError> {
        return try {
            supabase.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            currentUserResult()
        } catch (e: Exception) {
            DataResult.Error(mapError(e))
        }
    }

    override suspend fun doRegister(
        name: String,
        email: String,
        password: String,
    ): DataResult<User, AppError> {
        return try {
            supabase.auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
            when (val result = currentUserResult()) {
                is DataResult.Success -> DataResult.Success(result.data.copy(name = name))
                is DataResult.Error -> result
            }
        } catch (e: Exception) {
            DataResult.Error(mapRegisterError(e))
        }
    }

    override suspend fun isLoggedIn(): Boolean {
        return supabase.auth.currentUserOrNull() != null
    }

    private fun currentUserResult(): DataResult<User, AppError> {
        val currentUser = supabase.auth.currentUserOrNull()
        return if (currentUser != null) {
            DataResult.Success(
                User(
                    userId = currentUser.id,
                    name = "",
                    nickname = "",
                    followers = 0,
                    following = emptyList(),
                    userMode = UserMode.REGULAR_USER,
                    verified = false
                )
            )
        } else {
            DataResult.Error(AppError.Common.Unknown)
        }
    }

    private fun mapError(e: Exception): AppError =
        when (e) {
            is AuthRestException -> {
                when (e.error) {
                    "invalid_credentials" -> LoginError.InvalidCredentials
                    "email_not_confirmed" -> LoginError.EmailNotVerified
                    else -> AppError.Common.Unknown
                }
            }
            is IOException -> AppError.Common.Network
            else -> AppError.Common.Unknown
        }

    private fun mapRegisterError(e: Exception): AppError =
        when (e) {
            is AuthRestException -> {
                when (e.error) {
                    "user_already_exists", "email_exists" -> RegisterError.EmailAlreadyRegistered
                    "weak_password" -> RegisterError.WeakPassword
                    else -> AppError.Common.Unknown
                }
            }
            is IOException -> AppError.Common.Network
            else -> AppError.Common.Unknown
        }
}
