package com.marta.habittracker.domain.usecase

import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.model.AppError
import com.marta.habittracker.domain.model.Profile
import com.marta.habittracker.domain.model.ProfileError
import com.marta.habittracker.domain.repository.ProfileRepository
import javax.inject.Inject

class UpdateDisplayName @Inject constructor(
    private val profileRepository: ProfileRepository,
) {
    suspend operator fun invoke(displayName: String): DataResult<Profile, AppError> {
        val trimmed = displayName.trim()
        if (trimmed.isEmpty() || trimmed.length > MAX_LENGTH) {
            return DataResult.Error(ProfileError.InvalidDisplayName)
        }
        return profileRepository.updateDisplayName(trimmed)
    }

    private companion object {
        const val MAX_LENGTH = 40
    }
}
