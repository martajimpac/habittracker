package com.marta.habittracker.domain.repository

import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.model.RecommendationError
import com.marta.habittracker.domain.usecase.HabitWeekStat

interface RecommendationRepository {
    suspend fun request(stats: List<HabitWeekStat>): DataResult<String, RecommendationError>
}
