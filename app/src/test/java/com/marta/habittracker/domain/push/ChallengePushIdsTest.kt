package com.marta.habittracker.domain.push

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ChallengePushIdsTest {
    @Test
    fun `notification id is never zero`() {
        assertNotEquals(0, ChallengePushIds.notificationIdFor(""))
        assertNotEquals(0, ChallengePushIds.notificationIdFor("challenge-1"))
    }

    @Test
    fun `same challenge id is stable and distinct from the raw habit-style hash`() {
        val id = ChallengePushIds.notificationIdFor("abc")
        assertEquals(id, ChallengePushIds.notificationIdFor("abc"))
        val habitStyle = ("abc".hashCode() and 0x7fffffff).let { if (it == 0) 1 else it }
        assertNotEquals(habitStyle, id)
    }
}
