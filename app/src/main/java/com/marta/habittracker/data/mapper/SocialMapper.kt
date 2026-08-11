package com.marta.habittracker.data.mapper

import com.marta.habittracker.data.remote.dto.ChallengeDto
import com.marta.habittracker.data.remote.dto.FriendshipDto
import com.marta.habittracker.data.remote.dto.ProfileDto
import com.marta.habittracker.domain.model.Challenge
import com.marta.habittracker.domain.model.ChallengeCriteria
import com.marta.habittracker.domain.model.ChallengeStatus
import com.marta.habittracker.domain.model.Friendship
import com.marta.habittracker.domain.model.FriendshipStatus
import com.marta.habittracker.domain.model.Profile
import java.time.Instant
import javax.inject.Inject

class SocialMapper @Inject constructor() {

    fun ProfileDto.toDomain(): Profile = Profile(
        id = id,
        username = username,
        displayName = displayName,
        avatarColor = avatarColor,
    )

    fun FriendshipDto.toDomain(): Friendship = Friendship(
        id = id,
        requesterId = requesterId,
        addresseeId = addresseeId,
        status = status.toFriendshipStatus(),
    )

    fun ChallengeDto.toDomain(): Challenge = Challenge(
        id = id,
        challengerId = challengerId,
        challengedId = challengedId,
        challengerHabitId = challengerHabitId,
        challengedHabitId = challengedHabitId,
        criteria = criteria.toChallengeCriteria(),
        startsAt = Instant.parse(startsAt),
        endsAt = Instant.parse(endsAt),
        status = status.toChallengeStatus(),
    )

    private fun String.toFriendshipStatus(): FriendshipStatus = when (this) {
        "pending" -> FriendshipStatus.Pending
        "accepted" -> FriendshipStatus.Accepted
        "rejected" -> FriendshipStatus.Rejected
        else -> error("Unknown friendship status: $this")
    }

    private fun String.toChallengeCriteria(): ChallengeCriteria = when (this) {
        "streak" -> ChallengeCriteria.Streak
        "all_days" -> ChallengeCriteria.AllDays
        "completion_pct" -> ChallengeCriteria.CompletionPct
        else -> error("Unknown challenge criteria: $this")
    }

    private fun String.toChallengeStatus(): ChallengeStatus = when (this) {
        "pending" -> ChallengeStatus.Pending
        "active" -> ChallengeStatus.Active
        "completed" -> ChallengeStatus.Completed
        "declined" -> ChallengeStatus.Declined
        "cancelled" -> ChallengeStatus.Cancelled
        else -> error("Unknown challenge status: $this")
    }
}
