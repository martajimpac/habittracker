package com.marta.habittracker.domain.reminder

import com.marta.habittracker.domain.model.Habit

class FakeHabitReminderScheduler : HabitReminderScheduler {
    val scheduled = mutableListOf<Habit>()
    val cancelled = mutableListOf<String>()
    val rescheduled = mutableListOf<List<Habit>>()

    override fun schedule(habit: Habit) {
        scheduled.add(habit)
    }

    override fun cancel(habitId: String) {
        cancelled.add(habitId)
    }

    override fun rescheduleAll(habits: List<Habit>) {
        rescheduled.add(habits)
    }
}
