package com.marta.habittracker.data.remote.ai.datasources

import com.marta.habittracker.data.remote.ai.BloomAccessTokens
import com.marta.habittracker.data.remote.ai.RecommendationTransport
import com.marta.habittracker.data.remote.ai.TokenRefresh
import com.marta.habittracker.data.remote.ai.TransportResult
import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.model.AssistantRole
import com.marta.habittracker.domain.model.AssistantAction
import com.marta.habittracker.domain.model.AssistantActionStatus
import com.marta.habittracker.domain.model.AssistantTurn
import com.marta.habittracker.domain.model.ChatReply
import com.marta.habittracker.domain.model.HabitCatalog
import com.marta.habittracker.domain.model.HabitSchedule
import com.marta.habittracker.domain.model.ProposedAction
import com.marta.habittracker.domain.usecase.ChatContext
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
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

    private val context = ChatContext(
        habits = listOf(HabitWeekStat(name = "Read", completedDays = 2, totalDays = 3)),
        today = LocalDate(2026, 11, 12),
        catalog = HabitCatalog(icons = listOf("menu_book"), colorHexes = listOf("#294536")),
    )


    // ---- Retos (SPEC chat-challenges) ----

    private val laura = com.marta.habittracker.domain.model.Profile(
        id = "laura-secret-id",
        username = "laura_secret_user",
        displayName = "Laura Secret Surname",
        avatarColor = "#FF7775",
        avatarUrl = "https://example.com/laura.png",
    )

    private val challengeProposal = com.marta.habittracker.domain.model.ChallengeProposal(
        friendQuery = "Laura",
        draft = com.marta.habittracker.domain.model.ChallengeHabitDraft(
            name = "Meditar",
            icon = "menu_book",
            colorHex = "#294536",
            daysOfWeek = setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY),
            scheduleMode = "weekdays",
        ),
        criteria = com.marta.habittracker.domain.model.ChallengeCriteria.Streak,
        durationDays = 14,
    )

    @Test
    fun `parses a create challenge action`() = runTest {
        val payload = """{"message":"Reto:","action":{"type":"create_challenge","friend_name":"Laura","name":"Meditar",""" +
            """"icon":"menu_book","color":"#294536","schedule":{"mode":"daily"},"criteria":"streak","duration_days":14}}"""
        val source = dataSource(
            baseUrl = "http://10.0.2.2:8000",
            transport = FakeRecommendationTransport(TransportResult.Http(200, payload)),
        )

        val result = source.send(context, "es", listOf(AssistantTurn(role = AssistantRole.User, text = "reta a Laura")))

        val proposal = (result as DataResult.Success).data.proposal as ProposedAction.CreateChallenge
        assertEquals("Laura", proposal.friendName)
        assertEquals("streak", proposal.criteria)
        assertEquals(14, proposal.durationDays)
        assertEquals("daily", proposal.scheduleMode)
    }

    // Privacy: a confirmed challenge card goes back to Bloom with the typed name only.
    @Test
    fun `challenge history never contains the resolved friend data`() = runTest {
        val transport = FakeRecommendationTransport(TransportResult.Http(200, """{"message":"ok"}"""))
        val source = dataSource(baseUrl = "http://10.0.2.2:8000", transport = transport)
        val card = AssistantTurn(
            role = AssistantRole.Assistant,
            text = "Reto:",
            action = AssistantAction.CreateChallenge("challenge-id", "habit-id", laura, challengeProposal),
            actionStatus = AssistantActionStatus.Done,
        )

        source.send(context, "es", listOf(AssistantTurn(AssistantRole.User, "reta a Laura"), card, AssistantTurn(AssistantRole.User, "gracias")))

        val body = transport.body.orEmpty()
        assertTrue(body.contains("\"type\":\"create_challenge\""))
        assertTrue(body.contains("\"friend_name\":\"Laura\""))
        assertTrue(body.contains("\"action_status\":\"done\""))
        assertTrue(body.contains("\"criteria\":\"streak\""))
        assertTrue(body.contains("\"duration_days\":14"))
        for (secret in listOf("laura-secret-id", "laura_secret_user", "Secret Surname", "example.com", "challenge-id", "habit-id")) {
            assertFalse("Body leaks $secret", body.contains(secret))
        }
    }

    // RF-12: a discarded challenge carries its reason; the notice (with friend names) is not sent.
    @Test
    fun `discarded challenge is sent with its reason and without candidates`() = runTest {
        val transport = FakeRecommendationTransport(TransportResult.Http(200, """{"message":"ok"}"""))
        val source = dataSource(baseUrl = "http://10.0.2.2:8000", transport = transport)
        val discarded = AssistantTurn(
            role = AssistantRole.Assistant,
            text = "Reto:",
            actionStatus = AssistantActionStatus.Discarded,
            discard = com.marta.habittracker.domain.model.ChallengeDiscard(
                proposal = challengeProposal,
                reason = com.marta.habittracker.domain.model.ChallengeDiscardReason.FriendAmbiguous,
                candidates = listOf("Laura Secret Surname", "Laura Other"),
            ),
        )

        source.send(context, "es", listOf(AssistantTurn(AssistantRole.User, "reta a Laura"), discarded, AssistantTurn(AssistantRole.User, "la primera")))

        val body = transport.body.orEmpty()
        assertTrue(body.contains("\"action_status\":\"discarded\""))
        assertTrue(body.contains("\"discard_reason\":\"friend_ambiguous\""))
        assertTrue(body.contains("\"content\":\"Reto:\""))
        assertFalse(body.contains("Secret Surname"))
        assertFalse(body.contains("Laura Other"))
    }

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
            context = context,
            language = "en",
            messages = listOf(
                AssistantTurn(role = AssistantRole.User, text = "How is my week?"),
            ),
        )

        assertEquals(DataResult.Success(ChatReply("Walk after lunch.")), result)
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
            context = context,
            language = "es",
            messages = listOf(AssistantTurn(role = AssistantRole.User, text = "¿Cómo voy?")),
        )

        assertEquals(DataResult.Success(ChatReply(markdown)), result)
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
            context = context,
            language = "es",
            messages = listOf(AssistantTurn(role = AssistantRole.User, text = "Hola")),
        )

        assertEquals(DataResult.Success(ChatReply("**Hola**\n* uno")), result)
    }

    @Test
    fun `maps an empty chat body to an empty response`() = runTest {
        val source = dataSource(
            baseUrl = "http://10.0.2.2:8000",
            transport = FakeRecommendationTransport(TransportResult.Http(200, """{"message":"  "}""")),
        )

        val result = source.send(
            context = context,
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
            context = context,
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

        source.send(context, "es", hello)

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

        val result = source.send(context, "es", hello)

        assertEquals(DataResult.Success(ChatReply("Hola")), result)
        assertEquals(listOf("session-token", "fresh-token"), transport.tokens)
        assertEquals(1, tokens.refreshCalls)
    }

    // CA-05
    @Test
    fun `a second 401 ends with session expired and no more retries`() = runTest {
        val transport = FakeRecommendationTransport(TransportResult.Http(401, UNAUTHORIZED_BODY))
        val tokens = FakeAccessTokens()
        val source = dataSource(BASE_URL, transport, tokens)

        val result = source.send(context, "es", hello)

        assertEquals(DataResult.Error(RecommendationError.SessionExpired), result)
        assertEquals(2, transport.tokens.size)
        assertEquals(1, tokens.refreshCalls)
    }

    @Test
    fun `a rejected refresh ends with session expired without retrying`() = runTest {
        val transport = FakeRecommendationTransport(TransportResult.Http(401, UNAUTHORIZED_BODY))
        val source = dataSource(BASE_URL, transport, FakeAccessTokens(refresh = TokenRefresh.SignedOut))

        val result = source.send(context, "es", hello)

        assertEquals(DataResult.Error(RecommendationError.SessionExpired), result)
        assertEquals(1, transport.tokens.size)
    }

    @Test
    fun `a refresh that cannot reach supabase reports unavailable, not session expired`() = runTest {
        val transport = FakeRecommendationTransport(TransportResult.Http(401, UNAUTHORIZED_BODY))
        val source = dataSource(BASE_URL, transport, FakeAccessTokens(refresh = TokenRefresh.Failed))

        val result = source.send(context, "es", hello)

        assertEquals(DataResult.Error(RecommendationError.Unavailable), result)
        assertEquals(1, transport.tokens.size)
    }

    // CA-06
    @Test
    fun `without a session nothing is sent and the session error is returned`() = runTest {
        val transport = FakeRecommendationTransport(TransportResult.Http(200, OK_BODY))
        val tokens = FakeAccessTokens(current = null)
        val source = dataSource(BASE_URL, transport, tokens)

        val result = source.send(context, "es", hello)

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

        source.send(context, "es", hello)

        assertFalse(transport.body.orEmpty().contains("secret-session-token"))
    }

    private val hello = listOf(AssistantTurn(role = AssistantRole.User, text = "Hola"))

    private companion object {
        const val BASE_URL = "http://10.0.2.2:8000"
        const val OK_BODY = """{"message":"Hola"}"""
        const val UNAUTHORIZED_BODY = """{"detail":"Sesión no válida."}"""
    }

    @Test
    fun `sends today, the catalog, habit schedules and the outcome of earlier proposals`() = runTest {
        val transport = FakeRecommendationTransport(TransportResult.Http(200, """{"message":"Ok"}"""))
        val source = dataSource(baseUrl = "http://10.0.2.2:8000", transport = transport)
        val gym = HabitWeekStat(
            name = "Gym",
            completedDays = 1,
            totalDays = 2,
            id = "h-gym",
            schedule = HabitSchedule.WEEKDAYS,
            days = setOf(DayOfWeek.FRIDAY, DayOfWeek.MONDAY),
            weeklyTarget = 7,
            doneDates = listOf(LocalDate(2026, 11, 9)),
        )
        val proposal = AssistantAction.SetCompletion("h-gym", "Gym", "fitness_center", "#294536", LocalDate(2026, 11, 9), false)

        source.send(
            context = context.copy(habits = listOf(gym)),
            language = "es",
            messages = listOf(
                AssistantTurn(AssistantRole.User, "quita el gym del lunes"),
                AssistantTurn(AssistantRole.Assistant, "¿Lo quito?", proposal, AssistantActionStatus.Cancelled),
                AssistantTurn(AssistantRole.User, "¿qué ha pasado?"),
            ),
        )

        val body = transport.body.orEmpty()
        assertTrue(body.contains("\"today\":\"2026-11-12\""))
        assertTrue(body.contains("\"catalog\":{\"icons\":[\"menu_book\"],\"colors\":[\"#294536\"]}"))
        assertTrue(body.contains("\"id\":\"h-gym\""))
        assertTrue(body.contains("\"schedule\":{\"mode\":\"weekdays\",\"days\":[\"MONDAY\",\"FRIDAY\"]}"))
        assertTrue(body.contains("\"done_dates\":[\"2026-11-09\"]"))
        assertTrue(body.contains("\"action\":{\"type\":\"set_completion\",\"habit_id\":\"h-gym\",\"date\":\"2026-11-09\",\"completed\":false}"))
        assertTrue(body.contains("\"action_status\":\"cancelled\""))
    }

    @Test
    fun `reads a proposed action next to the text`() = runTest {
        val payload = """{"message":"Mira:","action":{"type":"create_habit","name":"Leer","icon":"menu_book","color":"#294536","schedule":{"mode":"weekly_count","weekly_target":3}}}"""
        val source = dataSource(baseUrl = "http://10.0.2.2:8000", transport = FakeRecommendationTransport(TransportResult.Http(200, payload)))

        val result = source.send(context, "es", listOf(AssistantTurn(AssistantRole.User, "crea leer")))

        assertEquals(
            DataResult.Success(
                ChatReply(
                    text = "Mira:",
                    proposal = ProposedAction.CreateHabit("Leer", null, "menu_book", "#294536", "weekly_count", emptyList(), 3),
                ),
            ),
            result,
        )
    }

    // CA-23
    @Test
    fun `a malformed action is dropped and the text is kept`() = runTest {
        listOf(
            """{"message":"Hola","action":{"type":"set_completion","habit_id":"h","date":"2026-11-11","completed":"yes"}}""",
            """{"message":"Hola","action":{"type":"delete_habit"}}""",
            """{"message":"Hola","action":"oops"}""",
        ).forEach { payload ->
            val source = dataSource(baseUrl = "http://10.0.2.2:8000", transport = FakeRecommendationTransport(TransportResult.Http(200, payload)))

            val result = source.send(context, "es", listOf(AssistantTurn(AssistantRole.User, "hola")))

            assertEquals(payload, DataResult.Success(ChatReply("Hola")), result)
        }
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
