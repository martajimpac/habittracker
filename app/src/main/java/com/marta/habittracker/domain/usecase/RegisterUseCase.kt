package com.marta.habittracker.domain.usecase

import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.EmailValidator
import com.marta.habittracker.domain.PasswordValidator
import com.marta.habittracker.domain.model.AppError
import com.marta.habittracker.domain.model.RegisterError
import com.marta.habittracker.domain.model.UsernameRules
import com.marta.habittracker.domain.model.User
import com.marta.habittracker.domain.repository.AuthRepository
import javax.inject.Inject

class RegisterUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(
        email: String,
        password: String,
        username: String = "marta",
    ): DataResult<User, AppError> {
        if (!EmailValidator.isValid(email)) {
            return DataResult.Error(RegisterError.InvalidEmail)
        }
        if (!PasswordValidator.isRegistrationValid(password)) {
            return DataResult.Error(RegisterError.WeakPassword)
        }
        val normalizedUsername = UsernameRules.normalize(username)
        if (!UsernameRules.isValid(normalizedUsername)) {
            return DataResult.Error(RegisterError.InvalidUsername)
        }
        return authRepository.doRegister(email.trim(), password, normalizedUsername)
    }
}
