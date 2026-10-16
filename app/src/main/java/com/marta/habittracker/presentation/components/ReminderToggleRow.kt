package com.marta.habittracker.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.marta.habittracker.R
import com.marta.habittracker.presentation.theme.HabitField
import com.marta.habittracker.presentation.theme.HabitOnSurface
import com.marta.habittracker.presentation.theme.InstaDevTheme

@Composable
fun ReminderToggleRow(
    enabled: Boolean,
    reminderTime: String,
    onEnabledChange: (Boolean) -> Unit,
    onTimeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(HabitField.copy(alpha = 0.5f))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.habit_reminder_toggle),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = HabitOnSurface,
            )
            Switch(
                checked = enabled,
                onCheckedChange = onEnabledChange,
            )
        }
        if (enabled) {
            Text(
                text = stringResource(R.string.add_habit_reminder_time),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(HabitField)
                    .clickable(onClick = onTimeClick)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                Text(
                    text = reminderTime,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = HabitOnSurface,
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Reminder off")
@Composable
private fun ReminderToggleRowOffPreview() {
    InstaDevTheme {
        ReminderToggleRow(
            enabled = false,
            reminderTime = "08:00",
            onEnabledChange = {},
            onTimeClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true, name = "Reminder on")
@Composable
private fun ReminderToggleRowOnPreview() {
    InstaDevTheme {
        ReminderToggleRow(
            enabled = true,
            reminderTime = "08:00",
            onEnabledChange = {},
            onTimeClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
