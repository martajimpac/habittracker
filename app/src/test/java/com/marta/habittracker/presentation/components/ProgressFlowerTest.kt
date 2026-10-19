package com.marta.habittracker.presentation.components

import org.junit.Assert.assertEquals
import org.junit.Test

class ProgressFlowerTest {

    @Test
    fun `no progress leaves every petal empty`() {
        assertEquals(listOf(0f, 0f, 0f, 0f, 0f), petalFillAmounts(0f))
    }

    @Test
    fun `half progress fills two petals and half of the next`() {
        assertEquals(listOf(1f, 1f, 0.5f, 0f, 0f), petalFillAmounts(0.5f))
    }

    @Test
    fun `full progress fills every petal`() {
        assertEquals(listOf(1f, 1f, 1f, 1f, 1f), petalFillAmounts(1f))
    }
}
