package com.marta.habittracker.presentation.widgets

import android.content.Context
import com.marta.habittracker.domain.model.Habit
import com.marta.habittracker.domain.widget.AppWidgetRefresher
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GlanceAppWidgetRefresher @Inject constructor(
    @ApplicationContext private val context: Context,
) : AppWidgetRefresher {
    override suspend fun refreshTodayWidget(habits: List<Habit>) {
        WidgetRefresher.refreshTodayWidget(habits, context)
    }
}
