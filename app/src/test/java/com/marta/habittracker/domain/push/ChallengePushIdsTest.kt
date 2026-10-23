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

    @Test
    fun `declined notification id is stable and distinct from the created challenge`() {
        val created = ChallengePushIds.notificationIdFor("abc")
        val declined = ChallengePushIds.declinedNotificationIdFor("abc")
        assertNotEquals(0, declined)
        assertEquals(declined, ChallengePushIds.declinedNotificationIdFor("abc"))
        assertNotEquals(created, declined)
    }
}
