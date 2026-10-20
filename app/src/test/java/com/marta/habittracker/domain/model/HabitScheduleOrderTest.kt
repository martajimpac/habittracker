package com.marta.habittracker.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class HabitScheduleOrderTest {

    @Test
    fun `habits with a reminder come first ordered by time`() {
        val late = habit(id = "late", time = "18:00", reminder = true)
        val early = habit(id = "early", time = "07:30", reminder = true)
        val noReminder = habit(id = "quiet", time = "06:00", reminder = false)

        val ordered = listOf(noReminder, late, early).sortedForSchedule()

        assertEquals(listOf("early", "late", "quiet"), ordered.map { it.id })
    }

    @Test
    fun `habits without a reminder stay last and keep time order`() {
        val none = habit(id = "none", time = null, reminder = false)
        val afternoon = habit(id = "afternoon", time = "16:00", reminder = false)
        val morning = habit(id = "morning", time = "09:00", reminder = false)

        val ordered = listOf(none, afternoon, morning).sortedForSchedule()

        assertEquals(listOf("morning", "afternoon", "none"), ordered.map { it.id })
    }

    private fun habit(id: String, time: String?, reminder: Boolean) = Habit(
        id = id,
        name = id,
        description = null,
        daysOfWeek = emptySet(),
        icon = "water_drop",
        colorHex = "#294536",
        reminderTime = time,
        createdAt = Instant.EPOCH,
        records = emptyList(),
        reminderEnabled = reminder,
    )
}
