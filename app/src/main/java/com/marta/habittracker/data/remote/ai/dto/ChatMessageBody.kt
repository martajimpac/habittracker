package com.marta.habittracker.data.remote.ai.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * One conversation turn in a Bloom chat request; [role] is `user` or `assistant`. A Bloom turn
 * that showed a card carries its [action] and [actionStatus] (`pending`, `done` or `cancelled`).
 */
@Serializable
internal data class ChatMessageBody(
    val role: String,
    val content: String,
    val action: ChatActionBody? = null,
    @SerialName("action_status") val actionStatus: String? = null,
)
