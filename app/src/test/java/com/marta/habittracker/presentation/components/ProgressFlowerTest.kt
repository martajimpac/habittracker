package com.marta.habittracker.presentation.components

import org.junit.Assert.assertEquals
import org.junit.Test

class ProgressFlowerTest {

    @Test
    fun `no progress has not started growing`() {
        assertGrowth(0f, stem = 0f, leaves = 0f, bloom = 0f)
    }

    @Test
    fun `early progress grows the stem before the flower`() {
        val growth = flowerGrowth(0.3f)

        assertEquals(0.5f, growth.stem, 0.001f)
        assertEquals(0f, growth.bloom, 0.001f)
        assertEquals(true, growth.leaves < growth.stem)
    }

    @Test
    fun `full progress finishes stem leaves and bloom`() {
        assertGrowth(1f, stem = 1f, leaves = 1f, bloom = 1f)
    }

    private fun assertGrowth(progress: Float, stem: Float, leaves: Float, bloom: Float) {
        val growth = flowerGrowth(progress)
        assertEquals(stem, growth.stem, 0.001f)
        assertEquals(leaves, growth.leaves, 0.001f)
        assertEquals(bloom, growth.bloom, 0.001f)
    }
}
