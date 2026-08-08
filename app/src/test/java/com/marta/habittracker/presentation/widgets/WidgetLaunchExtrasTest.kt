package com.marta.habittracker.presentation.widgets

import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import android.app.Application

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26], application = Application::class)
class WidgetLaunchExtrasTest {

    @Test
    fun `tabFromIntent returns known tabs only`() {
        val context = RuntimeEnvironment.getApplication()

        assertEquals(
            WidgetLaunchExtras.TAB_FRIENDS,
            WidgetLaunchExtras.tabFromIntent(WidgetLaunchExtras.openFriendsIntent(context)),
        )
        assertEquals(
            WidgetLaunchExtras.TAB_STATS,
            WidgetLaunchExtras.tabFromIntent(WidgetLaunchExtras.openStatsIntent(context)),
        )
        assertEquals(
            WidgetLaunchExtras.TAB_HOME,
            WidgetLaunchExtras.tabFromIntent(WidgetLaunchExtras.openHomeIntent(context, "habit-1")),
        )
        assertNull(WidgetLaunchExtras.tabFromIntent(Intent()))
        assertNull(
            WidgetLaunchExtras.tabFromIntent(
                Intent().putExtra(WidgetLaunchExtras.EXTRA_BOTTOM_TAB, "profile"),
            ),
        )
    }

    @Test
    fun `openHomeIntent includes optional habit id`() {
        val intent = WidgetLaunchExtras.openHomeIntent(
            RuntimeEnvironment.getApplication(),
            habitId = "habit-42",
        )

        assertEquals("habit-42", intent.getStringExtra(WidgetLaunchExtras.EXTRA_HABIT_ID))
    }
}
