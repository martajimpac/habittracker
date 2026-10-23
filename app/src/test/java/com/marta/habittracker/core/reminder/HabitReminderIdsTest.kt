package com.marta.habittracker.core.reminder

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HabitReminderIdsTest {

    @Test
    fun `notificationIdFor never returns zero`() {
        // Empty string hashCode is 0; FCM already uses notify id 0.
        assertNotEquals(0, HabitReminderIds.notificationIdFor(""))
        assertNotEquals(0, HabitReminderIds.notificationIdFor("habit-1"))
    }

    @Test
    fun `notificationIdFor is stable for the same habit id`() {
        val habitId = "habit-42"
        assertEquals(
            HabitReminderIds.notificationIdFor(habitId),
            HabitReminderIds.notificationIdFor(habitId),
        )
    }

    @Test
    fun `notificationIdFor is positive and matches unsigned hash except zero`() {
        val habitId = "habit-1"
        val expected = (habitId.hashCode() and 0x7fffffff).let { if (it == 0) 1 else it }
        assertEquals(expected, HabitReminderIds.notificationIdFor(habitId))
        assertTrue(HabitReminderIds.notificationIdFor(habitId) > 0)
        assertEquals(1, HabitReminderIds.notificationIdFor(""))
    }

    @Test
    fun `complete action id is stable and distinct from the alarm id`() {
        val habitId = "habit-1"
        val actionId = HabitReminderIds.completeActionIdFor(habitId)
        assertNotEquals(0, actionId)
        assertEquals(actionId, HabitReminderIds.completeActionIdFor(habitId))
        assertNotEquals(HabitReminderIds.notificationIdFor(habitId), actionId)
    }
}
