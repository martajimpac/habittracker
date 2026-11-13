package com.marta.habittracker.data.remote.ai.dto

import kotlinx.serialization.Serializable

/** Response of `POST /api/chat`. The reply is read from [message], falling back to [recommendations]. */
@Serializable
internal data class ChatResponseBody(
    val message: String? = null,
    val recommendations: String? = null,
)
