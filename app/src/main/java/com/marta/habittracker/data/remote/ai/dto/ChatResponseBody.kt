package com.marta.habittracker.data.remote.ai.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Response of `POST /api/chat`. The reply is read from [message], falling back to [recommendations].
 * [action] stays raw so a malformed proposal can be dropped without losing the text.
 * [actionRejected] is true when the API dropped a proposal it found invalid.
 */
@Serializable
internal data class ChatResponseBody(
    val message: String? = null,
    val recommendations: String? = null,
    val action: JsonElement? = null,
    @SerialName("action_rejected") val actionRejected: Boolean = false,
)
