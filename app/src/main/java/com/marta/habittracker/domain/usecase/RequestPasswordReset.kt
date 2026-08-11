package com.marta.habittracker.domain.usecase

import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.EmailValidator
import com.marta.habittracker.domain.model.AppError
import com.marta.habittracker.domain.model.RegisterError
import com.marta.habittracker.domain.repository.AuthRepository
import javax.inject.Inject

class RequestPasswordReset @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(email: String): DataResult<Unit, AppError> {
        if (!EmailValidator.isValid(email)) {
            return DataResult.Error(RegisterError.InvalidEmail)
        }
        return authRepository.requestPasswordReset(email.trim())
    }
}
