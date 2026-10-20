package com.marta.habittracker.domain.reminder

import com.marta.habittracker.domain.model.Habit

interface HabitReminderScheduler {
    fun schedule(habit: Habit)
    fun cancel(habitId: String)
    fun rescheduleAll(habits: List<Habit>)
}

internal fun afterHabitPersisted(habit: Habit, scheduler: HabitReminderScheduler) {
    if (!habit.reminderEnabled || habit.reminderTime.isNullOrBlank()) scheduler.cancel(habit.id)
    else scheduler.schedule(habit)
}

internal fun afterHabitDeleted(habitId: String, scheduler: HabitReminderScheduler) {
    scheduler.cancel(habitId)
}

internal fun afterHabitsSynced(habits: List<Habit>, scheduler: HabitReminderScheduler) {
    scheduler.rescheduleAll(habits)
}
