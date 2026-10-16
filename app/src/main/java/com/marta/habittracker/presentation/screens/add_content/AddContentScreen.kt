package com.marta.habittracker.presentation.screens.add_content

import android.Manifest
import android.app.AlarmManager
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.marta.habittracker.R
import com.marta.habittracker.presentation.components.CustomTextField
import com.marta.habittracker.presentation.components.HabitIconOptions
import com.marta.habittracker.presentation.components.HabitLineIcon
import com.marta.habittracker.presentation.components.ReminderToggleRow
import com.marta.habittracker.presentation.theme.HabitField
import com.marta.habittracker.presentation.theme.HabitOnSurface
import com.marta.habittracker.presentation.theme.HabitOnSurfaceVariant
import com.marta.habittracker.presentation.theme.HabitPrimary
import com.marta.habittracker.presentation.utils.ObserveAsEvents
import java.time.DayOfWeek
import java.time.format.TextStyle
import androidx.compose.ui.platform.LocalLocale
import com.marta.habittracker.presentation.utils.HabitColorOptions

@Composable
fun AddContentScreen(
    onBack: () -> Unit = {},
    viewModel: AddContentViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            viewModel.onReminderEnabled(true)
        } else {
            viewModel.onReminderEnabled(false)
            viewModel.onReminderDenied(R.string.habit_reminder_permission_denied)
        }
    }

    ObserveAsEvents(flow = viewModel.habitSaved) {
        onBack()
    }

    AddContentContent(
        uiState = uiState,
        onBack = onBack,
        onNameChanged = viewModel::onNameChanged,
        onDescriptionChanged = viewModel::onDescriptionChanged,
        onIconSelected = viewModel::onIconSelected,
        onColorSelected = viewModel::onColorSelected,
        onReminderEnabledChange = { enabled ->
            if (!enabled) {
                viewModel.onReminderEnabled(false)
            } else if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                context.getSystemService(AlarmManager::class.java)?.canScheduleExactAlarms() != true
            ) {
                viewModel.onReminderEnabled(false)
                viewModel.onReminderDenied(R.string.habit_reminder_exact_alarm_denied)
            } else if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                viewModel.onReminderEnabled(true)
            }
        },
        onReminderTimeChanged = viewModel::onReminderTimeChanged,
        onDayToggled = viewModel::onDayToggled,
        onPublicChanged = viewModel::onPublicChanged,
        onSaveClicked = viewModel::onSaveClicked,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddContentContent(
    uiState: AddContentUiState,
    onBack: () -> Unit,
    onNameChanged: (String) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onIconSelected: (String) -> Unit,
    onColorSelected: (Color) -> Unit,
    onReminderEnabledChange: (Boolean) -> Unit,
    onReminderTimeChanged: (String) -> Unit,
    onDayToggled: (DayOfWeek) -> Unit,
    onPublicChanged: (Boolean) -> Unit,
    onSaveClicked: () -> Unit,
) {
    val selectedColor = uiState.color
    val canSave = uiState.name.isNotBlank() && uiState.selectedDays.isNotEmpty() && !uiState.isSaving
    var showTimePicker by remember { mutableStateOf(false) }

    if (showTimePicker) {
        val parts = uiState.reminderTime.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 8
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
        val timePickerState = rememberTimePickerState(
            initialHour = hour,
            initialMinute = minute,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        onReminderTimeChanged(
                            "%02d:%02d".format(timePickerState.hour, timePickerState.minute),
                        )
                        showTimePicker = false
                    },
                ) {
                    Text(stringResource(R.string.add_habit_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(stringResource(R.string.add_habit_cancel))
                }
            },
            text = { TimePicker(state = timePickerState) },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    HabitPrimary,
                )
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.25f))
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.add_habit_close),
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Text(
                    text = stringResource(R.string.add_habit_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(selectedColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center,
                ) {
                    HabitLineIcon(
                        iconKey = uiState.icon,
                        tint = selectedColor,
                        size = 52.dp,
                    )
                }
            }

            CustomTextField(
                label = stringResource(R.string.add_habit_name_label),
                value = uiState.name,
                onValueChange = onNameChanged,
                placeholder = stringResource(R.string.add_habit_name_placeholder),
            )

            CustomTextField(
                label = stringResource(R.string.add_habit_description_label),
                value = uiState.description,
                onValueChange = onDescriptionChanged,
                placeholder = stringResource(R.string.add_habit_description_placeholder),
            )

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.add_habit_choose_icon),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                HabitIconOptions.chunked(6).forEach { rowIcons ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        rowIcons.forEach { icon ->
                            val selected = uiState.icon == icon
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(
                                        if (selected) selectedColor.copy(alpha = 0.12f) else HabitField,
                                    )
                                    .border(
                                        width = 2.dp,
                                        color = if (selected) selectedColor else Color.Transparent,
                                        shape = RoundedCornerShape(18.dp),
                                    )
                                    .clickable { onIconSelected(icon) },
                                contentAlignment = Alignment.Center,
                            ) {
                                HabitLineIcon(
                                    iconKey = icon,
                                    tint = if (selected) selectedColor else HabitOnSurfaceVariant,
                                    size = 24.dp,
                                )
                            }
                        }
                        repeat(6 - rowIcons.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.add_habit_choose_color),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                HabitColorOptions.chunked(6).forEach { rowColors ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        rowColors.forEach { color ->
                            val selected = uiState.color == color
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.Center,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .shadow(if (selected) 6.dp else 3.dp, CircleShape)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(
                                            width = if (selected) 3.dp else 0.dp,
                                            color = Color.White,
                                            shape = CircleShape,
                                        )
                                        .clickable { onColorSelected(color) },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (selected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                }
                            }
                        }
                        repeat(6 - rowColors.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            ReminderToggleRow(
                enabled = uiState.reminderEnabled,
                reminderTime = uiState.reminderTime,
                onEnabledChange = onReminderEnabledChange,
                onTimeClick = { showTimePicker = true },
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.add_habit_frequency),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    DayOfWeek.entries.forEach { day ->
                        val selected = uiState.selectedDays.contains(day)
                        val label = day.getDisplayName(TextStyle.NARROW, LocalLocale.current.platformLocale)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selected) HabitPrimary else HabitField)
                                .clickable { onDayToggled(day) }
                                .padding(horizontal = 9.dp, vertical = 9.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (selected) Color.White else HabitOnSurface,
                            )
                        }
                    }
                }
            }

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
                    text = stringResource(R.string.habit_is_public),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = HabitOnSurface,
                )
                Switch(
                    checked = uiState.isPublic,
                    onCheckedChange = onPublicChanged,
                )
            }

            uiState.errorMessageRes?.let { messageRes ->
                Text(
                    text = stringResource(messageRes),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .shadow(if (canSave) 6.dp else 2.dp, RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (canSave) {
                            Brush.linearGradient(
                                listOf(selectedColor, selectedColor.copy(alpha = 0.7f)),
                            )
                        } else {
                            Brush.linearGradient(listOf(Color(0xFFCAC4D0), Color(0xFFCAC4D0)))
                        },
                    )
                    .clickable(enabled = canSave, onClick = onSaveClicked),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.add_habit_create),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun AddContentContentPreview() {
    AddContentContent(
        uiState = AddContentUiState(),
        onBack = {},
        onNameChanged = {},
        onDescriptionChanged = {},
        onIconSelected = {},
        onColorSelected = {},
        onReminderEnabledChange = {},
        onReminderTimeChanged = {},
        onDayToggled = {},
        onPublicChanged = {},
        onSaveClicked = {},
    )
}
