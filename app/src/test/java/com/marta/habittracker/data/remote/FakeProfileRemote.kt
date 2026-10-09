package com.marta.habittracker.data.remote

import com.marta.habittracker.data.remote.dto.ProfileDto

class FakeProfileRemote(
    initial: ProfileDto = defaultDto,
    private val deleteAccountException: Exception? = null,
) : ProfileRemote {

    var current: ProfileDto = initial
        private set
    var fetchProfileByIdCalls: Int = 0
        private set
    var updateMyProfileCalls: Int = 0
        private set
    var uploadAvatarCalls: Int = 0
        private set
    var clearAvatarCalls: Int = 0
        private set
    var deleteAccountCalls: Int = 0
        private set
    var lastUploadedBytes: ByteArray? = null
        private set
    var lastUpdateMyProfileAvatarUrl: String? = UNSET_AVATAR
        private set

    override suspend fun fetchProfileById(id: String): ProfileDto? {
        fetchProfileByIdCalls++
        return current
    }

    override suspend fun updateMyProfile(displayName: String?, avatarUrl: String?): ProfileDto {
        updateMyProfileCalls++
        lastUpdateMyProfileAvatarUrl = avatarUrl
        current = current.copy(
            displayName = displayName ?: current.displayName,
            avatarUrl = avatarUrl ?: current.avatarUrl,
        )
        return current
    }

    override suspend fun uploadAvatar(jpegBytes: ByteArray): ProfileDto {
        uploadAvatarCalls++
        lastUploadedBytes = jpegBytes
        current = current.copy(
            avatarUrl = cacheBustAvatarUrl(PUBLIC_URL, System.currentTimeMillis()),
        )
        return current
    }

    override suspend fun clearAvatar(): ProfileDto {
        clearAvatarCalls++
        current = current.copy(avatarUrl = null)
        return current
    }

    override suspend fun deleteAccount() {
        deleteAccountCalls++
        deleteAccountException?.let { throw it }
    }

    companion object {
        const val PUBLIC_URL =
            "https://example.supabase.co/storage/v1/object/public/avatars/user-1/avatar.jpg"
        const val UNSET_AVATAR = "__unset__"

        val defaultDto = ProfileDto(
            id = "user-1",
            username = "marta",
            displayName = "Marta",
            avatarColor = "#6750A4",
            avatarUrl = null,
        )
    }
}
