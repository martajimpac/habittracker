package com.marta.habittracker.data.repository

import android.util.Log
import com.marta.habittracker.core.push.DeviceTokenRegistration
import com.marta.habittracker.data.mapper.toDomain
import com.marta.habittracker.data.remote.supabase.datasources.AuthRemoteDataSource
import com.marta.habittracker.data.remote.supabase.datasources.AuthRemoteException
import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.model.User
import com.marta.habittracker.domain.model.errors.AppError
import com.marta.habittracker.domain.model.errors.LoginError
import com.marta.habittracker.domain.model.errors.RegisterError
import com.marta.habittracker.domain.repository.AuthRepository
import com.marta.habittracker.domain.repository.HabitRepository
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authRemote: AuthRemoteDataSource,
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
            val user = authRemote.signIn(email, password)
                ?: return DataResult.Error(AppError.Common.Unknown)

            DataResult.Success(user.toDomain())
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: AuthRemoteException) {
            DataResult.Error(mapAuthError(exception.errorCode))
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
        name: String,
    ): DataResult<User, AppError> {
        return try {
            Log.d(TAG, "Register started")
            val user = authRemote.signUp(email, password, username, name)
            Log.d(TAG, "Register finished")

            if (user == null) {
                return DataResult.Error(RegisterError.EmailConfirmationRequired)
            }

            DataResult.Success(user.toDomain())
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: AuthRemoteException) {
            DataResult.Error(mapRegisterError(exception.errorCode))
        } catch (_: IOException) {
            DataResult.Error(AppError.Common.Network)
        } catch (_: Exception) {
            DataResult.Error(AppError.Common.Unknown)
        }
    }

    override suspend fun requestPasswordReset(email: String): DataResult<Unit, AppError> {
        return try {
            Log.d(TAG, "Password reset request started")
            authRemote.requestPasswordReset(email)
            Log.d(TAG, "Password reset request finished")
            DataResult.Success(Unit)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: AuthRemoteException) {
            Log.e(TAG, "Password reset request failed: auth error", exception)
            DataResult.Error(mapAuthError(exception.errorCode))
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
            authRemote.updatePassword(newPassword)
            Log.d(TAG, "Password update finished")
            DataResult.Success(Unit)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: AuthRemoteException) {
            Log.e(TAG, "Password update failed: auth error", exception)
            DataResult.Error(mapAuthError(exception.errorCode))
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
            val loggedIn = authRemote.currentUser() != null
            Log.d(TAG, "isLoggedIn check: ${if (loggedIn) "YES" else "NO"}")
            loggedIn
        } catch (e: Exception) {
            Log.e(TAG, "Error checking login status", e)
            false
        }
    }

    override suspend fun getCurrentUserId(): String? {
        return authRemote.currentUser()?.id
    }

    override suspend fun getCurrentUserEmail(): String {
        return authRemote.currentUser()?.email.orEmpty()
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
            authRemote.signOut()
            try {
                habitRepository.clearLocal()
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
