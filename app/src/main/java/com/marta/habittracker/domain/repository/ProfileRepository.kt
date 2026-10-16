package com.marta.habittracker.domain.repository

import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.model.AppError
import com.marta.habittracker.domain.model.Profile

interface ProfileRepository {
    suspend fun getMyProfile(): DataResult<Profile, AppError>

    suspend fun updateDisplayName(displayName: String): DataResult<Profile, AppError>

    suspend fun uploadAvatar(jpegBytes: ByteArray): DataResult<Profile, AppError>

    suspend fun clearAvatar(): DataResult<Profile, AppError>

    suspend fun deleteAccount(): DataResult<Unit, AppError>
}
