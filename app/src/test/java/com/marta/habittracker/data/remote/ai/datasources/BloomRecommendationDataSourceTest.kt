package com.marta.habittracker.data.remote.ai.datasources

import com.marta.habittracker.data.remote.ai.BloomAccessTokens
import com.marta.habittracker.data.remote.ai.RecommendationTransport
import com.marta.habittracker.data.remote.ai.TokenRefresh
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
        val source = dataSource(
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
        val source = dataSource(
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
        val source = dataSource(
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
        val source = dataSource(
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
        val source = dataSource(baseUrl = "  ", transport = transport)

        val result = source.send(
            stats = stats,
            language = "es",
            messages = listOf(AssistantTurn(role = AssistantRole.User, text = "Hola")),
        )

        assertEquals(DataResult.Error(RecommendationError.MissingBaseUrl), result)
        assertEquals(null, transport.url)
    }

    // CA-03
    @Test
    fun `sends the session token with the chat request`() = runTest {
        val transport = FakeRecommendationTransport(TransportResult.Http(200, OK_BODY))
        val source = dataSource(BASE_URL, transport, FakeAccessTokens(current = "session-token"))

        source.send(stats, "es", hello)

        assertEquals(listOf("session-token"), transport.tokens)
    }

    // CA-04
    @Test
    fun `refreshes the session and retries once after a 401`() = runTest {
        val transport = FakeRecommendationTransport(
            TransportResult.Http(401, UNAUTHORIZED_BODY),
            TransportResult.Http(200, OK_BODY),
        )
        val tokens = FakeAccessTokens(refresh = TokenRefresh.Refreshed("fresh-token"))
        val source = dataSource(BASE_URL, transport, tokens)

        val result = source.send(stats, "es", hello)

        assertEquals(DataResult.Success("Hola"), result)
        assertEquals(listOf("session-token", "fresh-token"), transport.tokens)
        assertEquals(1, tokens.refreshCalls)
    }

    // CA-05
    @Test
    fun `a second 401 ends with session expired and no more retries`() = runTest {
        val transport = FakeRecommendationTransport(TransportResult.Http(401, UNAUTHORIZED_BODY))
        val tokens = FakeAccessTokens()
        val source = dataSource(BASE_URL, transport, tokens)

        val result = source.send(stats, "es", hello)

        assertEquals(DataResult.Error(RecommendationError.SessionExpired), result)
        assertEquals(2, transport.tokens.size)
        assertEquals(1, tokens.refreshCalls)
    }

    @Test
    fun `a rejected refresh ends with session expired without retrying`() = runTest {
        val transport = FakeRecommendationTransport(TransportResult.Http(401, UNAUTHORIZED_BODY))
        val source = dataSource(BASE_URL, transport, FakeAccessTokens(refresh = TokenRefresh.SignedOut))

        val result = source.send(stats, "es", hello)

        assertEquals(DataResult.Error(RecommendationError.SessionExpired), result)
        assertEquals(1, transport.tokens.size)
    }

    @Test
    fun `a refresh that cannot reach supabase reports unavailable, not session expired`() = runTest {
        val transport = FakeRecommendationTransport(TransportResult.Http(401, UNAUTHORIZED_BODY))
        val source = dataSource(BASE_URL, transport, FakeAccessTokens(refresh = TokenRefresh.Failed))

        val result = source.send(stats, "es", hello)

        assertEquals(DataResult.Error(RecommendationError.Unavailable), result)
        assertEquals(1, transport.tokens.size)
    }

    // CA-06
    @Test
    fun `without a session nothing is sent and the session error is returned`() = runTest {
        val transport = FakeRecommendationTransport(TransportResult.Http(200, OK_BODY))
        val tokens = FakeAccessTokens(current = null)
        val source = dataSource(BASE_URL, transport, tokens)

        val result = source.send(stats, "es", hello)

        assertEquals(DataResult.Error(RecommendationError.SessionExpired), result)
        assertEquals(null, transport.url)
        assertTrue(transport.tokens.isEmpty())
        assertEquals(0, tokens.refreshCalls)
    }

    // CA-09: the token travels in the header, never in the request body.
    @Test
    fun `the token is not part of the request body`() = runTest {
        val transport = FakeRecommendationTransport(TransportResult.Http(200, OK_BODY))
        val source = dataSource(BASE_URL, transport, FakeAccessTokens(current = "secret-session-token"))

        source.send(stats, "es", hello)

        assertFalse(transport.body.orEmpty().contains("secret-session-token"))
    }

    private val hello = listOf(AssistantTurn(role = AssistantRole.User, text = "Hola"))

    private companion object {
        const val BASE_URL = "http://10.0.2.2:8000"
        const val OK_BODY = """{"message":"Hola"}"""
        const val UNAUTHORIZED_BODY = """{"detail":"Sesión no válida."}"""
    }
}

private fun dataSource(
    baseUrl: String,
    transport: RecommendationTransport,
    accessTokens: BloomAccessTokens = FakeAccessTokens(),
) = BloomRecommendationDataSource(
    baseUrl = baseUrl,
    transport = transport,
    accessTokens = accessTokens,
)

/** Answers each request with the next result; the last one repeats. */
private class FakeRecommendationTransport(
    private vararg val results: TransportResult,
) : RecommendationTransport {
    var url: String? = null
    var body: String? = null
    val tokens = mutableListOf<String>()

    override suspend fun post(url: String, jsonBody: String, bearerToken: String): TransportResult {
        this.url = url
        this.body = jsonBody
        tokens += bearerToken
        return results[minOf(tokens.size, results.size) - 1]
    }
}

private class FakeAccessTokens(
    private val current: String? = "session-token",
    private val refresh: TokenRefresh = TokenRefresh.Refreshed("fresh-token"),
) : BloomAccessTokens {
    var refreshCalls = 0

    override suspend fun current(): String? = current

    override suspend fun refresh(): TokenRefresh {
        refreshCalls++
        return refresh
    }
}
