package com.marta.habittracker.presentation.screens.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
import com.marta.habittracker.presentation.components.toHeatmapWeeks
import com.marta.habittracker.domain.usecase.habitStatsInsight
import com.marta.habittracker.presentation.screens.home.calculateStreak
import com.marta.habittracker.presentation.screens.home.habitTimeLabel
import com.marta.habittracker.presentation.theme.HabitOnSurface
import com.marta.habittracker.presentation.theme.HabitOnSurfaceVariant
import com.marta.habittracker.presentation.theme.HabitPrimary
import com.marta.habittracker.presentation.utils.habitAccentColor
import java.time.DayOfWeek
import java.time.LocalDate

@Composable
fun StatsScreen(
    viewModel: StatsViewModel = hiltViewModel(),
) {
    val habits by viewModel.habits.collectAsStateWithLifecycle()

    StatsContent(habits = habits)
}

@Composable
fun StatsContent(
    habits: List<Habit>,
) {
    val today = remember { LocalDate.now() }
    val insight = remember(habits, today) { habitStatsInsight(habits, today) }
    val activityWeeks = remember(habits, today) {
        toHeatmapWeeks(buildRollingFourWeeks(today)) { date ->
            globalDayIntensity(habits, date)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding(),
    ) {
        Text(
            text = stringResource(R.string.stats_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
        )

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
                item {
                    StatsInsightCard(
                        text = insight.strongestWeekday?.let { day ->
                            stringResource(
                                R.string.stats_strongest_day,
                                stringResource(weekdayLabelRes(day)),
                                insight.strongestWeekdayCount,
                            )
                        } ?: stringResource(R.string.stats_strongest_day_empty),
                    )
                }
                item {
                    val atRisk = insight.streakAtRisk
                    StatsInsightCard(
                        text = if (atRisk == null) {
                            stringResource(R.string.stats_streak_risk_empty)
                        } else {
                            stringResource(
                                R.string.stats_streak_risk,
                                atRisk.habitName,
                                atRisk.streakDays,
                            )
                        },
                    )
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
                    StatsHabitRow(habit = habit)
                }
            }
        }
    }
}

@Composable
private fun StatsInsightCard(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.SemiBold,
        color = HabitOnSurface,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(androidx.compose.ui.graphics.Color.White)
            .border(1.dp, HabitPrimary.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
            .padding(16.dp),
    )
}

private fun weekdayLabelRes(dayOfWeek: DayOfWeek): Int = when (dayOfWeek) {
    DayOfWeek.MONDAY -> R.string.stats_weekday_monday
    DayOfWeek.TUESDAY -> R.string.stats_weekday_tuesday
    DayOfWeek.WEDNESDAY -> R.string.stats_weekday_wednesday
    DayOfWeek.THURSDAY -> R.string.stats_weekday_thursday
    DayOfWeek.FRIDAY -> R.string.stats_weekday_friday
    DayOfWeek.SATURDAY -> R.string.stats_weekday_saturday
    DayOfWeek.SUNDAY -> R.string.stats_weekday_sunday
}

@Composable
private fun StatsHabitRow(habit: Habit) {
    val accent = habitAccentColor(habit)
    val streak = calculateStreak(habit.records)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(androidx.compose.ui.graphics.Color.White)
            .border(1.dp, HabitPrimary.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(accent.copy(alpha = 0.09f)),
            contentAlignment = Alignment.Center,
        ) {
            HabitLineIcon(
                habit = habit,
                tint = accent,
                size = 24.dp,
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = habit.name,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = HabitOnSurface,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(top = 2.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.LocalFireDepartment,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(12.dp),
                )
                Text(
                    text = stringResource(R.string.home_streak_days, streak),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = accent,
                )
                Text(
                    text = stringResource(R.string.home_time_separator, habitTimeLabel(habit)),
                    style = MaterialTheme.typography.labelSmall,
                    color = HabitOnSurfaceVariant,
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun StatsContentPreview() {
    StatsContent(habits = emptyList())
}
