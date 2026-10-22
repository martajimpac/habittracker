package com.marta.habittracker.presentation.widgets.today

import com.marta.habittracker.core.toJava
import com.marta.habittracker.domain.model.Habit
import com.marta.habittracker.domain.usecase.shownOn
import java.time.LocalDate
import kotlinx.datetime.LocalDate as KotlinLocalDate

data class TodayWidgetItem(
    val id: String,
    val name: String,
    val completed: Boolean,
    val iconKey: String = "water_drop",
    val colorHex: String = "#3D6E50",
)

data class TodayWidgetSnapshot(
    val items: List<TodayWidgetItem>,
    val firstPendingId: String?,
)

fun todayWidgetSnapshot(habits: List<Habit>, today: LocalDate): TodayWidgetSnapshot {
    val items = habits
        .filter { it.shownOn(KotlinLocalDate.parse(today.toString())) }
        .map { habit ->
            TodayWidgetItem(
                id = habit.id,
                name = habit.name,
                completed = habit.records.any { it.isCompleted && it.date.toJava() == today },
                iconKey = habit.icon,
                colorHex = habit.colorHex,
            )
        }
    return TodayWidgetSnapshot(
        items = items,
        firstPendingId = items.firstOrNull { !it.completed }?.id,
    )
}
