package com.marta.habittracker.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ChallengeLeaderTest {

    @Test
    fun `higher progress is the leader`() {
        assertEquals(ChallengeLeader.Me, challengeLeader(60, 40))
        assertEquals("Me", challengeLeaderLabel(60, 40, "Me", "Alex", "Tie"))
    }

    @Test
    fun `opponent leads when their progress is higher`() {
        assertEquals(ChallengeLeader.Opponent, challengeLeader(20, 80))
        assertEquals("Alex", challengeLeaderLabel(20, 80, "Me", "Alex", "Tie"))
    }

    @Test
    fun `equal progress is a tie`() {
        assertEquals(ChallengeLeader.Tie, challengeLeader(0, 0))
        assertEquals("Tie", challengeLeaderLabel(50, 50, "Me", "Alex", "Tie"))
    }
}
