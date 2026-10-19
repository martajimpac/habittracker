package com.marta.habittracker.presentation.screens.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.marta.habittracker.R
import com.marta.habittracker.domain.model.Habit
import com.marta.habittracker.domain.model.HabitRecord
import com.marta.habittracker.core.toJava
import com.marta.habittracker.presentation.components.HabitButton
import com.marta.habittracker.presentation.components.HabitButtonVariant
import com.marta.habittracker.presentation.components.HabitIconButton
import com.marta.habittracker.presentation.components.HabitLineIcon
import com.marta.habittracker.presentation.components.MonthCalendar
import com.marta.habittracker.presentation.screens.home.calculateStreak
import com.marta.habittracker.presentation.theme.HabitOnSurface
import com.marta.habittracker.presentation.theme.HabitOutline
import com.marta.habittracker.presentation.utils.ObserveAsEvents
import com.marta.habittracker.presentation.utils.parseHabitColor
import kotlinx.datetime.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun HabitDetailScreen(
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    viewModel: HabitDetailViewModel = hiltViewModel(),
) {
    val habit by viewModel.habit.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val completionRate by viewModel.completionPercentage.collectAsStateWithLifecycle()
    var showDeleteConfirm by remember { mutableStateOf(false) }

    ObserveAsEvents(flow = viewModel.habitDeleted) {
        onBack()
    }

    HabitDetailContent(
        habit = habit,
        records = history,
        completionRate = completionRate,
        onBack = onBack,
        onEdit = { habit?.id?.let(onEdit) },
        onDelete = { showDeleteConfirm = true },
    )

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.detail_delete_title)) },
            text = { Text(stringResource(R.string.detail_delete_message)) },
            confirmButton = {
                HabitButton(
                    text = stringResource(R.string.detail_delete_habit),
                    onClick = {
                        showDeleteConfirm = false
                        viewModel.deleteHabit()
                    },
                    fillMaxWidth = false,
                    compact = true,
                )
            },
            dismissButton = {
                HabitButton(
                    text = stringResource(R.string.add_habit_cancel),
                    onClick = { showDeleteConfirm = false },
                    variant = HabitButtonVariant.Secondary,
                    fillMaxWidth = false,
                    compact = true,
                )
            },
        )
    }
}

@Composable
fun HabitDetailContent(
    habit: Habit?,
    records: List<HabitRecord>,
    completionRate: Float,
    onBack: () -> Unit,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {},
) {
    val habitName = habit?.name ?: stringResource(R.string.detail_loading)
    val iconKey = habit?.icon
    val accent = parseHabitColor(habit?.colorHex.orEmpty())
    val reminder = habit?.reminderTime?.takeIf { it.isNotBlank() }
        ?: stringResource(R.string.home_all_day)
    val actionsEnabled = habit != null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            HabitIconButton(
                onClick = onBack,
                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = stringResource(R.string.detail_back),
            )
            Spacer(modifier = Modifier.weight(1f))
            HabitIconButton(
                onClick = onEdit,
                icon = Icons.Rounded.Edit,
                contentDescription = stringResource(R.string.detail_edit_habit),
                enabled = actionsEnabled,
            )
            HabitIconButton(
                onClick = onDelete,
                icon = Icons.Rounded.Delete,
                contentDescription = stringResource(R.string.detail_delete_habit),
                enabled = actionsEnabled,
                iconTint = MaterialTheme.colorScheme.error,
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
        ) {
            DetailOverviewCard(
                habit = habit,
                records = records,
                habitName = habitName,
                iconKey = iconKey,
                accent = accent,
                reminder = reminder,
            )

            DetailChartsCard(
                habit = habit,
                records = records,
                accent = accent,
                completionRate = completionRate,
            )

            DetailCalendar(records = records, accent = accent)
        }
    }
}

private val DetailCardShape = RoundedCornerShape(20.dp)

@Composable
private fun DetailOverviewCard(
    habit: Habit?,
    records: List<HabitRecord>,
    habitName: String,
    iconKey: String?,
    accent: Color,
    reminder: String,
) {
    val streak = if (habit == null) 0 else calculateStreak(records)
    val description = habit?.description?.takeIf { it.isNotBlank() }
    Column(
        modifier = Modifier
            .padding(top = 8.dp)
            .detailCard(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                HabitLineIcon(
                    iconKey = iconKey,
                    tint = accent,
                    size = 28.dp,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = habitName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = reminder,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (streak > 0) {
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.LocalFireDepartment,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = pluralStringResource(R.plurals.stats_habit_row_streak, streak, streak),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = accent,
                        )
                    }
                }
            }
        }
        if (description != null) {
            Text(
                text = description,
                modifier = Modifier.padding(top = 16.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (habit != null) {
            Text(
                text = stringResource(R.string.detail_scheduled_days),
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            ScheduledDays(days = habit.daysOfWeek, accent = accent)
        }
    }
}

@Composable
private fun ScheduledDays(
    days: Set<DayOfWeek>,
    accent: Color,
) {
    val labels = listOf(
        DayOfWeek.MONDAY to stringResource(R.string.activity_day_mon),
        DayOfWeek.TUESDAY to stringResource(R.string.activity_day_tue),
        DayOfWeek.WEDNESDAY to stringResource(R.string.activity_day_wed),
        DayOfWeek.THURSDAY to stringResource(R.string.activity_day_thu),
        DayOfWeek.FRIDAY to stringResource(R.string.activity_day_fri),
        DayOfWeek.SATURDAY to stringResource(R.string.activity_day_sat),
        DayOfWeek.SUNDAY to stringResource(R.string.activity_day_sun),
    )
    val onAccent = if (accent.luminance() > 0.6f) HabitOnSurface else Color.White
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        labels.forEach { (day, label) ->
            val selected = day in days
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (selected) accent else MaterialTheme.colorScheme.surfaceContainerHighest,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selected) onAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun DetailChartsCard(
    habit: Habit?,
    records: List<HabitRecord>,
    accent: Color,
    completionRate: Float,
) {
    Column(
        modifier = Modifier
            .padding(top = 12.dp)
            .detailCard(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.detail_overall_performance),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(20.dp))
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { completionRate / 100f },
                modifier = Modifier.size(180.dp),
                strokeWidth = 16.dp,
                color = accent,
                trackColor = accent.copy(alpha = 0.2f),
                strokeCap = StrokeCap.Round,
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${completionRate.toInt()}%",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = accent,
                )
                Text(
                    text = stringResource(R.string.detail_completed),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (habit != null) {
            Spacer(modifier = Modifier.height(28.dp))
            LastFourWeeksChart(
                habit = habit,
                records = records,
                accent = accent,
            )
            Spacer(modifier = Modifier.height(28.dp))
            WeekdayCompletionsChart(
                records = records,
                accent = accent,
            )
        }
    }
}

@Composable
private fun LastFourWeeksChart(
    habit: Habit,
    records: List<HabitRecord>,
    accent: Color,
) {
    val today = remember { LocalDate.now() }
    val weeks = remember(habit.daysOfWeek, habit.createdAt, records, today) {
        recentWeekCompletions(
            daysOfWeek = habit.daysOfWeek,
            records = records,
            createdOn = habit.createdAt.atZone(ZoneId.systemDefault()).toLocalDate(),
            today = today,
        )
    }
    val formatter = remember {
        DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
    }
    Text(
        text = stringResource(R.string.detail_last_four_weeks),
        modifier = Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
    )
    Spacer(modifier = Modifier.height(16.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        weeks.forEach { week ->
            val label = week.weekStart.format(formatter)
            val percent = if (week.scheduled == 0) {
                stringResource(R.string.detail_week_empty)
            } else {
                "${(week.rate * 100).toInt()}%"
            }
            val description = stringResource(
                R.string.detail_week_bar,
                label,
                if (week.scheduled == 0) 0 else (week.rate * 100).toInt(),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .semantics { contentDescription = description },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = percent,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = accent,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(88.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(accent.copy(alpha = 0.15f)),
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .fillMaxHeight(week.rate.coerceIn(0f, 1f))
                            .background(accent),
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun WeekdayCompletionsChart(
    records: List<HabitRecord>,
    accent: Color,
) {
    val today = remember { LocalDate.now() }
    val days = remember(records, today) { weekdayCompletions(records, today) }
    val maxCount = days.maxOf { it.count }.coerceAtLeast(1)
    val labels = listOf(
        stringResource(R.string.activity_day_mon),
        stringResource(R.string.activity_day_tue),
        stringResource(R.string.activity_day_wed),
        stringResource(R.string.activity_day_thu),
        stringResource(R.string.activity_day_fri),
        stringResource(R.string.activity_day_sat),
        stringResource(R.string.activity_day_sun),
    )
    Text(
        text = stringResource(R.string.detail_weekday_chart),
        modifier = Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
    )
    Spacer(modifier = Modifier.height(16.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        days.forEachIndexed { index, day ->
            val fraction = day.count.toFloat() / maxCount
            val dayName = day.day.getDisplayName(TextStyle.FULL_STANDALONE, Locale.getDefault())
            val description = pluralStringResource(
                R.plurals.detail_weekday_bar,
                day.count,
                dayName,
                day.count,
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .semantics { contentDescription = description },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = day.count.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = accent,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(88.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(accent.copy(alpha = 0.15f)),
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .fillMaxHeight(fraction.coerceIn(0f, 1f))
                            .background(accent),
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = labels[index],
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun DetailCalendar(
    records: List<HabitRecord>,
    accent: Color,
) {
    val today = remember { LocalDate.now() }
    var month by remember { mutableStateOf(YearMonth.from(today)) }
    val completedDates = remember(records) {
        records
            .filter { it.isCompleted }
            .map { it.date.toJava() }
            .toSet()
    }

    MonthCalendar(
        month = month,
        completedDates = completedDates,
        accentColor = accent,
        today = today,
        onPreviousMonth = { month = month.minusMonths(1) },
        onNextMonth = { month = month.plusMonths(1) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
    )
}

@Composable
private fun Modifier.detailCard(): Modifier = this
    .fillMaxWidth()
    .padding(horizontal = 20.dp)
    .clip(DetailCardShape)
    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
    .border(1.dp, HabitOutline, DetailCardShape)
    .padding(20.dp)

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun HabitDetailContentPreview() {
    HabitDetailContent(
        habit = null,
        records = emptyList(),
        completionRate = 75f,
        onBack = {},
    )
}
