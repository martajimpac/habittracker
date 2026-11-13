package com.marta.habittracker.data.remote.supabase.datasources

import android.util.Log
import com.marta.habittracker.core.auth.AuthLinks
import com.marta.habittracker.data.remote.supabase.dto.AuthUserDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.user.UserInfo
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Rejected Supabase Auth request. [errorCode] is the GoTrue code
 * (`invalid_credentials`, `user_already_exists`, ...), so callers never see Supabase types.
 */
class AuthRemoteException(
    val errorCode: String,
    cause: Throwable,
) : Exception("Auth request rejected: $errorCode", cause)

/**
 * Supabase Auth (GoTrue) calls. Throws [AuthRemoteException] for rejected requests;
 * network and other failures are rethrown as they are.
 */
@Singleton
class AuthRemoteDataSource @Inject constructor(
    private val supabase: SupabaseClient,
) {

    /** Returns the signed-in user, or null if Supabase reports no session afterwards. */
    suspend fun signIn(email: String, password: String): AuthUserDto? = authCall("signIn") {
        supabase.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
        currentUser()
    }

    /** Returns the new user, or null when the account must confirm its email first. */
    suspend fun signUp(
        email: String,
        password: String,
        username: String,
        name: String,
    ): AuthUserDto? = authCall("signUp") {
        supabase.auth.signUpWith(Email, redirectUrl = AuthLinks.CONFIRM_URL) {
            this.email = email
            this.password = password
            data = buildJsonObject {
                put("username", username)
                put("name", name)
            }
        }
        currentUser()
    }

    suspend fun requestPasswordReset(email: String) = authCall("requestPasswordReset") {
        supabase.auth.resetPasswordForEmail(
            email = email,
            redirectUrl = AuthLinks.RESET_URL,
        )
    }

    suspend fun updatePassword(newPassword: String) = authCall("updatePassword") {
        supabase.auth.updateUser {
            password = newPassword
        }
    }

    suspend fun signOut() = authCall("signOut") {
        supabase.auth.signOut()
    }

    fun currentUser(): AuthUserDto? = supabase.auth.currentUserOrNull()?.toDto()

    private suspend fun <T> authCall(operation: String, block: suspend () -> T): T {
        return try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: AuthRestException) {
            Log.e(TAG, "$operation rejected code=${e.error}", e)
            throw AuthRemoteException(e.error, e)
        } catch (e: Exception) {
            Log.e(TAG, "$operation failed", e)
            throw e
        }
    }

    private fun UserInfo.toDto(): AuthUserDto =
        AuthUserDto(
            id = id,
            email = email,
            name = userMetadata?.get("name")?.toString().orEmpty(),
            emailConfirmed = emailConfirmedAt != null,
        )

    private companion object {
        const val TAG = "AuthRemoteDS"
    }
}
