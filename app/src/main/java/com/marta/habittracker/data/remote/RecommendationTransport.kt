package com.marta.habittracker.data.remote

interface RecommendationTransport {
    suspend fun post(url: String, jsonBody: String): TransportResult
}

sealed interface TransportResult {
    data class Http(val code: Int, val body: String) : TransportResult
    data object Timeout : TransportResult
    data object Failure : TransportResult
}
