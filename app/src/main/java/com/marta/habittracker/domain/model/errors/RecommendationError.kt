package com.marta.habittracker.domain.model.errors

sealed interface RecommendationError : AppError {
    data object InvalidRequest : RecommendationError
    data object Unavailable : RecommendationError
    data object TimedOut : RecommendationError
    data object EmptyResponse : RecommendationError
    data object MissingBaseUrl : RecommendationError

    /** No usable sign-in session: the user has to sign in again to use Bloom. */
    data object SessionExpired : RecommendationError
}
