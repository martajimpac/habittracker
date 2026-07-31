package com.marta.habittracker.data.repository

import android.util.Log
import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.model.AppError
import com.marta.habittracker.domain.model.LoginError
import com.marta.habittracker.domain.model.RegisterError
import com.marta.habittracker.domain.model.User
import com.marta.habittracker.domain.model.UserMode
import com.marta.habittracker.domain.repository.AuthRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.user.UserInfo
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient,
) : AuthRepository {

    companion object {
        private const val TAG = "AuthRepository"
    }

    override suspend fun doLogin(
        email: String,
        password: String,
    ): DataResult<User, AppError> {
        return try {
            supabase.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }

            val currentUser = supabase.auth.currentUserOrNull()
                ?: return DataResult.Error(AppError.Common.Unknown)

            DataResult.Success(mapUser(currentUser))
        } catch (exception: AuthRestException) {
            DataResult.Error(mapAuthError(exception.error))
        } catch (_: IOException) {
            DataResult.Error(AppError.Common.Network)
        } catch (_: Exception) {
            DataResult.Error(AppError.Common.Unknown)
        }
    }

    override suspend fun doRegister(
        email: String,
        password: String,
    ): DataResult<User, AppError> {
        return try {
            supabase.auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }

            val currentUser = supabase.auth.currentUserOrNull()
            if (currentUser == null) {
                return DataResult.Error(RegisterError.EmailConfirmationRequired)
            }

            DataResult.Success(mapUser(currentUser))
        } catch (exception: AuthRestException) {
            DataResult.Error(mapRegisterError(exception.error))
        } catch (_: IOException) {
            DataResult.Error(AppError.Common.Network)
        } catch (_: Exception) {
            DataResult.Error(AppError.Common.Unknown)
        }
    }

    override suspend fun isLoggedIn(): Boolean {
        return supabase.auth.currentUserOrNull() != null
    }

    override suspend fun getCurrentUserDisplayName(): String {
        val user = supabase.auth.currentUserOrNull() ?: return ""
        val metadataName = user.userMetadata?.get("name")?.toString()?.trim().orEmpty()
        if (metadataName.isNotBlank()) return metadataName
        return user.email?.substringBefore("@").orEmpty()
    }

    override suspend fun getCurrentUserEmail(): String {
        return supabase.auth.currentUserOrNull()?.email.orEmpty()
    }

    override suspend fun signOut(): DataResult<Unit, AppError> {
        return try {
            Log.d(TAG, "Sign out started")
            supabase.auth.signOut()
            Log.d(TAG, "Sign out finished ok")
            DataResult.Success(Unit)
        } catch (_: IOException) {
            Log.e(TAG, "Sign out failed: network")
            DataResult.Error(AppError.Common.Network)
        } catch (e: Exception) {
            Log.e(TAG, "Sign out failed", e)
            DataResult.Error(AppError.Common.Unknown)
        }
    }

    private fun mapUser(currentUser: UserInfo): User =
        User(
            userId = currentUser.id,
            name = currentUser.userMetadata?.get("name")?.toString().orEmpty(),
            nickname = currentUser.email.orEmpty(),
            followers = 0,
            following = emptyList(),
            userMode = UserMode.RegularUser,
            verified = currentUser.emailConfirmedAt != null,
        )

    private fun mapRegisterError(errorCode: String): AppError = when (errorCode) {
        "user_already_exists" -> RegisterError.EmailAlreadyRegistered
        "weak_password" -> RegisterError.WeakPassword
        else -> AppError.Common.Unknown
    }

    private fun mapAuthError(errorCode: String): AppError = when (errorCode) {
        "invalid_credentials" -> LoginError.InvalidCredentials
        "email_not_confirmed" -> LoginError.EmailNotVerified
        else -> AppError.Common.Unknown
    }
}
