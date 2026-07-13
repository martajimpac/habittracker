package com.marta.habittracker.domain.usecase

import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.EmailValidator
import com.marta.habittracker.domain.models.AppError
import com.marta.habittracker.domain.models.LoginError
import com.marta.habittracker.domain.models.User
import com.marta.habittracker.domain.repository.AuthRepository
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {

    suspend operator fun invoke(
        email: String,
        password: String
    ): DataResult<User, AppError> {

        if (!EmailValidator.isValid(email)) {
            return DataResult.Error(LoginError.InvalidCredentials)
        }

        return authRepository.doLogin(email, password)
    }
}