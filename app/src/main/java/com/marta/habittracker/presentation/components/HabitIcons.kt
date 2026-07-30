package com.marta.habittracker.presentation.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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

fun habitIconVector(iconKey: String?): ImageVector = when (iconKey) {
    HabitIconKeys.WATER, "💧" -> Icons.Outlined.WaterDrop
    HabitIconKeys.RUN, "🏃" -> Icons.AutoMirrored.Outlined.DirectionsRun
    HabitIconKeys.BOOK, "📚" -> Icons.AutoMirrored.Outlined.MenuBook
    HabitIconKeys.MEDITATE, "🧘" -> Icons.Outlined.SelfImprovement
    HabitIconKeys.FOOD, "🍎" -> Icons.Outlined.Restaurant
    HabitIconKeys.SLEEP, "😴" -> Icons.Outlined.Bedtime
    HabitIconKeys.COFFEE, "☕" -> Icons.Outlined.LocalCafe
    HabitIconKeys.MUSIC, "🎵" -> Icons.Outlined.MusicNote
    HabitIconKeys.WRITE, "✍️" -> Icons.Outlined.Edit
    HabitIconKeys.HEART, "❤️" -> Icons.Outlined.FavoriteBorder
    HabitIconKeys.TARGET, "🎯" -> Icons.Outlined.TrackChanges
    HabitIconKeys.BOLT, "⚡" -> Icons.Outlined.Bolt
    else -> Icons.Outlined.WaterDrop
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
        imageVector = habitIconVector(iconKey),
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
