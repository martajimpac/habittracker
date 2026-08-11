package com.marta.habittracker.domain.repository

interface OnboardingRepository {
    suspend fun isCompleted(): Boolean
    suspend fun setCompleted()
}
