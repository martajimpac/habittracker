package com.marta.habittracker.domain.reminder

import com.marta.habittracker.domain.model.Habit
import com.marta.habittracker.domain.usecase.shownOn
import kotlinx.datetime.LocalDate

const val DAILY_CHECK_IN_ID = "daily_check_in"

fun shouldSendDailyCheckIn(habits: List<Habit>, today: LocalDate): Boolean =
    habits.any { habit ->
        habit.shownOn(today) && habit.records.none { it.isCompleted && it.date == today }
    }
