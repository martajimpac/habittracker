package com.marta.habittracker.domain.model

enum class AssistantRole {
    User,
    Assistant,
}

data class AssistantTurn(
    val role: AssistantRole,
    val text: String,
)
