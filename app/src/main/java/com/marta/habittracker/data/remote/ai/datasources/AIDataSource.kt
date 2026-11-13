package com.marta.habittracker.data.remote.ai.datasources

import android.util.Log
import com.marta.habittracker.data.remote.ai.RecommendationTransport
import com.marta.habittracker.data.remote.ai.TransportResult
import com.marta.habittracker.data.remote.ai.dto.ChatMessageBody
import com.marta.habittracker.data.remote.ai.dto.ChatRequestBody
import com.marta.habittracker.data.remote.ai.dto.ChatResponseBody
import com.marta.habittracker.data.remote.ai.dto.RecommendationHabitBody
import com.marta.habittracker.data.remote.ai.helper.unwrapAssistantReply
import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.model.AssistantRole
import com.marta.habittracker.domain.model.AssistantTurn
import com.marta.habittracker.domain.model.errors.RecommendationError
import com.marta.habittracker.domain.usecase.HabitWeekStat
import kotlinx.serialization.json.Json

class BloomRecommendationDataSource(
    private val baseUrl: String,
    private val transport: RecommendationTransport,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {


    suspend fun send(
        stats: List<HabitWeekStat>,
        language: String,
        messages: List<AssistantTurn>,
    ): DataResult<String, RecommendationError> {
        val trimmed = baseUrl.trim()
        if (trimmed.isEmpty()) {
            Log.w(TAG, "Chat skipped: base URL is blank")
            return DataResult.Error(RecommendationError.MissingBaseUrl)
        }

        val url = trimmed.trimEnd('/') + CHAT_PATH
        val body = json.encodeToString(
            ChatRequestBody(
                habits = stats.map { stat ->
                    RecommendationHabitBody(
                        name = stat.name,
                        completedDays = stat.completedDays,
                        totalDays = stat.totalDays,
                    )
                },
                language = language,
                messages = messages.map { turn ->
                    ChatMessageBody(
                        role = turn.role.wire(),
                        content = turn.text,
                    )
                },
            ),
        )
        Log.d(TAG, "Chat request started")
        val result = interpretChat(transport.post(url, body))
        when (result) {
            is DataResult.Success -> Log.d(TAG, "Chat request finished ok")
            is DataResult.Error -> Log.e(TAG, "Chat request failed: ${result.error}")
        }
        return result
    }



    private fun interpretChat(response: TransportResult): DataResult<String, RecommendationError> {
        return when (response) {
            TransportResult.Timeout -> DataResult.Error(RecommendationError.TimedOut)
            TransportResult.Failure -> DataResult.Error(RecommendationError.Unavailable)
            is TransportResult.Http -> interpretChatHttp(response.code, response.body)
        }
    }

    private fun interpretChatHttp(code: Int, body: String): DataResult<String, RecommendationError> {
        return when (code) {
            400, 422 -> DataResult.Error(RecommendationError.InvalidRequest)
            504 -> DataResult.Error(RecommendationError.TimedOut)
            in 200..299 -> parseChat(body)
            else -> DataResult.Error(RecommendationError.Unavailable)
        }
    }

    private fun parseChat(body: String): DataResult<String, RecommendationError> {
        if (body.isBlank()) return DataResult.Error(RecommendationError.EmptyResponse)
        val text = try {
            val parsed = json.decodeFromString<ChatResponseBody>(body)
            parsed.message?.takeIf { it.isNotBlank() } ?: parsed.recommendations
        } catch (error: IllegalArgumentException) {
            Log.e(TAG, "Chat response was not valid JSON", error)
            return DataResult.Error(RecommendationError.Unavailable)
        }
        val reply = unwrapAssistantReply(text?.trim().orEmpty(), json).trim()
        return if (reply.isEmpty()) {
            DataResult.Error(RecommendationError.EmptyResponse)
        } else {
            DataResult.Success(reply)
        }
    }


    companion object {
        private const val TAG = "BloomRecommendations"
        private const val CHAT_PATH = "/api/chat"
    }
}

private fun AssistantRole.wire(): String = when (this) {
    AssistantRole.User -> "user"
    AssistantRole.Assistant -> "assistant"
}
