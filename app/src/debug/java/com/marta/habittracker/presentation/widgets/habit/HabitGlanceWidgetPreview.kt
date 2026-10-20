package com.marta.habittracker.presentation.widgets.habit

import androidx.compose.runtime.Composable
import androidx.glance.action.action
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import com.marta.habittracker.domain.model.Habit
import com.marta.habittracker.presentation.components.HabitIconKeys
import com.marta.habittracker.presentation.theme.HabitColor1
import com.marta.habittracker.presentation.utils.toHex
import java.time.Instant
import kotlinx.datetime.DayOfWeek

// @OptIn(ExperimentalGlancePreviewApi::class)
// @Preview(widthDp = 180, heightDp = 110)
// @Composable
// fun HabitGlanceWidgetPreview() {
    // HabitWidgetContent(
        // habit = Habit(
            // id = "preview",
            // name = "Read",
            // description = null,
            // daysOfWeek = setOf(DayOfWeek.MONDAY),
            // icon = HabitIconKeys.BOOK,
            // colorHex = HabitColor1.toHex(),
            // reminderTime = null,
            // createdAt = Instant.EPOCH,
            // records = emptyList(),
            // isCompleted = true,
            // isPublic = false,
        // ),
        // configureText = "Configure",
        // completedText = "Completed today",
        // notCompletedText = "Not completed today",
        // dayName = "Tuesday",
        // streakText = "3 day streak",
        // openAppAction = action { },
        // toggleAction = action { },
    // )
// }
