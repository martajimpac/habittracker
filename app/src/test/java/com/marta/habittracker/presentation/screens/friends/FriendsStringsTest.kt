package com.marta.habittracker.presentation.screens.friends

import android.app.Application
import com.marta.habittracker.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26], application = Application::class)
class FriendsStringsTest {

    @Test
    fun `friends empty and add copy exist`() {
        val context = RuntimeEnvironment.getApplication()
        assertEquals("Add", context.getString(R.string.friends_add))
        assertEquals("No friends yet", context.getString(R.string.friends_empty_title))
        assertTrue(context.getString(R.string.friends_empty_message).isNotBlank())
    }
}
