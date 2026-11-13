package com.marta.habittracker.presentation.widgets.today

import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class TodayWidgetMidnightAlarmTest {

    private val madrid = ZoneId.of("Europe/Madrid")

    @Test
    fun `fires just after the next local midnight`() {
        val now = ZonedDateTime.of(2026, 11, 13, 22, 30, 0, 0, madrid)

        val trigger = TodayWidgetMidnightAlarm.nextTrigger(now)

        assertEquals(ZonedDateTime.of(2026, 11, 14, 0, 0, 5, 0, madrid).toInstant(), trigger)
    }

    @Test
    fun `right after midnight it targets the following night, not the one that just passed`() {
        val now = ZonedDateTime.of(2026, 11, 14, 0, 0, 5, 0, madrid)

        val trigger = TodayWidgetMidnightAlarm.nextTrigger(now)

        assertEquals(ZonedDateTime.of(2026, 11, 15, 0, 0, 5, 0, madrid).toInstant(), trigger)
    }

    @Test
    fun `daylight saving change still lands on local midnight`() {
        // Spain moves clocks back on 2026-10-25 at 03:00.
        val now = ZonedDateTime.of(2026, 10, 24, 12, 0, 0, 0, madrid)

        val trigger = TodayWidgetMidnightAlarm.nextTrigger(now)

        assertEquals(ZonedDateTime.of(2026, 10, 25, 0, 0, 5, 0, madrid).toInstant(), trigger)
    }
}
