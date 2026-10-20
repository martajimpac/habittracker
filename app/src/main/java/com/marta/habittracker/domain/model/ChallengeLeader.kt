package com.marta.habittracker.domain.model

enum class ChallengeLeader {
    Me,
    Opponent,
    Tie,
}

fun challengeLeader(myProgress: Int, theirProgress: Int): ChallengeLeader = when {
    myProgress > theirProgress -> ChallengeLeader.Me
    theirProgress > myProgress -> ChallengeLeader.Opponent
    else -> ChallengeLeader.Tie
}

fun challengeLeaderLabel(
    myProgress: Int,
    theirProgress: Int,
    meLabel: String,
    opponentName: String,
    tieLabel: String,
): String = when (challengeLeader(myProgress, theirProgress)) {
    ChallengeLeader.Me -> meLabel
    ChallengeLeader.Opponent -> opponentName
    ChallengeLeader.Tie -> tieLabel
}
