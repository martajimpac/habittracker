package com.marta.habittracker.domain.repository

import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.model.AssistantTurn
import com.marta.habittracker.domain.model.RecommendationError
import com.marta.habittracker.domain.usecase.HabitWeekStat

interface RecommendationRepository {
    suspend fun request(stats: List<HabitWeekStat>): DataResult<String, RecommendationError>

    suspend fun send(
        stats: List<HabitWeekStat>,
        language: String,
        messages: List<AssistantTurn>,
    ): DataResult<String, RecommendationError>
}
