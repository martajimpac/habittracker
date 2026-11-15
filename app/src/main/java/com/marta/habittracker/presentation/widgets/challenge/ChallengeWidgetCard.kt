package com.marta.habittracker.presentation.widgets.challenge

import com.marta.habittracker.domain.model.ChallengeLeader
import com.marta.habittracker.domain.model.challengeLeader
import com.marta.habittracker.presentation.components.avatarInitials
import com.marta.habittracker.presentation.components.challengeBarFraction

/** One side of the challenge card in the widget, already resolved for drawing. */
data class ChallengeWidgetParticipantUi(
    val name: String,
    val initials: String,
    /** Avatar color as stored in the profile; blank when unknown (older snapshots). */
    val avatarColorHex: String,
    val isLeader: Boolean,
    val score: Int,
    val scoreTotal: Int,
    val scoreText: String,
    val barFraction: Float,
    /** False when this participant is behind: bar and score are drawn dimmed, like in the app. */
    val emphasized: Boolean,
)

/** Everything the challenge widget card draws, mirroring the app's ChallengeVersusCard. */
data class ChallengeWidgetCardUi(
    val habitName: String,
    val habitIcon: String,
    val habitColorHex: String,
    val me: ChallengeWidgetParticipantUi,
    val opponent: ChallengeWidgetParticipantUi,
    val daysText: String,
    /** Shown under the days when the challenge is still pending; null otherwise. */
    val pendingText: String?,
)

/** Localized texts the card needs; resolved by the caller from string resources. */
class ChallengeWidgetCardStrings(
    val me: String,
    val initialsFallback: String,
    val pending: String,
    val lastDay: String,
    val daysRemaining: (days: Int) -> String,
    val score: (score: Int, total: Int) -> String,
)

internal fun ChallengeWidgetSnapshot.toCardUi(strings: ChallengeWidgetCardStrings): ChallengeWidgetCardUi {
    // Same leader rule as the app card: by completion progress.
    val leader = challengeLeader(myProgress, theirProgress)

    // Snapshots saved before the card design have no score scale: fall back to the
    // percentage until the next refresh fills the real scores.
    val usesScores = scoreTotal > 0
    val total = if (usesScores) scoreTotal else PERCENT_TOTAL
    val myValue = if (usesScores) myScore else myProgress.coerceIn(0, PERCENT_TOTAL)
    val theirValue = if (usesScores) theirScore else theirProgress.coerceIn(0, PERCENT_TOTAL)

    val myName = myDisplayName.takeIf { it.isNotBlank() } ?: strings.me

    return ChallengeWidgetCardUi(
        habitName = habitName,
        habitIcon = habitIcon,
        habitColorHex = habitColorHex,
        me = ChallengeWidgetParticipantUi(
            name = myName,
            initials = avatarInitials(myName, strings.initialsFallback),
            avatarColorHex = myAvatarColor,
            isLeader = leader == ChallengeLeader.Me,
            score = myValue,
            scoreTotal = total,
            scoreText = strings.score(myValue, total),
            barFraction = challengeBarFraction(myValue, total),
            emphasized = leader != ChallengeLeader.Opponent,
        ),
        opponent = ChallengeWidgetParticipantUi(
            name = opponentDisplayName,
            initials = avatarInitials(opponentDisplayName, strings.initialsFallback),
            avatarColorHex = opponentAvatarColor,
            isLeader = leader == ChallengeLeader.Opponent,
            score = theirValue,
            scoreTotal = total,
            scoreText = strings.score(theirValue, total),
            barFraction = challengeBarFraction(theirValue, total),
            emphasized = leader != ChallengeLeader.Me,
        ),
        daysText = if (daysLeft > 0) strings.daysRemaining(daysLeft) else strings.lastDay,
        pendingText = strings.pending.takeIf { status == STATUS_PENDING },
    )
}

private const val PERCENT_TOTAL = 100
private const val STATUS_PENDING = "pending"
