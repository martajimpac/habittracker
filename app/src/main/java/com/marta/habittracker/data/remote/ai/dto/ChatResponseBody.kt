package com.marta.habittracker.data.remote.ai.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Response of `POST /api/chat`. The reply is read from [message], falling back to [recommendations].
 * [action] stays raw so a malformed proposal can be dropped without losing the text.
 */
@Serializable
internal data class ChatResponseBody(
    val message: String? = null,
    val recommendations: String? = null,
    val action: JsonElement? = null,
)
