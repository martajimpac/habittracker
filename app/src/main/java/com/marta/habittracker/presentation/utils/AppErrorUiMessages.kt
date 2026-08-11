package com.marta.habittracker.presentation.utils

import androidx.annotation.StringRes
import com.marta.habittracker.R
import com.marta.habittracker.domain.model.AppError
import com.marta.habittracker.domain.model.LoginError
import com.marta.habittracker.domain.model.RegisterError

@StringRes
fun AppError.toUserMessageRes(): Int = when (this) {
    LoginError.InvalidCredentials -> R.string.error_login_invalid_credentials
    LoginError.EmailNotVerified -> R.string.error_login_email_not_verified
    RegisterError.InvalidEmail -> R.string.error_register_invalid_email
    RegisterError.WeakPassword -> R.string.error_register_weak_password
    RegisterError.EmailAlreadyRegistered -> R.string.error_register_email_already_registered
    RegisterError.EmailConfirmationRequired -> R.string.error_register_email_confirmation_required
    AppError.Common.Network -> R.string.error_no_internet
    AppError.Common.Unauthorized -> R.string.error_common_unauthorized
    AppError.Common.Unknown -> R.string.error_common_unknown
}
