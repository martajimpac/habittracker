package com.marta.habittracker.data.remote

import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.model.RecommendationError
import com.marta.habittracker.domain.usecase.HabitWeekStat
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BloomRecommendationDataSourceTest {

    private val stats = listOf(HabitWeekStat(name = "Read", completedDays = 2, totalDays = 3))

    @Test
    fun `posts recommendations and does not call ollama chat`() = runTest {
        val transport = FakeRecommendationTransport(
            TransportResult.Http(200, """{"recommendations":"Camina después de comer."}"""),
        )
        val source = BloomRecommendationDataSource(
            baseUrl = "http://10.0.2.2:8000/",
            transport = transport,
        )

        val result = source.request(stats)

        assertEquals(
            DataResult.Success("Camina después de comer."),
            result,
        )
        assertEquals("http://10.0.2.2:8000/api/recommendations", transport.url)
        assertFalse(transport.url.orEmpty().contains("/api/chat"))
        assertTrue(transport.body.orEmpty().contains("\"completed_days\":2"))
        assertTrue(transport.body.orEmpty().contains("\"total_days\":3"))
    }

    @Test
    fun `maps failed responses to errors without a suggestion`() = runTest {
        val cases = listOf(
            TransportResult.Http(400, """{"detail":"no"}""") to RecommendationError.InvalidRequest,
            TransportResult.Http(502, """{"detail":"down"}""") to RecommendationError.Unavailable,
            TransportResult.Http(504, """{"detail":"slow"}""") to RecommendationError.TimedOut,
            TransportResult.Http(200, """{"recommendations":"   "}""") to RecommendationError.EmptyResponse,
            TransportResult.Http(200, "") to RecommendationError.EmptyResponse,
            TransportResult.Timeout to RecommendationError.TimedOut,
            TransportResult.Failure to RecommendationError.Unavailable,
        )

        cases.forEach { (response, expected) ->
            val source = BloomRecommendationDataSource(
                baseUrl = "http://10.0.2.2:8000",
                transport = FakeRecommendationTransport(response),
            )

            assertEquals(DataResult.Error(expected), source.request(stats))
        }
    }

    @Test
    fun `does not call the network when the base url is blank`() = runTest {
        val transport = FakeRecommendationTransport(TransportResult.Failure)
        val source = BloomRecommendationDataSource(baseUrl = "  ", transport = transport)

        val result = source.request(stats)

        assertEquals(DataResult.Error(RecommendationError.MissingBaseUrl), result)
        assertEquals(null, transport.url)
    }
}

private class FakeRecommendationTransport(
    private val result: TransportResult,
) : RecommendationTransport {
    var url: String? = null
    var body: String? = null

    override suspend fun post(url: String, jsonBody: String): TransportResult {
        this.url = url
        this.body = jsonBody
        return result
    }
}
