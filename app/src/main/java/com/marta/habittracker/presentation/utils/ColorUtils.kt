package com.marta.habittracker.presentation.utils

import androidx.compose.ui.graphics.Color
import com.marta.habittracker.domain.model.Habit
import com.marta.habittracker.presentation.theme.HabitPrimary
import kotlin.math.roundToInt
import androidx.core.graphics.toColorInt
import com.marta.habittracker.presentation.theme.HabitColor1
import com.marta.habittracker.presentation.theme.HabitColor10
import com.marta.habittracker.presentation.theme.HabitColor11
import com.marta.habittracker.presentation.theme.HabitColor12
import com.marta.habittracker.presentation.theme.HabitColor2
import com.marta.habittracker.presentation.theme.HabitColor3
import com.marta.habittracker.presentation.theme.HabitColor4
import com.marta.habittracker.presentation.theme.HabitColor5
import com.marta.habittracker.presentation.theme.HabitColor6
import com.marta.habittracker.presentation.theme.HabitColor7
import com.marta.habittracker.presentation.theme.HabitColor8
import com.marta.habittracker.presentation.theme.HabitColor9

fun Color.toHex(): String {
    return String.format(
        "#%02X%02X%02X",
        (red * 255).roundToInt(),
        (green * 255).roundToInt(),
        (blue * 255).roundToInt(),
    )
}

fun String.toColor(): Color {
    return Color(this.toColorInt())
}

fun parseHabitColor(colorHex: String): Color {
    val cleaned = colorHex.removePrefix("#").trim()
    if (cleaned.length != 6) return HabitPrimary
    return runCatching {
        Color(("FF$cleaned").toLong(16))
    }.getOrDefault(HabitPrimary)
}

fun habitAccentColor(habit: Habit): Color = parseHabitColor(habit.colorHex)


val HabitColorOptions = listOf(
    HabitColor1,
    HabitColor2,
    HabitColor3,
    HabitColor4,
    HabitColor5,
    HabitColor6,
    HabitColor7,
    HabitColor8,
    HabitColor9,
    HabitColor10,
    HabitColor11,
    HabitColor12,
)