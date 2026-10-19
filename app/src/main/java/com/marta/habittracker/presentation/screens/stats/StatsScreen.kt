package com.marta.habittracker.presentation.screens.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.marta.habittracker.R
import com.marta.habittracker.domain.model.Habit
import com.marta.habittracker.presentation.components.ActivityHeatmap
import com.marta.habittracker.presentation.components.HabitLineIcon
import com.marta.habittracker.presentation.components.buildRollingFourWeeks
import com.marta.habittracker.presentation.components.globalDayIntensity
import com.marta.habittracker.presentation.components.habitDayIntensity
import com.marta.habittracker.presentation.components.toHeatmapWeeks
import com.marta.habittracker.domain.usecase.StatsStreakHighlight
import com.marta.habittracker.domain.usecase.statsStreakCopyVariant
import com.marta.habittracker.domain.usecase.statsStreakHighlight
import com.marta.habittracker.presentation.screens.home.calculateStreak
import com.marta.habittracker.presentation.screens.home.habitTimeLabel
import com.marta.habittracker.presentation.theme.HabitColor2
import com.marta.habittracker.presentation.theme.HabitPrimary
import com.marta.habittracker.presentation.utils.habitAccentColor
import java.time.LocalDate

@Composable
fun StatsScreen(
    onHabitClick: (String) -> Unit,
    viewModel: StatsViewModel = hiltViewModel(),
) {
    val habits by viewModel.habits.collectAsStateWithLifecycle()

    StatsContent(
        habits = habits,
        onHabitClick = onHabitClick,
    )
}

@Composable
fun StatsContent(
    habits: List<Habit>,
    onHabitClick: (String) -> Unit = {},
) {
    val today = remember { LocalDate.now() }
    val streakHighlight = remember(habits, today) { statsStreakHighlight(habits, today) }
    val activityWeeks = remember(habits, today) {
        toHeatmapWeeks(buildRollingFourWeeks(today)) { date ->
            globalDayIntensity(habits, date)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp)
                .heightIn(min = 44.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.stats_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        if (habits.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.stats_empty),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                streakHighlight?.let { highlight ->
                    item {
                        StatsStreakCard(highlight = highlight, today = today)
                    }
                }
                item {
                    ActivityHeatmap(
                        weeks = activityWeeks,
                        today = today,
                        filledColor = HabitPrimary,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                items(habits, key = { it.id }) { habit ->
                    StatsHabitRow(
                        habit = habit,
                        today = today,
                        onClick = { onHabitClick(habit.id) },
                    )
                }
            }
        }
    }
}

private val allHabitsRecordCopy = intArrayOf(
    R.plurals.stats_all_habits_record_1,
    R.plurals.stats_all_habits_record_2,
    R.plurals.stats_all_habits_record_3,
)

private val allHabitsCurrentCopy = intArrayOf(
    R.plurals.stats_all_habits_current_1,
    R.plurals.stats_all_habits_current_2,
    R.plurals.stats_all_habits_current_3,
)

private val habitStreakCopy = intArrayOf(
    R.plurals.stats_habit_streak_1,
    R.plurals.stats_habit_streak_2,
    R.plurals.stats_habit_streak_3,
    R.plurals.stats_habit_streak_4,
)

@Composable
private fun StatsStreakCard(
    highlight: StatsStreakHighlight,
    today: LocalDate,
) {
    val showFire = highlight is StatsStreakHighlight.AllHabits
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .border(1.dp, HabitPrimary.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (showFire) {
            Icon(
                imageVector = Icons.Rounded.LocalFireDepartment,
                contentDescription = null,
                tint = HabitColor2,
                modifier = Modifier.size(22.dp),
            )
        }
        Text(
            text = streakHighlightText(highlight, today),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun streakHighlightText(
    highlight: StatsStreakHighlight,
    today: LocalDate,
) = when (highlight) {
    is StatsStreakHighlight.AllHabits -> {
        val variants = if (highlight.isLongestSoFar) allHabitsRecordCopy else allHabitsCurrentCopy
        val variant = statsStreakCopyVariant(highlight.days, today.dayOfYear, variants.size)
        buildAnnotatedString {
            append(pluralStringResource(variants[variant], highlight.days, highlight.days))
        }
    }
    is StatsStreakHighlight.Habit -> {
        val variant = statsStreakCopyVariant(highlight.days, today.dayOfYear, habitStreakCopy.size)
        val habitName = highlight.habitName
        val raw = pluralStringResource(habitStreakCopy[variant], highlight.days, habitName, highlight.days)
        val nameStart = raw.indexOf(habitName)
        buildAnnotatedString {
            if (nameStart < 0) {
                append(raw)
            } else {
                append(raw.substring(0, nameStart))
                withStyle(SpanStyle(fontWeight = FontWeight.Black)) {
                    append(habitName)
                }
                append(raw.substring(nameStart + habitName.length))
            }
        }
    }
}

@Composable
private fun StatsHabitRow(
    habit: Habit,
    today: LocalDate,
    onClick: () -> Unit,
) {
    val accent = habitAccentColor(habit)
    val streak = calculateStreak(habit)
    val weeks = remember(habit.id, habit.records, today) {
        toHeatmapWeeks(buildRollingFourWeeks(today)) { date ->
            habitDayIntensity(habit, date)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .border(1.dp, HabitPrimary.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        StatsHabitSummary(
            habit = habit,
            accent = accent,
            streak = streak,
            modifier = Modifier.weight(1f),
        )
        ActivityHeatmap(
            weeks = weeks,
            today = today,
            filledColor = accent,
            showChrome = false,
            cellSize = 14.dp,
        )
    }
}

@Composable
private fun StatsHabitSummary(
    habit: Habit,
    accent: Color,
    streak: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(accent.copy(alpha = 0.09f)),
            contentAlignment = Alignment.Center,
        ) {
            HabitLineIcon(
                habit = habit,
                tint = accent,
                size = 22.dp,
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = habit.name,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (streak > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(top = 2.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.LocalFireDepartment,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = pluralStringResource(R.plurals.stats_habit_row_streak, streak, streak),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = accent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Text(
                text = habitTimeLabel(habit),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun StatsContentPreview() {
    StatsContent(habits = emptyList())
}
