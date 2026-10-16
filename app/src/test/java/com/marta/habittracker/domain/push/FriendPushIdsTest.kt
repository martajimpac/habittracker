package com.marta.habittracker.domain.push

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class FriendPushIdsTest {
    @Test
    fun `notification id is never zero`() {
        assertNotEquals(0, FriendPushIds.notificationIdFor(""))
        assertNotEquals(0, FriendPushIds.notificationIdFor("friendship-1"))
    }

    @Test
    fun `same friendship id is stable and distinct from a challenge id`() {
        val id = FriendPushIds.notificationIdFor("abc")
        assertEquals(id, FriendPushIds.notificationIdFor("abc"))
        assertNotEquals(ChallengePushIds.notificationIdFor("abc"), id)
    }
}
