package com.marta.habittracker.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun CustomButton(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
    text: String,
) {
    HabitButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        variant = HabitButtonVariant.Primary,
    )
}
