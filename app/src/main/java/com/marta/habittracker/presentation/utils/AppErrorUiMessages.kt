package com.marta.habittracker.presentation.utils

import androidx.annotation.StringRes
import com.marta.habittracker.R
import com.marta.habittracker.domain.model.AppError
import com.marta.habittracker.domain.model.LoginError
import com.marta.habittracker.domain.model.ProfileError
import com.marta.habittracker.domain.model.RecommendationError
import com.marta.habittracker.domain.model.RegisterError

@StringRes
fun AppError.toUserMessageRes(): Int = when (this) {
    LoginError.InvalidCredentials -> R.string.error_login_invalid_credentials
    LoginError.EmailNotVerified -> R.string.error_login_email_not_verified
    RegisterError.InvalidEmail -> R.string.error_register_invalid_email
    RegisterError.WeakPassword -> R.string.error_register_weak_password
    RegisterError.EmailAlreadyRegistered -> R.string.error_register_email_already_registered
    RegisterError.EmailConfirmationRequired -> R.string.error_register_email_confirmation_required
    RegisterError.InvalidUsername -> R.string.error_register_username_format
    ProfileError.InvalidDisplayName -> R.string.error_preferences_invalid_display_name
    RecommendationError.InvalidRequest -> R.string.recommendation_error_invalid
    RecommendationError.TimedOut -> R.string.recommendation_error_timeout
    RecommendationError.EmptyResponse -> R.string.recommendation_error_empty
    RecommendationError.Unavailable,
    RecommendationError.MissingBaseUrl,
    -> R.string.recommendation_error_unavailable
    AppError.Common.Network -> R.string.error_no_internet
    AppError.Common.Unauthorized -> R.string.error_common_unauthorized
    AppError.Common.Unknown -> R.string.error_common_unknown
}
