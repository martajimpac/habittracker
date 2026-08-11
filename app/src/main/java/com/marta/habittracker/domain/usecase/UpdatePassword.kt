package com.marta.habittracker.domain.usecase

import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.PasswordValidator
import com.marta.habittracker.domain.model.AppError
import com.marta.habittracker.domain.model.RegisterError
import com.marta.habittracker.domain.repository.AuthRepository
import javax.inject.Inject

class UpdatePassword @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(newPassword: String): DataResult<Unit, AppError> {
        if (!PasswordValidator.isValid(newPassword)) {
            return DataResult.Error(RegisterError.WeakPassword)
        }
        return when (val updateResult = authRepository.updatePassword(newPassword)) {
            is DataResult.Error -> updateResult
            is DataResult.Success -> authRepository.signOut()
        }
    }
}
