package com.marta.habittracker.data.remote.ai.datasources

import com.marta.habittracker.data.remote.ai.RecommendationTransport
import com.marta.habittracker.data.remote.ai.TransportResult
import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.model.AssistantRole
import com.marta.habittracker.domain.model.AssistantTurn
import com.marta.habittracker.domain.model.errors.RecommendationError
import com.marta.habittracker.domain.usecase.HabitWeekStat
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BloomRecommendationDataSourceTest {

    private val stats = listOf(HabitWeekStat(name = "Read", completedDays = 2, totalDays = 3))


    @Test
    fun `posts the chat thread to bloom api and does not call ollama`() = runTest {
        val transport = FakeRecommendationTransport(
            TransportResult.Http(200, """{"message":"Walk after lunch."}"""),
        )
        val source = BloomRecommendationDataSource(
            baseUrl = "http://10.0.2.2:8000/",
            transport = transport,
        )

        val result = source.send(
            stats = stats,
            language = "en",
            messages = listOf(
                AssistantTurn(role = AssistantRole.User, text = "How is my week?"),
            ),
        )

        assertEquals(DataResult.Success("Walk after lunch."), result)
        assertEquals("http://10.0.2.2:8000/api/chat", transport.url)
        assertFalse(transport.url.orEmpty().contains("11434"))
        assertTrue(transport.body.orEmpty().contains("\"language\":\"en\""))
        assertTrue(transport.body.orEmpty().contains("\"role\":\"user\""))
        assertTrue(transport.body.orEmpty().contains("How is my week?"))
        assertTrue(transport.body.orEmpty().contains("\"completed_days\":2"))
    }

    @Test
    fun `unwraps a recommendations json message into markdown`() = runTest {
        val markdown = "**Observaciones:**\n* El hábito aún no tiene registros.\n\n---\n\n1. **Empezar:** da el primer paso."
        val envelope = buildJsonObject { put("recommendations", markdown) }.toString()
        val payload = buildJsonObject { put("message", envelope) }.toString()
        val source = BloomRecommendationDataSource(
            baseUrl = "http://10.0.2.2:8000",
            transport = FakeRecommendationTransport(TransportResult.Http(200, payload)),
        )

        val result = source.send(
            stats = stats,
            language = "es",
            messages = listOf(AssistantTurn(role = AssistantRole.User, text = "¿Cómo voy?")),
        )

        assertEquals(DataResult.Success(markdown), result)
    }

    @Test
    fun `reads recommendations when the chat body has no message`() = runTest {
        val source = BloomRecommendationDataSource(
            baseUrl = "http://10.0.2.2:8000",
            transport = FakeRecommendationTransport(
                TransportResult.Http(200, """{"recommendations":"**Hola**\n* uno"}"""),
            ),
        )

        val result = source.send(
            stats = stats,
            language = "es",
            messages = listOf(AssistantTurn(role = AssistantRole.User, text = "Hola")),
        )

        assertEquals(DataResult.Success("**Hola**\n* uno"), result)
    }

    @Test
    fun `maps an empty chat body to an empty response`() = runTest {
        val source = BloomRecommendationDataSource(
            baseUrl = "http://10.0.2.2:8000",
            transport = FakeRecommendationTransport(TransportResult.Http(200, """{"message":"  "}""")),
        )

        val result = source.send(
            stats = stats,
            language = "es",
            messages = listOf(AssistantTurn(role = AssistantRole.User, text = "Hola")),
        )

        assertEquals(DataResult.Error(RecommendationError.EmptyResponse), result)
    }


    @Test
    fun `does not call the network when the base url is blank`() = runTest {
        val transport = FakeRecommendationTransport(TransportResult.Failure)
        val source = BloomRecommendationDataSource(baseUrl = "  ", transport = transport)

        val result = source.send(
            stats = stats,
            language = "es",
            messages = listOf(AssistantTurn(role = AssistantRole.User, text = "Hola")),
        )

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
