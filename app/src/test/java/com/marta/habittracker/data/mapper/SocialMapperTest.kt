package com.marta.habittracker.data.mapper

import com.marta.habittracker.data.remote.dto.ChallengeDto
import com.marta.habittracker.data.remote.dto.FriendshipDto
import com.marta.habittracker.data.remote.dto.ProfileDto
import com.marta.habittracker.domain.model.ChallengeCriteria
import com.marta.habittracker.domain.model.ChallengeStatus
import com.marta.habittracker.domain.model.FriendshipStatus
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class SocialMapperTest {

    private val mapper = SocialMapper()

    @Test
    fun `profile dto maps snake case fields to domain profile`() {
        val profile = with(mapper) {
            ProfileDto(
                id = "profile-1",
                username = "marta",
                displayName = "Marta",
                avatarColor = "#6750A4",
                avatarUrl = "https://example.com/avatar.jpg",
            ).toDomain()
        }

        assertEquals("profile-1", profile.id)
        assertEquals("marta", profile.username)
        assertEquals("Marta", profile.displayName)
        assertEquals("#6750A4", profile.avatarColor)
        assertEquals("https://example.com/avatar.jpg", profile.avatarUrl)
    }

    @Test
    fun `profile dto maps null avatar url to domain`() {
        val profile = with(mapper) {
            ProfileDto(
                id = "profile-2",
                username = "alex",
                displayName = "Alex",
                avatarColor = "#2563EB",
            ).toDomain()
        }

        assertEquals(null, profile.avatarUrl)
    }

    @Test
    fun `friendship dto maps accepted status to domain enum`() {
        val friendship = with(mapper) {
            FriendshipDto(
                id = "friendship-1",
                requesterId = "profile-1",
                addresseeId = "profile-2",
                status = "accepted",
            ).toDomain()
        }

        assertEquals(FriendshipStatus.Accepted, friendship.status)
    }

    @Test
    fun `challenge dto maps criteria status and timestamps to domain`() {
        val challenge = with(mapper) {
            ChallengeDto(
                id = "challenge-1",
                challengerId = "profile-1",
                challengedId = "profile-2",
                challengerHabitId = "habit-1",
                challengedHabitId = "habit-2",
                criteria = "completion_pct",
                startsAt = "2026-07-31T00:00:00Z",
                endsAt = "2026-08-07T00:00:00Z",
                status = "active",
            ).toDomain()
        }

        assertEquals(ChallengeCriteria.CompletionPct, challenge.criteria)
        assertEquals(ChallengeStatus.Active, challenge.status)
        assertEquals(Instant.parse("2026-07-31T00:00:00Z"), challenge.startsAt)
        assertEquals(Instant.parse("2026-08-07T00:00:00Z"), challenge.endsAt)
    }
}
