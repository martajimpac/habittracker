package com.marta.habittracker.presentation.widgets.challenge

import com.marta.habittracker.presentation.components.avatarInitials
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChallengeWidgetCardTest {

    private val strings = ChallengeWidgetCardStrings(
        me = "Me",
        initialsFallback = "?",
        pending = "Pending",
        lastDay = "Last day",
        daysRemaining = { days -> "$days days left" },
        score = { score, total -> "$score/$total" },
    )

    private fun snapshot(
        myProgress: Int = 60,
        theirProgress: Int = 40,
        myScore: Int = 4,
        theirScore: Int = 3,
        scoreTotal: Int = 7,
        daysLeft: Int = 5,
        status: String = "active",
        myDisplayName: String = "Marta Jiménez",
    ) = ChallengeWidgetSnapshot(
        challengeId = "c1",
        habitName = "Run",
        habitColorHex = "#F27A24",
        opponentDisplayName = "Alex",
        myProgress = myProgress,
        theirProgress = theirProgress,
        daysLeft = daysLeft,
        status = status,
        habitIcon = "directions_run",
        myDisplayName = myDisplayName,
        myAvatarColor = "#3A98EA",
        opponentAvatarColor = "#12CEA8",
        myScore = myScore,
        theirScore = theirScore,
        scoreTotal = scoreTotal,
    )

    // CA-03
    @Test
    fun `when I lead only my avatar carries the trophy and my bar is emphasized`() {
        val ui = snapshot(myProgress = 60, theirProgress = 40).toCardUi(strings)

        assertTrue(ui.me.isLeader)
        assertFalse(ui.opponent.isLeader)
        assertTrue(ui.me.emphasized)
        assertFalse(ui.opponent.emphasized)
    }

    @Test
    fun `when the rival leads only the rival carries the trophy and my bar is dimmed`() {
        val ui = snapshot(myProgress = 20, theirProgress = 80).toCardUi(strings)

        assertFalse(ui.me.isLeader)
        assertTrue(ui.opponent.isLeader)
        assertFalse(ui.me.emphasized)
        assertTrue(ui.opponent.emphasized)
    }

    @Test
    fun `a tie has no trophy and both bars emphasized`() {
        val ui = snapshot(myProgress = 50, theirProgress = 50).toCardUi(strings)

        assertFalse(ui.me.isLeader)
        assertFalse(ui.opponent.isLeader)
        assertTrue(ui.me.emphasized)
        assertTrue(ui.opponent.emphasized)
    }

    // CA-04
    @Test
    fun `bars share the same score scale`() {
        val ui = snapshot(myScore = 4, theirScore = 3, scoreTotal = 7).toCardUi(strings)

        assertEquals("4/7", ui.me.scoreText)
        assertEquals("3/7", ui.opponent.scoreText)
        assertEquals(4f / 7f, ui.me.barFraction, 0.0001f)
        assertEquals(3f / 7f, ui.opponent.barFraction, 0.0001f)
    }

    // CA-05
    @Test
    fun `days left uses the plural text and zero days says last day`() {
        assertEquals("5 days left", snapshot(daysLeft = 5).toCardUi(strings).daysText)
        assertEquals("Last day", snapshot(daysLeft = 0).toCardUi(strings).daysText)
    }

    @Test
    fun `pending challenge shows the pending label and active does not`() {
        assertEquals("Pending", snapshot(status = "pending").toCardUi(strings).pendingText)
        assertNull(snapshot(status = "active").toCardUi(strings).pendingText)
    }

    // RF-02, RF-03
    @Test
    fun `participants get names and initials like the app`() {
        val ui = snapshot(myDisplayName = "Marta Jiménez").toCardUi(strings)

        assertEquals("Marta Jiménez", ui.me.name)
        assertEquals("MJ", ui.me.initials)
        assertEquals("#3A98EA", ui.me.avatarColorHex)
        assertEquals("Alex", ui.opponent.name)
        assertEquals("AL", ui.opponent.initials)
        assertEquals("#12CEA8", ui.opponent.avatarColorHex)
    }

    @Test
    fun `without my profile name the local participant is called Me`() {
        val ui = snapshot(myDisplayName = "").toCardUi(strings)

        assertEquals("Me", ui.me.name)
        assertEquals("ME", ui.me.initials)
    }

    // CA-09
    @Test
    fun `an old snapshot without scores falls back to the percentage`() {
        val old = ChallengeWidgetSnapshot(
            challengeId = "c1",
            habitName = "Run",
            habitColorHex = "#F27A24",
            opponentDisplayName = "Alex",
            myProgress = 60,
            theirProgress = 40,
            daysLeft = 5,
        )

        val ui = old.toCardUi(strings)

        assertEquals("60/100", ui.me.scoreText)
        assertEquals("40/100", ui.opponent.scoreText)
        assertEquals(0.6f, ui.me.barFraction, 0.0001f)
        assertEquals("Me", ui.me.name)
        assertEquals("", ui.me.avatarColorHex)
        assertTrue(ui.me.isLeader)
    }

    @Test
    fun `avatar initials keep the app rule`() {
        assertEquals("MJ", avatarInitials("Marta Jiménez", "?"))
        assertEquals("AL", avatarInitials("alex", "?"))
        assertEquals("?", avatarInitials("   ", "?"))
    }
}
