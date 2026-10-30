package com.marta.habittracker.data.repository

import com.marta.habittracker.data.remote.BloomRecommendationDataSource
import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.model.AssistantTurn
import com.marta.habittracker.domain.model.RecommendationError
import com.marta.habittracker.domain.repository.RecommendationRepository
import com.marta.habittracker.domain.usecase.HabitWeekStat
import javax.inject.Inject

class RecommendationRepositoryImpl @Inject constructor(
    private val dataSource: BloomRecommendationDataSource,
) : RecommendationRepository {
    override suspend fun request(stats: List<HabitWeekStat>): DataResult<String, RecommendationError> {
        return dataSource.request(stats)
    }

    override suspend fun send(
        stats: List<HabitWeekStat>,
        language: String,
        messages: List<AssistantTurn>,
    ): DataResult<String, RecommendationError> {
        return dataSource.send(stats, language, messages)
    }
}
