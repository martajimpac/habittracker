package com.marta.habittracker.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChallengeMembershipTest {

    @Test
    fun `challenger habit is the one on their list`() {
        assertEquals(
            "mine",
            ownChallengeHabitId(
                userId = "me",
                challengerId = "me",
                challengedId = "them",
                challengerHabitId = "mine",
                challengedHabitId = null,
            ),
        )
    }

    @Test
    fun `challenged habit appears only after they have a copy`() {
        assertNull(
            ownChallengeHabitId(
                userId = "me",
                challengerId = "them",
                challengedId = "me",
                challengerHabitId = "theirs",
                challengedHabitId = null,
            ),
        )
        assertEquals(
            "copy",
            ownChallengeHabitId(
                userId = "me",
                challengerId = "them",
                challengedId = "me",
                challengerHabitId = "theirs",
                challengedHabitId = "copy",
            ),
        )
    }
}
