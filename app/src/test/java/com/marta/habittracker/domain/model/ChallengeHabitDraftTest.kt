package com.marta.habittracker.domain.model

import kotlinx.datetime.DayOfWeek
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class ChallengeHabitDraftTest {

    @Test
    fun `shared challenge habit keeps the chosen days and is public`() {
        val habit = newChallengeHabit(
            id = "habit",
            draft = ChallengeHabitDraft(
                name = "  Leer  ",
                icon = "menu_book",
                colorHex = "#3A98EA",
                daysOfWeek = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY),
            ),
            createdAt = Instant.EPOCH,
        )

        assertEquals("Leer", habit.name)
        assertEquals(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY), habit.daysOfWeek)
        assertEquals("menu_book", habit.icon)
        assertEquals("#3A98EA", habit.colorHex)
        assertTrue(habit.isPublic)
        assertFalse(habit.reminderEnabled)
        assertEquals(null, habit.reminderTime)
    }

    @Test
    fun `accepted copy uses the same schedule as the challenger habit`() {
        val source = newChallengeHabit(
            id = "mine",
            draft = ChallengeHabitDraft(
                name = "Leer",
                icon = "menu_book",
                colorHex = "#3A98EA",
                daysOfWeek = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY),
            ),
            createdAt = Instant.EPOCH,
        )

        val copy = copyChallengeHabit(source, id = "theirs", createdAt = Instant.parse("2026-10-20T00:00:00Z"))

        assertEquals("theirs", copy.id)
        assertEquals(source.daysOfWeek, copy.daysOfWeek)
        assertEquals(source.name, copy.name)
        assertEquals(source.icon, copy.icon)
        assertEquals(source.colorHex, copy.colorHex)
        assertTrue(copy.records.isEmpty())
    }
}
