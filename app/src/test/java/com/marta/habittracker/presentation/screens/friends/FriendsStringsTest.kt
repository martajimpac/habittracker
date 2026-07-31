package com.marta.habittracker.presentation.screens.friends

import com.marta.habittracker.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class FriendsStringsTest {

    @Test
    fun `friends empty and add copy exist`() {
        val context = RuntimeEnvironment.getApplication()
        assertEquals("Add", context.getString(R.string.friends_add))
        assertEquals("No friends yet", context.getString(R.string.friends_empty_title))
        assertTrue(context.getString(R.string.friends_empty_message).isNotBlank())
    }
}
