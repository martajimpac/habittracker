package com.marta.habittracker.data.remote

import android.util.Log
import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.model.AssistantRole
import com.marta.habittracker.domain.model.AssistantTurn
import com.marta.habittracker.domain.model.RecommendationError
import com.marta.habittracker.domain.usecase.HabitWeekStat
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class BloomRecommendationDataSource(
    private val baseUrl: String,
    private val transport: RecommendationTransport,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {

    suspend fun request(stats: List<HabitWeekStat>): DataResult<String, RecommendationError> {
        val trimmed = baseUrl.trim()
        if (trimmed.isEmpty()) {
            Log.w(TAG, "Recommendation skipped: base URL is blank")
            return DataResult.Error(RecommendationError.MissingBaseUrl)
        }

        val url = trimmed.trimEnd('/') + RECOMMENDATIONS_PATH
        val body = json.encodeToString(
            RecommendationRequestBody(
                habits = stats.map { stat ->
                    RecommendationHabitBody(
                        name = stat.name,
                        completedDays = stat.completedDays,
                        totalDays = stat.totalDays,
                    )
                },
            ),
        )
        Log.d(TAG, "Recommendation request started")
        val result = interpret(transport.post(url, body))
        when (result) {
            is DataResult.Success -> Log.d(TAG, "Recommendation request finished ok")
            is DataResult.Error -> Log.e(TAG, "Recommendation request failed: ${result.error}")
        }
        return result
    }

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

    private fun interpret(response: TransportResult): DataResult<String, RecommendationError> {
        return when (response) {
            TransportResult.Timeout -> DataResult.Error(RecommendationError.TimedOut)
            TransportResult.Failure -> DataResult.Error(RecommendationError.Unavailable)
            is TransportResult.Http -> interpretHttp(response.code, response.body)
        }
    }

    private fun interpretHttp(code: Int, body: String): DataResult<String, RecommendationError> {
        return when (code) {
            400, 422 -> DataResult.Error(RecommendationError.InvalidRequest)
            504 -> DataResult.Error(RecommendationError.TimedOut)
            in 200..299 -> parseSuccess(body)
            else -> DataResult.Error(RecommendationError.Unavailable)
        }
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
            json.decodeFromString<ChatResponseBody>(body).message
        } catch (error: IllegalArgumentException) {
            Log.e(TAG, "Chat response was not valid JSON", error)
            return DataResult.Error(RecommendationError.Unavailable)
        }
        val reply = text?.trim().orEmpty()
        return if (reply.isEmpty()) {
            DataResult.Error(RecommendationError.EmptyResponse)
        } else {
            DataResult.Success(reply)
        }
    }

    private fun parseSuccess(body: String): DataResult<String, RecommendationError> {
        if (body.isBlank()) return DataResult.Error(RecommendationError.EmptyResponse)
        val text = try {
            json.decodeFromString<RecommendationResponseBody>(body).recommendations
        } catch (error: IllegalArgumentException) {
            Log.e(TAG, "Recommendation response was not valid JSON", error)
            return DataResult.Error(RecommendationError.Unavailable)
        }
        val suggestion = text?.trim().orEmpty()
        return if (suggestion.isEmpty()) {
            DataResult.Error(RecommendationError.EmptyResponse)
        } else {
            DataResult.Success(suggestion)
        }
    }

    companion object {
        private const val TAG = "BloomRecommendations"
        private const val RECOMMENDATIONS_PATH = "/api/recommendations"
        private const val CHAT_PATH = "/api/chat"
    }
}

private fun AssistantRole.wire(): String = when (this) {
    AssistantRole.User -> "user"
    AssistantRole.Assistant -> "assistant"
}

@Serializable
private data class RecommendationRequestBody(
    val habits: List<RecommendationHabitBody>,
)

@Serializable
private data class RecommendationHabitBody(
    val name: String,
    @SerialName("completed_days") val completedDays: Int,
    @SerialName("total_days") val totalDays: Int,
)

@Serializable
private data class RecommendationResponseBody(
    val recommendations: String? = null,
)

@Serializable
private data class ChatRequestBody(
    val habits: List<RecommendationHabitBody>,
    val language: String,
    val messages: List<ChatMessageBody>,
)

@Serializable
private data class ChatMessageBody(
    val role: String,
    val content: String,
)

@Serializable
private data class ChatResponseBody(
    val message: String? = null,
)
