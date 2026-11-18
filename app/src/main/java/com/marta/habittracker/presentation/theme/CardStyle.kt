package com.marta.habittracker.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Corner radius shared by the app's surface cards (Home, Stats, Profile, Preferences…). */
val HabitCardShape = RoundedCornerShape(24.dp)

/** Soft outline for surface cards so every screen uses the same subtle edge. */
@Composable
fun habitCardBorderColor(): Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
