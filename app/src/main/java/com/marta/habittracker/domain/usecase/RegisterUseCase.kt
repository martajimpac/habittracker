package com.marta.habittracker.domain.usecase

import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.EmailValidator
import com.marta.habittracker.domain.PasswordValidator
import com.marta.habittracker.domain.model.AppError
import com.marta.habittracker.domain.model.RegisterError
import com.marta.habittracker.domain.model.User
import com.marta.habittracker.domain.repository.AuthRepository
import javax.inject.Inject

class RegisterUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(
        email: String,
        password: String,
    ): DataResult<User, AppError> {
        if (!EmailValidator.isValid(email)) {
            return DataResult.Error(RegisterError.InvalidEmail)
        }
        if (!PasswordValidator.isValid(password)) {
            return DataResult.Error(RegisterError.WeakPassword)
        }
        return authRepository.doRegister(email.trim(), password)
    }
}
