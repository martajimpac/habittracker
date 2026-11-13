package com.marta.habittracker.data.remote.supabase

import com.marta.habittracker.data.remote.supabase.dto.ProfileDto

/**
 * Own-profile Postgrest + Storage operations. Implemented by [com.marta.habittracker.data.remote.supabase.datasources.ProfileRemoteDataSource]
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
