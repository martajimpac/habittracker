package com.marta.habittracker.domain.usecase

import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.model.AppError
import com.marta.habittracker.domain.model.Profile
import com.marta.habittracker.domain.repository.ProfileRepository

class FakeProfileRepository(
    private val myProfileResult: DataResult<Profile, AppError> = DataResult.Success(defaultProfile),
    private val updateDisplayNameResult: DataResult<Profile, AppError> = DataResult.Success(defaultProfile),
    private val uploadAvatarResult: DataResult<Profile, AppError> = DataResult.Error(AppError.Common.Unknown),
    private val clearAvatarResult: DataResult<Profile, AppError> = DataResult.Error(AppError.Common.Unknown),
    private val deleteAccountResult: DataResult<Unit, AppError> = DataResult.Error(AppError.Common.Unknown),
) : ProfileRepository {

    var getMyProfileCalls: Int = 0
        private set
    var updateDisplayNameCalls: Int = 0
        private set
    var uploadAvatarCalls: Int = 0
        private set
    var clearAvatarCalls: Int = 0
        private set
    var deleteAccountCalls: Int = 0
        private set
    var lastUpdatedDisplayName: String? = null
        private set
    var lastUploadedAvatarBytes: ByteArray? = null
        private set

    override suspend fun getMyProfile(): DataResult<Profile, AppError> {
        getMyProfileCalls++
        return myProfileResult
    }

    override suspend fun updateDisplayName(displayName: String): DataResult<Profile, AppError> {
        updateDisplayNameCalls++
        lastUpdatedDisplayName = displayName
        return updateDisplayNameResult
    }

    override suspend fun uploadAvatar(jpegBytes: ByteArray): DataResult<Profile, AppError> {
        uploadAvatarCalls++
        lastUploadedAvatarBytes = jpegBytes
        return uploadAvatarResult
    }

    override suspend fun clearAvatar(): DataResult<Profile, AppError> {
        clearAvatarCalls++
        return clearAvatarResult
    }

    override suspend fun deleteAccount(): DataResult<Unit, AppError> {
        deleteAccountCalls++
        return deleteAccountResult
    }

    companion object {
        val defaultProfile = Profile(
            id = "user-1",
            username = "marta",
            displayName = "Marta",
            avatarColor = "#6750A4",
        )
    }
}
