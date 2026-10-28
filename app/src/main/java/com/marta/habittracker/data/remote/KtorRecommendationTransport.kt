package com.marta.habittracker.data.remote

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlin.coroutines.cancellation.CancellationException

class KtorRecommendationTransport : RecommendationTransport {

    private val client = HttpClient(Android) {
        install(HttpTimeout) {
            requestTimeoutMillis = REQUEST_TIMEOUT_MS
            socketTimeoutMillis = REQUEST_TIMEOUT_MS
        }
    }

    override suspend fun post(url: String, jsonBody: String): TransportResult {
        return try {
            val response = client.post(url) {
                contentType(ContentType.Application.Json)
                setBody(jsonBody)
            }
            TransportResult.Http(code = response.status.value, body = response.bodyAsText())
        } catch (error: CancellationException) {
            throw error
        } catch (error: HttpRequestTimeoutException) {
            Log.e(TAG, "Recommendation transport timed out", error)
            TransportResult.Timeout
        } catch (error: Exception) {
            Log.e(TAG, "Recommendation transport failed", error)
            TransportResult.Failure
        }
    }

    private companion object {
        const val TAG = "BloomRecommendations"
        const val REQUEST_TIMEOUT_MS = 100_000L
    }
}
