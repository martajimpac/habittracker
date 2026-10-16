package com.marta.habittracker.core.reminder

import com.marta.habittracker.domain.reminder.HabitReminderScheduler
import com.marta.habittracker.domain.repository.HabitRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface HabitReminderEntryPoint {
    fun habitRepository(): HabitRepository
    fun habitReminderScheduler(): HabitReminderScheduler
}
