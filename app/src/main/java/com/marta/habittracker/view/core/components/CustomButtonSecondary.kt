package com.marta.habittracker.view.core.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun CustomButtonSecondary(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    title: String,
) {
    HabitButton(
        text = title,
        onClick = onClick,
        modifier = modifier,
        variant = HabitButtonVariant.Secondary,
    )
}
