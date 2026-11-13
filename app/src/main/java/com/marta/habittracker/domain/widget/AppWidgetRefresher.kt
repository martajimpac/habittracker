package com.marta.habittracker.domain.widget

import com.marta.habittracker.domain.model.Habit

interface AppWidgetRefresher {
    /** [habits] must be today's list as Home shows it: scheduled today, same order, with today's status. */
    suspend fun refreshTodayWidget(habits: List<Habit>)
}
