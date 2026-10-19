package com.marta.habittracker.presentation.utils

import com.marta.habittracker.R
import org.junit.Assert.assertEquals
import org.junit.Test

class GreetingForHourTest {

    @Test
    fun `morning lasts through 13`() {
        assertEquals(R.string.home_greeting_morning, greetingForHour(0))
        assertEquals(R.string.home_greeting_morning, greetingForHour(11))
        assertEquals(R.string.home_greeting_morning, greetingForHour(12))
        assertEquals(R.string.home_greeting_morning, greetingForHour(13))
    }

    @Test
    fun `afternoon runs from 14 until 20`() {
        assertEquals(R.string.home_greeting_afternoon, greetingForHour(14))
        assertEquals(R.string.home_greeting_afternoon, greetingForHour(17))
        assertEquals(R.string.home_greeting_afternoon, greetingForHour(20))
    }

    @Test
    fun `evening starts at 21`() {
        assertEquals(R.string.home_greeting_evening, greetingForHour(21))
        assertEquals(R.string.home_greeting_evening, greetingForHour(23))
    }
}
