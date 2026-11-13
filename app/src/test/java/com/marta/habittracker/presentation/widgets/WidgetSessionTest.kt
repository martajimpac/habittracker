package com.marta.habittracker.presentation.widgets

import com.marta.habittracker.domain.widget.ChallengeWidgetLookup
import com.marta.habittracker.domain.widget.resolveChallengeWidgetSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WidgetSessionTest {

    @Test
    fun `challenge widget hides the previous account when there is no session`() {
        val snapshot = resolveChallengeWidgetSnapshot(
            hasSession = false,
            cachedJson = """{"opponent":"Ana"}""",
            lookup = ChallengeWidgetLookup.Unavailable,
        )

        assertNull(snapshot)
    }

    @Test
    fun `challenge widget hides a challenge that is not on this account`() {
        val snapshot = resolveChallengeWidgetSnapshot(
            hasSession = true,
            cachedJson = """{"opponent":"Ana"}""",
            lookup = ChallengeWidgetLookup.MissingFromAccount,
        )

        assertNull(snapshot)
    }

    @Test
    fun `challenge widget keeps the cache when this account is offline`() {
        val cached = """{"opponent":"Ana"}"""

        val snapshot = resolveChallengeWidgetSnapshot(
            hasSession = true,
            cachedJson = cached,
            lookup = ChallengeWidgetLookup.Unavailable,
        )

        assertEquals(cached, snapshot)
    }

    @Test
    fun `challenge widget uses the refreshed snapshot for this account`() {
        val snapshot = resolveChallengeWidgetSnapshot(
            hasSession = true,
            cachedJson = """{"opponent":"Ana"}""",
            lookup = ChallengeWidgetLookup.Found("""{"opponent":"Luis"}"""),
        )

        assertEquals("""{"opponent":"Luis"}""", snapshot)
    }
}
