package com.marta.habittracker.data.repository

import com.marta.habittracker.data.remote.ai.datasources.BloomRecommendationDataSource
import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.model.AssistantTurn
import com.marta.habittracker.domain.model.ChatReply
import com.marta.habittracker.domain.model.errors.RecommendationError
import com.marta.habittracker.domain.repository.RecommendationRepository
import com.marta.habittracker.domain.usecase.ChatContext
import javax.inject.Inject

class RecommendationRepositoryImpl @Inject constructor(
    private val dataSource: BloomRecommendationDataSource,
) : RecommendationRepository {

    override suspend fun send(
        context: ChatContext,
        language: String,
        messages: List<AssistantTurn>,
    ): DataResult<ChatReply, RecommendationError> {
        return dataSource.send(context, language, messages)
    }
}
