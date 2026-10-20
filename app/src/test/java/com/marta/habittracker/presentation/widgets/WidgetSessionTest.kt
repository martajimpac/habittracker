package com.marta.habittracker.presentation.widgets

import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.model.AppError
import com.marta.habittracker.domain.widget.ChallengeWidgetLookup
import com.marta.habittracker.domain.widget.onHabitSyncFinished
import com.marta.habittracker.domain.widget.onSignedOut
import com.marta.habittracker.domain.widget.resolveChallengeWidgetSnapshot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WidgetSessionTest {

    @Test
    fun `successful habit sync refreshes widgets`() = runTest {
        var refreshes = 0

        onHabitSyncFinished(DataResult.Success(Unit)) { refreshes++ }

        assertEquals(1, refreshes)
    }

    @Test
    fun `failed habit sync leaves widgets as they are`() = runTest {
        var refreshes = 0

        onHabitSyncFinished(DataResult.Error(AppError.Common.Network)) { refreshes++ }

        assertEquals(0, refreshes)
    }

    @Test
    fun `successful sign out clears the local account`() = runTest {
        var clears = 0

        onSignedOut(DataResult.Success(Unit)) { clears++ }

        assertEquals(1, clears)
    }

    @Test
    fun `failed sign out keeps the local account`() = runTest {
        var clears = 0

        onSignedOut(DataResult.Error(AppError.Common.Network)) { clears++ }

        assertEquals(0, clears)
    }

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
