package com.marta.habittracker.data.remote

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * The model sometimes returns the useful reply as JSON:
 * `{ "recommendations": "<markdown>" }`, either as the HTTP body or as the
 * `message` string. The thread should keep that markdown, not the envelope.
 */
internal fun unwrapAssistantReply(
    raw: String,
    json: Json = Json { ignoreUnknownKeys = true },
): String {
    var current = raw.trim()
    repeat(2) {
        val extracted = extractEnvelope(current, json) ?: return current
        val trimmed = extracted.trim()
        if (trimmed.isEmpty() || trimmed == current) return trimmed
        current = trimmed
    }
    return current
}

private fun extractEnvelope(raw: String, json: Json): String? {
    val candidate = stripFence(raw.trim())
    if (!candidate.startsWith("{") || !candidate.endsWith("}")) return null
    return try {
        val envelope = json.decodeFromString<ReplyEnvelope>(candidate)
        when {
            envelope.recommendations != null -> envelope.recommendations
            envelope.message != null -> envelope.message
            else -> null
        }
    } catch (_: IllegalArgumentException) {
        recommendationsField.find(candidate)?.groupValues?.get(1)?.let(::unescapeJsonString)
    }
}

private fun stripFence(text: String): String {
    val match = fence.find(text) ?: return text
    return match.groupValues[1].trim()
}

private fun unescapeJsonString(value: String): String = buildString {
    var index = 0
    while (index < value.length) {
        val character = value[index]
        if (character == '\\' && index + 1 < value.length) {
            when (val next = value[index + 1]) {
                'n' -> append('\n')
                'r' -> append('\r')
                't' -> append('\t')
                '"' -> append('"')
                '\\' -> append('\\')
                else -> append(next)
            }
            index += 2
        } else {
            append(character)
            index++
        }
    }
}

private val fence = Regex("""^```(?:json|JSON)?\s*\n?([\s\S]*?)\n?```\s*$""")

private val recommendationsField = Regex(
    """"recommendations"\s*:\s*"((?:\\.|[^"\\])*)"""",
)

@Serializable
private data class ReplyEnvelope(
    val recommendations: String? = null,
    val message: String? = null,
)
