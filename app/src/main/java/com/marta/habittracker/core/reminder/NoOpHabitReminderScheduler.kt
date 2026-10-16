package com.marta.habittracker.core.reminder

import android.util.Log
import com.marta.habittracker.domain.model.Habit
import com.marta.habittracker.domain.reminder.HabitReminderScheduler
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoOpHabitReminderScheduler @Inject constructor() : HabitReminderScheduler {

    override fun schedule(habit: Habit) {
        Log.d(TAG, "schedule skipped (noop) habitId=${habit.id}")
    }

    override fun cancel(habitId: String) {
        Log.d(TAG, "cancel skipped (noop) habitId=$habitId")
    }

    override fun rescheduleAll(habits: List<Habit>) {
        Log.d(TAG, "rescheduleAll skipped (noop) count=${habits.size}")
    }

    companion object {
        private const val TAG = "HabitReminderSched"
    }
}
