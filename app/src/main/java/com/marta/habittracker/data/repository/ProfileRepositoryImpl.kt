package com.marta.habittracker.data.repository

import android.util.Log
import com.marta.habittracker.core.network.NetworkChecker
import com.marta.habittracker.data.mapper.SocialMapper
import com.marta.habittracker.data.remote.ProfileRemote
import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.model.AppError
import com.marta.habittracker.domain.model.Profile
import com.marta.habittracker.domain.repository.AuthRepository
import com.marta.habittracker.domain.repository.ProfileRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

/**
 * Own-profile reads/writes. Online-first: when offline, returns [AppError.Common.Network]
 * without calling remote.
 */
@Singleton
class ProfileRepositoryImpl @Inject constructor(
    private val networkChecker: NetworkChecker,
    private val profileRemote: ProfileRemote,
    private val socialMapper: SocialMapper,
    private val authRepository: AuthRepository,
) : ProfileRepository {

    override suspend fun getMyProfile(): DataResult<Profile, AppError> {
        if (!networkChecker.isOnline()) {
            Log.w(TAG, "$OP_GET_MY_PROFILE skipped: offline")
            return DataResult.Error(AppError.Common.Network)
        }
        val userId = authRepository.getCurrentUserId()
        if (userId == null) {
            Log.w(TAG, "$OP_GET_MY_PROFILE failed: no authenticated user")
            return DataResult.Error(AppError.Common.Unauthorized)
        }
        return runCatchingRemote(OP_GET_MY_PROFILE) {
            val dto = profileRemote.fetchProfileById(userId)
                ?: error("Missing profile row")
            with(socialMapper) { dto.toDomain() }
        }
    }

    override suspend fun updateDisplayName(displayName: String): DataResult<Profile, AppError> =
        runOnline(OP_UPDATE_DISPLAY_NAME) {
            val dto = profileRemote.updateMyProfile(
                displayName = displayName,
                avatarUrl = null,
            )
            with(socialMapper) { dto.toDomain() }
        }

    override suspend fun uploadAvatar(jpegBytes: ByteArray): DataResult<Profile, AppError> =
        runOnline(OP_UPLOAD_AVATAR) {
            val dto = profileRemote.uploadAvatar(jpegBytes)
            with(socialMapper) { dto.toDomain() }
        }

    override suspend fun clearAvatar(): DataResult<Profile, AppError> =
        runOnline(OP_CLEAR_AVATAR) {
            val dto = profileRemote.clearAvatar()
            with(socialMapper) { dto.toDomain() }
        }

    override suspend fun deleteAccount(): DataResult<Unit, AppError> {
        if (!networkChecker.isOnline()) {
            Log.w(TAG, "$OP_DELETE_ACCOUNT skipped: offline")
            return DataResult.Error(AppError.Common.Network)
        }
        return try {
            Log.d(TAG, "$OP_DELETE_ACCOUNT started")
            try {
                profileRemote.deleteAccount()
            } catch (e: CancellationException) {
                throw e
            }
            withContext(NonCancellable) {
                val signOutResult = authRepository.signOut()
                if (signOutResult is DataResult.Error) {
                    Log.w(TAG, "$OP_DELETE_ACCOUNT remote ok; local signOut failed")
                }
                Log.d(TAG, "$OP_DELETE_ACCOUNT finished")
                DataResult.Success(Unit)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "$OP_DELETE_ACCOUNT failed", e)
            DataResult.Error(AppError.Common.Unknown)
        }
    }

    private suspend fun <T> runOnline(
        operation: String,
        block: suspend () -> T,
    ): DataResult<T, AppError> {
        if (!networkChecker.isOnline()) {
            Log.w(TAG, "$operation skipped: offline")
            return DataResult.Error(AppError.Common.Network)
        }
        return runCatchingRemote(operation, block)
    }

    private suspend fun <T> runCatchingRemote(
        operation: String,
        block: suspend () -> T,
    ): DataResult<T, AppError> {
        return try {
            Log.d(TAG, "$operation started")
            val result = block()
            Log.d(TAG, "$operation finished")
            DataResult.Success(result)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "$operation failed", e)
            DataResult.Error(AppError.Common.Unknown)
        }
    }

    companion object {
        private const val TAG = "ProfileRepository"
        private const val OP_GET_MY_PROFILE = "getMyProfile"
        private const val OP_UPDATE_DISPLAY_NAME = "updateDisplayName"
        private const val OP_UPLOAD_AVATAR = "uploadAvatar"
        private const val OP_CLEAR_AVATAR = "clearAvatar"
        private const val OP_DELETE_ACCOUNT = "deleteAccount"
    }
}
