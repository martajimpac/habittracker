package com.marta.habittracker.presentation.theme

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Horizontal inset for back / close actions in top app bars and screen headers. */
val HabitTopBarHorizontalPadding = 16.dp

fun Modifier.habitTopBarButtonHorizontalPadding(): Modifier =
    padding(horizontal = HabitTopBarHorizontalPadding)
