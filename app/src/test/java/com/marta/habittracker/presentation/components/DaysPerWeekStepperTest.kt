package com.marta.habittracker.presentation.components

import org.junit.Assert.assertEquals
import org.junit.Test

class DaysPerWeekStepperTest {

    @Test
    fun `arrows stay between 1 and 7 days`() {
        assertEquals(2, stepDaysPerWeek(1, 1))
        assertEquals(1, stepDaysPerWeek(1, -1))
        assertEquals(7, stepDaysPerWeek(7, 1))
        assertEquals(6, stepDaysPerWeek(7, -1))
        assertEquals(4, stepDaysPerWeek(3, 1))
    }
}
