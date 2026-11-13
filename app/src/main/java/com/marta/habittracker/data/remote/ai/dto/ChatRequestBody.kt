package com.marta.habittracker.data.remote.ai.dto

import kotlinx.serialization.Serializable

/** Body of `POST /api/chat`. */
@Serializable
internal data class ChatRequestBody(
    val habits: List<RecommendationHabitBody>,
    val language: String,
    val messages: List<ChatMessageBody>,
)
