package com.marta.habittracker.presentation.widgets

import android.content.Context
import android.content.Intent
import com.marta.habittracker.MainActivity

object WidgetLaunchExtras {
    const val EXTRA_BOTTOM_TAB = "extra_bottom_tab"
    const val EXTRA_HABIT_ID = "extra_habit_id"

    const val TAB_HOME = "home"
    const val TAB_STATS = "stats"
    const val TAB_FRIENDS = "friends"

    fun openHomeIntent(context: Context, habitId: String? = null): Intent =
        launchIntent(context, TAB_HOME).apply {
            habitId?.let { putExtra(EXTRA_HABIT_ID, it) }
        }

    fun openFriendsIntent(context: Context): Intent = launchIntent(context, TAB_FRIENDS)

    fun openStatsIntent(context: Context): Intent = launchIntent(context, TAB_STATS)

    fun tabFromIntent(intent: Intent?): String? =
        intent?.getStringExtra(EXTRA_BOTTOM_TAB)?.takeIf {
            it == TAB_HOME || it == TAB_STATS || it == TAB_FRIENDS
        }

    private fun launchIntent(context: Context, tab: String): Intent =
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_BOTTOM_TAB, tab)
        }
}
