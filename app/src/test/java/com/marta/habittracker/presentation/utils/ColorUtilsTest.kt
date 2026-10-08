package com.marta.habittracker.presentation.utils

import androidx.compose.ui.graphics.Color
import com.marta.habittracker.presentation.theme.HabitColor1
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorUtilsTest {

    @Test
    fun `habit color options has twelve unique colors in two rows of six`() {
        assertEquals(12, HabitColorOptions.size)
        assertEquals(12, HabitColorOptions.distinct().size)

        val rows = HabitColorOptions.chunked(6)
        assertEquals(2, rows.size)
        assertTrue(rows.all { it.size == 6 })
    }

    @Test
    fun `habit color options starts with color 1 and follows rainbow hue order`() {
        assertEquals(HabitColor1, HabitColorOptions.first())
        assertEquals("#F04F45", HabitColor1.toHex())

        val hues = HabitColorOptions.map { hueDegrees(it) }
        hues.zipWithNext().forEach { (previous, next) ->
            assertTrue("hues should increase: $previous then $next", next > previous)
        }
    }

    private fun hueDegrees(color: Color): Float {
        val red = color.red
        val green = color.green
        val blue = color.blue
        val max = maxOf(red, green, blue)
        val min = minOf(red, green, blue)
        val delta = max - min
        if (delta < 0.0001f) return 0f
        val hue = when (max) {
            red -> ((green - blue) / delta)
            green -> ((blue - red) / delta) + 2f
            else -> ((red - green) / delta) + 4f
        }
        return ((hue * 60f) + 360f) % 360f
    }
}
