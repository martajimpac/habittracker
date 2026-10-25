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
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Fort
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
import com.marta.habittracker.domain.model.HabitSchedule
import com.marta.habittracker.presentation.components.CustomTextField
import com.marta.habittracker.presentation.components.DaysPerWeekStepper
import com.marta.habittracker.presentation.components.HabitButton
import com.marta.habittracker.presentation.components.HabitButtonVariant
import com.marta.habittracker.presentation.components.HabitDialog
import com.marta.habittracker.presentation.components.HabitIconOptions
import com.marta.habittracker.presentation.components.HabitLineIcon
import com.marta.habittracker.presentation.components.ReminderToggleRow
import com.marta.habittracker.presentation.theme.HabitField
import com.marta.habittracker.presentation.theme.HabitOnSurface
import com.marta.habittracker.presentation.theme.HabitOnSurfaceVariant
import com.marta.habittracker.presentation.utils.ObserveAsEvents
import com.marta.habittracker.presentation.components.HabitIconButton
import com.marta.habittracker.presentation.theme.HabitTopBarHorizontalPadding
import com.marta.habittracker.presentation.utils.HabitColorOptions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch

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
        onDaysPerWeekSelected = viewModel::onDaysPerWeekSelected,
        onScheduleSelected = viewModel::onScheduleSelected,
        onWeekdayToggled = viewModel::onWeekdayToggled,
        onPublicChanged = viewModel::onPublicChanged,
        onSaveClicked = viewModel::onSaveClicked,
        focusField = viewModel.focusField,
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
    onDaysPerWeekSelected: (Int) -> Unit,
    onScheduleSelected: (HabitSchedule) -> Unit,
    onWeekdayToggled: (kotlinx.datetime.DayOfWeek) -> Unit,
    onPublicChanged: (Boolean) -> Unit,
    onSaveClicked: () -> Unit,
    focusField: Flow<AddContentField> = emptyFlow(),
) {
    val selectedColor = uiState.color
    val canSave = uiState.name.isNotBlank() && !uiState.isSaving
    val nameMissing = uiState.errorMessageRes == R.string.add_habit_error_name_required
    val reminderInvalid = uiState.errorMessageRes == R.string.add_habit_error_reminder_format
    val nameFocus = remember { FocusRequester() }
    val daysFocus = remember { FocusRequester() }
    val reminderFocus = remember { FocusRequester() }
    val nameBringIntoView = remember { BringIntoViewRequester() }
    val daysBringIntoView = remember { BringIntoViewRequester() }
    val reminderBringIntoView = remember { BringIntoViewRequester() }
    val focusScope = rememberCoroutineScope()

    ObserveAsEvents(flow = focusField) { field ->
        focusScope.launch {
            when (field) {
                AddContentField.Name -> {
                    nameBringIntoView.bringIntoView()
                    nameFocus.requestFocus()
                }
                AddContentField.Days -> {
                    daysBringIntoView.bringIntoView()
                    daysFocus.requestFocus()
                }
                AddContentField.Reminder -> {
                    reminderBringIntoView.bringIntoView()
                    reminderFocus.requestFocus()
                }
            }
        }
    }
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
        HabitDialog(
            onDismissRequest = { showTimePicker = false },
            confirmText = stringResource(R.string.add_habit_save),
            dismissText = stringResource(R.string.add_habit_cancel),
            onConfirm = {
                onReminderTimeChanged(
                    "%02d:%02d".format(timePickerState.hour, timePickerState.minute),
                )
                showTimePicker = false
            },
            content = { TimePicker(state = timePickerState) },
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
                .background(MaterialTheme.colorScheme.primary)
                .statusBarsPadding()
                .padding(horizontal = HabitTopBarHorizontalPadding, vertical = 16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(
                        if (uiState.isEditing) R.string.detail_edit_habit else R.string.add_habit_title,
                    ),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.weight(1f),
                )
                HabitIconButton(
                    onClick = onBack,
                    icon = Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.add_habit_close),
                    backgroundColor = MaterialTheme.colorScheme.primaryContainer,
                    iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
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
                modifier = Modifier
                    .focusRequester(nameFocus)
                    .bringIntoViewRequester(nameBringIntoView),
                label = stringResource(R.string.add_habit_name_label),
                value = uiState.name,
                onValueChange = onNameChanged,
                placeholder = stringResource(R.string.add_habit_name_placeholder),
                isError = nameMissing,
                supportingText = if (nameMissing) {
                    stringResource(R.string.add_habit_error_name_required)
                } else {
                    ""
                },
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
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            shape = CircleShape,
                                        )
                                        .clickable { onColorSelected(color) },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (selected) {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimary,
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

            Column(
                modifier = Modifier
                    .focusRequester(reminderFocus)
                    .focusable()
                    .bringIntoViewRequester(reminderBringIntoView)
                    .then(
                        if (reminderInvalid) {
                            Modifier.border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.error,
                                shape = RoundedCornerShape(16.dp),
                            )
                        } else {
                            Modifier
                        },
                    )
                    .padding(if (reminderInvalid) 8.dp else 0.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ReminderToggleRow(
                    enabled = uiState.reminderEnabled,
                    reminderTime = uiState.reminderTime,
                    onEnabledChange = onReminderEnabledChange,
                    onTimeClick = { showTimePicker = true },
                )
                if (reminderInvalid) {
                    Text(
                        text = stringResource(R.string.add_habit_error_reminder_format),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            Column(
                modifier = Modifier
                    .focusRequester(daysFocus)
                    .focusable()
                    .bringIntoViewRequester(daysBringIntoView),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
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
                    listOf(
                        HabitSchedule.DAILY to R.string.frequency_daily,
                        HabitSchedule.WEEKDAYS to R.string.frequency_weekdays,
                        HabitSchedule.WEEKLY_COUNT to R.string.frequency_weekly_count,
                    ).forEach { (mode, label) ->
                        val selected = uiState.schedule == mode
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selected) MaterialTheme.colorScheme.primary else HabitField)
                                .clickable { onScheduleSelected(mode) }
                                .padding(horizontal = 9.dp, vertical = 9.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = stringResource(label),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (selected) MaterialTheme.colorScheme.onPrimary else HabitOnSurface,
                            )
                        }
                    }
                }
                if (uiState.schedule == HabitSchedule.WEEKDAYS) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        kotlinx.datetime.DayOfWeek.entries.forEach { day ->
                            val selected = day in uiState.selectedDays
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (selected) MaterialTheme.colorScheme.primary else HabitField)
                                    .clickable { onWeekdayToggled(day) }
                                    .padding(horizontal = 12.dp, vertical = 9.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = stringResource(weekdayLabel(day)),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimary else HabitOnSurface,
                                )
                            }
                        }
                    }
                }
                if (uiState.schedule == HabitSchedule.WEEKLY_COUNT) {
                    DaysPerWeekStepper(
                        days = uiState.daysPerWeek,
                        onDaysChange = onDaysPerWeekSelected,
                    )
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

            val otherError = uiState.errorMessageRes?.takeUnless {
                it == R.string.add_habit_error_name_required ||
                    it == R.string.add_habit_error_days_required ||
                    it == R.string.add_habit_error_reminder_format
            }
            otherError?.let { messageRes ->
                Text(
                    text = stringResource(messageRes),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }

            HabitButton(
                text = stringResource(
                    if (uiState.isEditing) R.string.add_habit_save else R.string.add_habit_create,
                ),
                onClick = onSaveClicked,
                enabled = canSave,
                clickableWhenDisabled = !uiState.isSaving,
            )

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

private fun weekdayLabel(day: kotlinx.datetime.DayOfWeek): Int = when (day) {
    kotlinx.datetime.DayOfWeek.MONDAY -> R.string.activity_day_mon
    kotlinx.datetime.DayOfWeek.TUESDAY -> R.string.activity_day_tue
    kotlinx.datetime.DayOfWeek.WEDNESDAY -> R.string.activity_day_wed
    kotlinx.datetime.DayOfWeek.THURSDAY -> R.string.activity_day_thu
    kotlinx.datetime.DayOfWeek.FRIDAY -> R.string.activity_day_fri
    kotlinx.datetime.DayOfWeek.SATURDAY -> R.string.activity_day_sat
    kotlinx.datetime.DayOfWeek.SUNDAY -> R.string.activity_day_sun
}

// @Preview(showBackground = true, showSystemUi = true)
// @Composable
// private fun AddContentContentPreview() {
    // AddContentContent(
        // uiState = AddContentUiState(),
        // onBack = {},
        // onNameChanged = {},
        // onDescriptionChanged = {},
        // onIconSelected = {},
        // onColorSelected = {},
        // onReminderEnabledChange = {},
        // onReminderTimeChanged = {},
        // onDaysPerWeekSelected = {},
        // onPublicChanged = {},
        // onSaveClicked = {},
    // )
// }
