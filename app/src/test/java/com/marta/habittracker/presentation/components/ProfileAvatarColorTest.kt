package com.marta.habittracker.presentation.components

import androidx.compose.ui.graphics.Color
import com.marta.habittracker.presentation.theme.HabitPrimaryLight
import org.junit.Assert.assertEquals
import org.junit.Test

class ProfileAvatarColorTest {

    @Test
    fun `initials avatar always uses the brand green`() {
        val stored = Color(0xFF6750A4)

        assertEquals(HabitPrimaryLight, avatarBackgroundColor(avatarUrl = null, storedColor = stored))
        assertEquals(HabitPrimaryLight, avatarBackgroundColor(avatarUrl = "", storedColor = stored))
        assertEquals(HabitPrimaryLight, avatarBackgroundColor(avatarUrl = "   ", storedColor = stored))
    }

    @Test
    fun `photo avatar keeps the stored color behind the image`() {
        val stored = Color(0xFF6750A4)

        assertEquals(
            stored,
            avatarBackgroundColor(avatarUrl = "https://example.com/a.jpg", storedColor = stored),
        )
    }
}
