package com.marta.habittracker.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.marta.habittracker.R
import com.marta.habittracker.presentation.theme.HabitField
import com.marta.habittracker.presentation.theme.HabitOnSurface

internal fun stepDaysPerWeek(current: Int, delta: Int): Int =
    (current + delta).coerceIn(1, 7)

@Composable
fun DaysPerWeekStepper(
    days: Int,
    onDaysChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val value = days.coerceIn(1, 7)
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(
            modifier = Modifier
                .clip(shape)
                .background(HabitField)
                .border(1.dp, MaterialTheme.colorScheme.outline, shape),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            StepperArrow(
                icon = Icons.Rounded.KeyboardArrowUp,
                enabled = value < 7,
                contentDescription = stringResource(R.string.cd_days_per_week_increase),
                onClick = { onDaysChange(stepDaysPerWeek(value, 1)) },
            )
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = HabitOnSurface,
            )
            StepperArrow(
                icon = Icons.Rounded.KeyboardArrowDown,
                enabled = value > 1,
                contentDescription = stringResource(R.string.cd_days_per_week_decrease),
                onClick = { onDaysChange(stepDaysPerWeek(value, -1)) },
            )
        }
        Text(
            text = pluralStringResource(R.plurals.frequency_days_per_week_label, value),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = HabitOnSurface,
        )
    }
}

@Composable
private fun StepperArrow(
    icon: ImageVector,
    enabled: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(40.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = HabitOnSurface.copy(alpha = if (enabled) 1f else 0.3f),
        )
    }
}
