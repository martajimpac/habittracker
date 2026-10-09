package com.marta.habittracker.data.remote

import com.marta.habittracker.data.remote.dto.ProfileDto

/**
 * Own-profile Postgrest + Storage operations. Implemented by [SocialRemoteDataSource]
 * so [com.marta.habittracker.data.repository.ProfileRepositoryImpl] can be unit-tested
 * without a Supabase client.
 */
interface ProfileRemote {
    suspend fun fetchProfileById(id: String): ProfileDto?

    suspend fun updateMyProfile(displayName: String?, avatarUrl: String?): ProfileDto

    suspend fun uploadAvatar(jpegBytes: ByteArray): ProfileDto

    suspend fun clearAvatar(): ProfileDto

    suspend fun deleteAccount()
}
