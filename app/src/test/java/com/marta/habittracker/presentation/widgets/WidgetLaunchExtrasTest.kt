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

    @Test
    fun `habitIdFromIntent returns extra when not blank`() {
        val intent = WidgetLaunchExtras.openHomeIntent(
            RuntimeEnvironment.getApplication(),
            habitId = "habit-42",
        )

        assertEquals("habit-42", WidgetLaunchExtras.habitIdFromIntent(intent))
    }

    @Test
    fun `habitIdFromIntent returns null for blank missing or null intent`() {
        assertNull(WidgetLaunchExtras.habitIdFromIntent(null))
        assertNull(WidgetLaunchExtras.habitIdFromIntent(Intent()))
        assertNull(
            WidgetLaunchExtras.habitIdFromIntent(
                Intent().putExtra(WidgetLaunchExtras.EXTRA_HABIT_ID, "  "),
            ),
        )
        assertNull(
            WidgetLaunchExtras.habitIdFromIntent(
                WidgetLaunchExtras.openHomeIntent(RuntimeEnvironment.getApplication()),
            ),
        )
    }

    @Test
    fun `openChallengeIntent includes challenge id`() {
        val intent = WidgetLaunchExtras.openChallengeIntent(
            RuntimeEnvironment.getApplication(),
            challengeId = "challenge-7",
        )

        assertEquals("challenge-7", intent.getStringExtra(WidgetLaunchExtras.EXTRA_CHALLENGE_ID))
    }

    @Test
    fun `openFriendRequestIntent opens friends and keeps the friendship id`() {
        val intent = WidgetLaunchExtras.openFriendRequestIntent(
            RuntimeEnvironment.getApplication(),
            friendshipId = "friendship-3",
        )

        assertEquals(WidgetLaunchExtras.TAB_FRIENDS, WidgetLaunchExtras.tabFromIntent(intent))
        assertEquals("friendship-3", WidgetLaunchExtras.friendRequestIdFromIntent(intent))
    }

    @Test
    fun `friendRequestIdFromIntent returns null for blank missing or null intent`() {
        assertNull(WidgetLaunchExtras.friendRequestIdFromIntent(null))
        assertNull(WidgetLaunchExtras.friendRequestIdFromIntent(Intent()))
        assertNull(
            WidgetLaunchExtras.friendRequestIdFromIntent(
                Intent().putExtra(WidgetLaunchExtras.EXTRA_FRIEND_REQUEST_ID, "  "),
            ),
        )
    }

    @Test
    fun `challengeIdFromIntent returns null for blank missing or null intent`() {
        assertNull(WidgetLaunchExtras.challengeIdFromIntent(null))
        assertNull(WidgetLaunchExtras.challengeIdFromIntent(Intent()))
        assertNull(
            WidgetLaunchExtras.challengeIdFromIntent(
                Intent().putExtra(WidgetLaunchExtras.EXTRA_CHALLENGE_ID, "  "),
            ),
        )
    }
}
