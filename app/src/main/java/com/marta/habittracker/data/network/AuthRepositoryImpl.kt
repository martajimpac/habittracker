package com.marta.habittracker.data.network

import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.model.AppError
import com.marta.habittracker.domain.model.LoginError
import com.marta.habittracker.domain.model.User
import com.marta.habittracker.domain.model.UserMode
import com.marta.habittracker.domain.repository.AuthRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.auth.providers.builtin.Email
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
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

            val currentUser = supabase.auth.currentUserOrNull()
                ?: return DataResult.Error(AppError.Common.Unknown)

            DataResult.Success(
                User(
                    userId = currentUser.id,
                    name = currentUser.userMetadata?.get("name")?.toString().orEmpty(),
                    nickname = currentUser.email.orEmpty(),
                    followers = 0,
                    following = emptyList(),
                    userMode = UserMode.RegularUser,
                    verified = !currentUser.email.isNullOrBlank(),
                ),
            )
        } catch (exception: AuthRestException) {
            DataResult.Error(mapAuthError(exception.error))
        } catch (_: IOException) {
            DataResult.Error(AppError.Common.Network)
        } catch (_: Exception) {
            DataResult.Error(AppError.Common.Unknown)
        }
    }

    private fun mapAuthError(errorCode: String): AppError = when (errorCode) {
        "invalid_credentials" -> LoginError.InvalidCredentials
        "email_not_confirmed" -> LoginError.EmailNotVerified
        else -> AppError.Common.Unknown
    }
}
