package com.marta.habittracker.domain.usecase

import com.marta.habittracker.domain.model.Profile
import org.junit.Assert.assertEquals
import org.junit.Test

class MatchChallengeFriendTest {

    private fun profile(id: String, displayName: String, username: String) =
        Profile(id = id, username = username, displayName = displayName, avatarColor = "#FF7775")

    private val lauraG = profile("1", "Laura García", "laurag")
    private val lauraM = profile("2", "Laura Martín", "lauram")
    private val angel = profile("3", "Ángel Ruiz", "angel_r")

    // CA-08
    @Test
    fun `no friends`() {
        assertEquals(FriendMatch.NoFriends, matchChallengeFriend("Laura", emptyList()))
    }

    // CA-07
    @Test
    fun `unknown name is not found`() {
        assertEquals(FriendMatch.NotFound, matchChallengeFriend("Pedro", listOf(lauraG, angel)))
    }

    // CA-07
    @Test
    fun `several partial matches are ambiguous and sorted by name`() {
        assertEquals(
            FriendMatch.Ambiguous(listOf(lauraG, lauraM)),
            matchChallengeFriend("laura", listOf(lauraM, lauraG, angel)),
        )
    }

    // CA-07: after the app asks, the person answers with the full name.
    @Test
    fun `exact display name wins over partial matches`() {
        assertEquals(FriendMatch.Match(lauraG), matchChallengeFriend("Laura García", listOf(lauraG, lauraM)))
    }

    @Test
    fun `ignores case accents spaces and a leading at sign`() {
        assertEquals(FriendMatch.Match(angel), matchChallengeFriend("  angel   RUIZ ", listOf(angel, lauraG)))
        assertEquals(FriendMatch.Match(lauraM), matchChallengeFriend("@LauraM", listOf(lauraG, lauraM)))
    }

    @Test
    fun `a word of the name matches`() {
        assertEquals(FriendMatch.Match(lauraM), matchChallengeFriend("martin", listOf(lauraG, lauraM)))
    }

    @Test
    fun `single letter is not found`() {
        assertEquals(FriendMatch.NotFound, matchChallengeFriend("a", listOf(angel)))
    }

    // CA-10 and RF-05
    @Test
    fun `durations round to the closest allowed one, longer on a tie`() {
        assertEquals(7, nearestChallengeDuration(10))
        assertEquals(7, nearestChallengeDuration(5))
        assertEquals(3, nearestChallengeDuration(1))
        assertEquals(30, nearestChallengeDuration(60))
        assertEquals(14, nearestChallengeDuration(14))
        assertEquals(30, nearestChallengeDuration(22))
    }
}
