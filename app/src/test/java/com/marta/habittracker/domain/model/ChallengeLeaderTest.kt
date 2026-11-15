package com.marta.habittracker.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ChallengeLeaderTest {

    @Test
    fun `higher progress is the leader`() {
        assertEquals(ChallengeLeader.Me, challengeLeader(60, 40))
    }

    @Test
    fun `opponent leads when their progress is higher`() {
        assertEquals(ChallengeLeader.Opponent, challengeLeader(20, 80))
    }

    @Test
    fun `equal progress is a tie`() {
        assertEquals(ChallengeLeader.Tie, challengeLeader(0, 0))
    }
}
