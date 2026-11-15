package com.marta.habittracker.domain.repository

import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.model.AssistantTurn
import com.marta.habittracker.domain.model.ChatReply
import com.marta.habittracker.domain.model.errors.RecommendationError
import com.marta.habittracker.domain.usecase.ChatContext

interface RecommendationRepository {

    suspend fun send(
        context: ChatContext,
        language: String,
        messages: List<AssistantTurn>,
    ): DataResult<ChatReply, RecommendationError>
}
