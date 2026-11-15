package com.marta.habittracker.data.remote.ai.dto

import kotlinx.serialization.Serializable

/** Body of `POST /api/chat`. [today] is an ISO date; [habits] may be empty. */
@Serializable
internal data class ChatRequestBody(
    val habits: List<RecommendationHabitBody>,
    val language: String,
    val messages: List<ChatMessageBody>,
    val today: String? = null,
    val catalog: HabitCatalogBody? = null,
)
