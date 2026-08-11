package com.marta.habittracker.presentation.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.marta.habittracker.R
import com.marta.habittracker.domain.model.Habit
import com.marta.habittracker.presentation.theme.HabitPrimary

/** Stable keys stored in Room / Supabase for habit icons. */
val HabitIconOptions: List<String> = listOf(
    HabitIconKeys.WATER,
    HabitIconKeys.RUN,
    HabitIconKeys.BOOK,
    HabitIconKeys.MEDITATE,
    HabitIconKeys.FOOD,
    HabitIconKeys.SLEEP,
    HabitIconKeys.COFFEE,
    HabitIconKeys.MUSIC,
    HabitIconKeys.WRITE,
    HabitIconKeys.HEART,
    HabitIconKeys.TARGET,
    HabitIconKeys.BOLT,
)

object HabitIconKeys {
    const val WATER = "water_drop"
    const val RUN = "directions_run"
    const val BOOK = "menu_book"
    const val MEDITATE = "self_improvement"
    const val FOOD = "restaurant"
    const val SLEEP = "bedtime"
    const val COFFEE = "local_cafe"
    const val MUSIC = "music_note"
    const val WRITE = "edit"
    const val HEART = "favorite_border"
    const val TARGET = "track_changes"
    const val BOLT = "bolt"

    const val DEFAULT = WATER
}

@DrawableRes
fun habitIconRes(iconKey: String?): Int = when (iconKey) {
    HabitIconKeys.WATER -> R.drawable.ic_habit_water_drop
    HabitIconKeys.RUN -> R.drawable.ic_habit_directions_run
    HabitIconKeys.BOOK -> R.drawable.ic_habit_menu_book
    HabitIconKeys.MEDITATE -> R.drawable.ic_habit_self_improvement
    HabitIconKeys.FOOD -> R.drawable.ic_habit_restaurant
    HabitIconKeys.SLEEP -> R.drawable.ic_habit_bedtime
    HabitIconKeys.COFFEE -> R.drawable.ic_habit_local_cafe
    HabitIconKeys.MUSIC -> R.drawable.ic_habit_music_note
    HabitIconKeys.WRITE -> R.drawable.ic_habit_edit
    HabitIconKeys.HEART -> R.drawable.ic_habit_favorite_border
    HabitIconKeys.TARGET -> R.drawable.ic_habit_track_changes
    HabitIconKeys.BOLT -> R.drawable.ic_habit_bolt
    else -> R.drawable.ic_habit_water_drop
}

@Composable
fun HabitLineIcon(
    iconKey: String?,
    modifier: Modifier = Modifier,
    tint: Color = HabitPrimary,
    size: Dp = 24.dp,
    contentDescription: String? = null,
) {
    Icon(
        painter = painterResource(habitIconRes(iconKey)),
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        tint = tint,
    )
}

@Composable
fun HabitLineIcon(
    habit: Habit,
    modifier: Modifier = Modifier,
    tint: Color = HabitPrimary,
    size: Dp = 24.dp,
) {
    HabitLineIcon(
        iconKey = habit.icon,
        modifier = modifier,
        tint = tint,
        size = size,
    )
}
