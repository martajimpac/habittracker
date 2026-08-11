package com.marta.habittracker.presentation.widgets.challenge

import com.marta.habittracker.domain.model.Challenge
import com.marta.habittracker.domain.model.ChallengeCard
import com.marta.habittracker.domain.model.ChallengeCriteria
import com.marta.habittracker.domain.model.ChallengeStatus
import com.marta.habittracker.domain.model.Profile
import java.time.Instant
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ChallengeWidgetSnapshotTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `snapshot codec preserves the selected challenge card fields`() {
        val card = sampleCard()

        val snapshot = decodeChallengeWidgetSnapshot(
            encodeChallengeWidgetSnapshot(card, json),
            json,
        )

        assertEquals("challenge-1", snapshot.challengeId)
        assertEquals("Morning Run", snapshot.habitName)
        assertEquals("#FF6B6B", snapshot.habitColorHex)
        assertEquals("Alex Rivera", snapshot.opponentDisplayName)
        assertEquals(75, snapshot.myProgress)
        assertEquals(50, snapshot.theirProgress)
        assertEquals(7, snapshot.daysLeft)
        assertEquals("active", snapshot.status)
    }

    @Test
    fun `decode rejects invalid snapshot json`() {
        assertThrows(SerializationException::class.java) {
            decodeChallengeWidgetSnapshot("{not-json", json)
        }
    }

    @Test
    fun `encode uses challenge id and opponent display name`() {
        val encoded = encodeChallengeWidgetSnapshot(sampleCard(), json)
        val snapshot = decodeChallengeWidgetSnapshot(encoded, json)

        assertEquals("challenge-1", snapshot.challengeId)
        assertEquals("Alex Rivera", snapshot.opponentDisplayName)
        assertEquals("active", snapshot.status)
    }

    @Test
    fun `pending challenge encodes pending status`() {
        val pending = sampleCard().copy(
            challenge = sampleCard().challenge.copy(status = ChallengeStatus.Pending),
        )

        val snapshot = decodeChallengeWidgetSnapshot(
            encodeChallengeWidgetSnapshot(pending, json),
            json,
        )

        assertEquals("pending", snapshot.status)
    }

    private fun sampleCard() = ChallengeCard(
        challenge = Challenge(
            id = "challenge-1",
            challengerId = "me",
            challengedId = "opponent",
            challengerHabitId = "habit-1",
            challengedHabitId = "habit-2",
            criteria = ChallengeCriteria.CompletionPct,
            startsAt = Instant.parse("2026-08-01T00:00:00Z"),
            endsAt = Instant.parse("2026-08-14T00:00:00Z"),
            status = ChallengeStatus.Active,
        ),
        opponent = Profile(
            id = "opponent",
            username = "alex",
            displayName = "Alex Rivera",
            avatarColor = "#6750A4",
        ),
        myProgress = 75,
        theirProgress = 50,
        daysLeft = 7,
        habitName = "Morning Run",
        habitIcon = "directions_run",
        habitColorHex = "#FF6B6B",
    )
}
