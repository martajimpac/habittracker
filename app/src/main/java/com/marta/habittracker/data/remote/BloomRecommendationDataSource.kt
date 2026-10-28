package com.marta.habittracker.data.remote

import android.util.Log
import com.marta.habittracker.domain.DataResult
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
    }
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
