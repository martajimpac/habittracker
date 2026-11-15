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
