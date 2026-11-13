package com.marta.habittracker.data.remote.ai.dto

import kotlinx.serialization.Serializable

/** One conversation turn in a Bloom chat request; [role] is `user` or `assistant`. */
@Serializable
internal data class ChatMessageBody(
    val role: String,
    val content: String,
)
