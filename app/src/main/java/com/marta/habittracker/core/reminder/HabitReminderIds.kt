package com.marta.habittracker.core.reminder

object HabitReminderIds {
    /**
     * Stable per-habit notification / PendingIntent request code.
     * Never 0 — FCM already uses notify id 0.
     * Distinct habit ids may collide on [String.hashCode]; that is accepted.
     */
    fun notificationIdFor(habitId: String): Int =
        (habitId.hashCode() and 0x7fffffff).let { if (it == 0) 1 else it }
}
