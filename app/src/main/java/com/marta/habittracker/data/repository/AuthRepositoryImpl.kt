package com.marta.habittracker.data.repository

import android.util.Log
import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.model.AppError
import com.marta.habittracker.domain.model.LoginError
import com.marta.habittracker.domain.model.RegisterError
import com.marta.habittracker.domain.model.User
import com.marta.habittracker.domain.model.UserMode
import com.marta.habittracker.core.auth.AuthLinks
import com.marta.habittracker.core.push.DeviceTokenRegistration
import com.marta.habittracker.domain.repository.AuthRepository
import com.marta.habittracker.domain.repository.HabitRepository
import com.marta.habittracker.domain.widget.onSignedOut
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.user.UserInfo
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient,
    private val deviceTokenRegistration: DeviceTokenRegistration,
    private val habitRepository: HabitRepository,
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
        username: String,
    ): DataResult<User, AppError> {
        return try {
            Log.d(TAG, "Register started")
            supabase.auth.signUpWith(Email, redirectUrl = AuthLinks.CONFIRM_URL) {
                this.email = email
                this.password = password
                data = buildJsonObject {
                    put("username", username)
                }
            }
            Log.d(TAG, "Register finished")

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

    override suspend fun requestPasswordReset(email: String): DataResult<Unit, AppError> {
        return try {
            Log.d(TAG, "Password reset request started")
            supabase.auth.resetPasswordForEmail(
                email = email,
                redirectUrl = AuthLinks.RESET_URL,
            )
            Log.d(TAG, "Password reset request finished")
            DataResult.Success(Unit)
        } catch (exception: AuthRestException) {
            Log.e(TAG, "Password reset request failed: auth error", exception)
            DataResult.Error(mapAuthError(exception.error))
        } catch (exception: IOException) {
            Log.e(TAG, "Password reset request failed: network", exception)
            DataResult.Error(AppError.Common.Network)
        } catch (exception: Exception) {
            Log.e(TAG, "Password reset request failed", exception)
            DataResult.Error(AppError.Common.Unknown)
        }
    }

    override suspend fun updatePassword(newPassword: String): DataResult<Unit, AppError> {
        return try {
            Log.d(TAG, "Password update started")
            supabase.auth.updateUser {
                password = newPassword
            }
            Log.d(TAG, "Password update finished")
            DataResult.Success(Unit)
        } catch (exception: AuthRestException) {
            Log.e(TAG, "Password update failed: auth error", exception)
            DataResult.Error(mapAuthError(exception.error))
        } catch (exception: IOException) {
            Log.e(TAG, "Password update failed: network", exception)
            DataResult.Error(AppError.Common.Network)
        } catch (exception: Exception) {
            Log.e(TAG, "Password update failed", exception)
            DataResult.Error(AppError.Common.Unknown)
        }
    }

    override suspend fun isLoggedIn(): Boolean {
        return try {
            val user = supabase.auth.currentUserOrNull()
            Log.d(TAG, "isLoggedIn check: ${if (user != null) "YES (${user.email})" else "NO"}")
            user != null
        } catch (e: Exception) {
            Log.e(TAG, "Error checking login status", e)
            false
        }
    }

    override suspend fun getCurrentUserId(): String? {
        return supabase.auth.currentUserOrNull()?.id
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
            try {
                deviceTokenRegistration.clearCurrentToken()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "clearCurrentToken before sign-out failed", e)
            }
            supabase.auth.signOut()
            try {
                onSignedOut(DataResult.Success(Unit)) {
                    habitRepository.clearLocal()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "clear local account after sign-out failed", e)
            }
            Log.d(TAG, "Sign out finished ok")
            DataResult.Success(Unit)
        } catch (_: IOException) {
            Log.e(TAG, "Sign out failed: network")
            DataResult.Error(AppError.Common.Network)
        } catch (e: CancellationException) {
            throw e
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
