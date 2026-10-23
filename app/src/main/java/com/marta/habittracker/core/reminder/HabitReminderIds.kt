package com.marta.habittracker.core.reminder

object HabitReminderIds {
    /**
     * Stable per-habit notification / PendingIntent request code.
     * Never 0 — FCM already uses notify id 0.
     * Distinct habit ids may collide on [String.hashCode]; that is accepted.
     */
    fun notificationIdFor(habitId: String): Int =
        stableId(habitId)

    /**
     * Request code for the notification "complete" action.
     * Must stay distinct from [notificationIdFor] so the action does not replace the alarm.
     */
    fun completeActionIdFor(habitId: String): Int = stableId("complete:$habitId")

    private fun stableId(key: String): Int =
        (key.hashCode() and 0x7fffffff).let { if (it == 0) 1 else it }
}
