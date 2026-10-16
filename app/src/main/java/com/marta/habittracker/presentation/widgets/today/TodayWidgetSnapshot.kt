package com.marta.habittracker.presentation.widgets.today

import com.marta.habittracker.core.toJava
import com.marta.habittracker.domain.model.Habit
import java.time.LocalDate

const val TODAY_WIDGET_ROW_LIMIT = 5

data class TodayWidgetItem(
    val id: String,
    val name: String,
    val completed: Boolean,
)

data class TodayWidgetSnapshot(
    val items: List<TodayWidgetItem>,
    val firstPendingId: String?,
)

data class TodayWidgetDisplay(
    val rows: List<TodayWidgetItem>,
    val overflowCount: Int,
)

fun todayWidgetSnapshot(habits: List<Habit>, today: LocalDate): TodayWidgetSnapshot {
    val weekday = today.dayOfWeek.name
    val items = habits
        .filter { habit -> habit.daysOfWeek.any { it.name == weekday } }
        .map { habit ->
            TodayWidgetItem(
                id = habit.id,
                name = habit.name,
                completed = habit.records.any { it.isCompleted && it.date.toJava() == today },
            )
        }
    return TodayWidgetSnapshot(
        items = items,
        firstPendingId = items.firstOrNull { !it.completed }?.id,
    )
}

fun todayWidgetDisplay(
    items: List<TodayWidgetItem>,
    limit: Int = TODAY_WIDGET_ROW_LIMIT,
): TodayWidgetDisplay = TodayWidgetDisplay(
    rows = items.take(limit),
    overflowCount = (items.size - limit).coerceAtLeast(0),
)
