package com.marta.habittracker.domain.model

sealed interface RecommendationError : AppError {
    data object InvalidRequest : RecommendationError
    data object Unavailable : RecommendationError
    data object TimedOut : RecommendationError
    data object EmptyResponse : RecommendationError
    data object MissingBaseUrl : RecommendationError
}
