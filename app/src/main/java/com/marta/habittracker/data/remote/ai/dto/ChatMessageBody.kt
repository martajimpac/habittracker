package com.marta.habittracker.data.remote.ai.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * One conversation turn in a Bloom chat request; [role] is `user` or `assistant`. A Bloom turn
 * that showed a card carries its [action] and [actionStatus] (`pending`, `done`, `cancelled` or
 * `discarded`). A discarded challenge also carries [discardReason]. A proposal the app rejected as
 * invalid is sent without [action], as `discarded` with reason `invalid_proposal`.
 */
@Serializable
internal data class ChatMessageBody(
    val role: String,
    val content: String,
    val action: ChatActionBody? = null,
    @SerialName("action_status") val actionStatus: String? = null,
    @SerialName("discard_reason") val discardReason: String? = null,
)
