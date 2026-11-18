package com.marta.habittracker.domain.model

enum class AssistantRole {
    User,
    Assistant,
}

/**
 * One chat turn. A Bloom turn may carry a proposed [action] shown as a card; [actionStatus]
 * says what the person did with it and is sent back so Bloom knows the outcome.
 */
data class AssistantTurn(
    val role: AssistantRole,
    val text: String,
    val action: AssistantAction? = null,
    val actionStatus: AssistantActionStatus? = null,
    /** Set when the app replaced a challenge card with its own notice ([actionStatus] = Discarded). */
    val discard: ChallengeDiscard? = null,
)
