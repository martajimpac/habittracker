package com.marta.habittracker.domain.widget

import com.marta.habittracker.domain.model.Habit

interface AppWidgetRefresher {
    suspend fun refreshTodayWidget(habits: List<Habit>)
}
