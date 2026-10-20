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
    HabitIconKeys.FITNESS,
    HabitIconKeys.NO_SMOKING,
    HabitIconKeys.NO_PHONE,
    HabitIconKeys.PHONE,
    HabitIconKeys.MESSAGE,
    HabitIconKeys.LAPTOP,
    HabitIconKeys.STUDY,
    HabitIconKeys.STEPS,
    HabitIconKeys.MONEY,
    HabitIconKeys.TREE,
    HabitIconKeys.BRIEFCASE,
    HabitIconKeys.SUN,
    HabitIconKeys.APPLE,
    HabitIconKeys.CLEANING,
    HabitIconKeys.CART,
    HabitIconKeys.FLOWER_BLOOM,
    HabitIconKeys.FLOWER_LOTUS,
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
    const val FITNESS = "fitness_center"
    const val NO_SMOKING = "smoke_free"
    const val NO_PHONE = "mobile_off"
    const val PHONE = "smartphone"
    const val MESSAGE = "message"
    const val LAPTOP = "laptop"
    const val STUDY = "school"
    const val STEPS = "directions_walk"
    const val MONEY = "attach_money"
    const val TREE = "park"
    const val BRIEFCASE = "business_center"
    const val SUN = "wb_sunny"
    const val APPLE = "apple"
    const val CLEANING = "cleaning_services"
    const val CART = "shopping_cart"
    const val FLOWER_BLOOM = "flower_bloom"
    const val FLOWER_TULIP = "flower_tulip"
    const val FLOWER_LOTUS = "flower_lotus"
    const val FLOWER_BLOSSOM = "flower_blossom"

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
    HabitIconKeys.FITNESS -> R.drawable.ic_habit_fitness_center
    HabitIconKeys.NO_SMOKING -> R.drawable.ic_habit_smoke_free
    HabitIconKeys.NO_PHONE -> R.drawable.ic_habit_mobile_off
    HabitIconKeys.PHONE -> R.drawable.ic_habit_smartphone
    HabitIconKeys.MESSAGE -> R.drawable.ic_habit_message
    HabitIconKeys.LAPTOP -> R.drawable.ic_habit_laptop
    HabitIconKeys.STUDY -> R.drawable.ic_habit_school
    HabitIconKeys.STEPS -> R.drawable.ic_habit_directions_walk
    HabitIconKeys.MONEY -> R.drawable.ic_habit_attach_money
    HabitIconKeys.TREE -> R.drawable.ic_habit_park
    HabitIconKeys.BRIEFCASE -> R.drawable.ic_habit_business_center
    HabitIconKeys.SUN -> R.drawable.ic_habit_wb_sunny
    HabitIconKeys.APPLE -> R.drawable.ic_habit_apple
    HabitIconKeys.CLEANING -> R.drawable.ic_habit_cleaning_services
    HabitIconKeys.CART -> R.drawable.ic_habit_shopping_cart
    HabitIconKeys.FLOWER_BLOOM -> R.drawable.ic_flower_bloom
    HabitIconKeys.FLOWER_TULIP -> R.drawable.ic_flower_tulip
    HabitIconKeys.FLOWER_LOTUS -> R.drawable.ic_flower_lotus
    HabitIconKeys.FLOWER_BLOSSOM -> R.drawable.ic_flower_blossom
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
