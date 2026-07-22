package com.marta.habittracker.view.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.marta.habittracker.R
import com.marta.habittracker.domain.model.Habit
import com.marta.habittracker.ui.theme.HabitField
import com.marta.habittracker.ui.theme.HabitOnSurface
import com.marta.habittracker.ui.theme.HabitOnSurfaceVariant
import com.marta.habittracker.ui.theme.HabitOutline
import com.marta.habittracker.ui.theme.HabitPrimary
import com.marta.habittracker.ui.theme.HabitPrimaryLight
import com.marta.habittracker.ui.theme.HabitSurface
import com.marta.habittracker.ui.theme.HabitTermsBg
import com.marta.habittracker.ui.theme.HabitTermsText
import com.marta.habittracker.view.core.components.HabitFab
import com.marta.habittracker.view.core.components.ProgressRing
import com.marta.habittracker.view.utils.dayShortLabel
import com.marta.habittracker.view.utils.greetingForHour
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun HomeScreen(
    onAdd: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val habits by viewModel.habits.collectAsStateWithLifecycle()
    val allHabits by viewModel.allHabits.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val userDisplayName by viewModel.userDisplayName.collectAsStateWithLifecycle()

    val today = viewModel.today
    val isToday = selectedDate == today
    val isPast = selectedDate.isBefore(today)
    val isFuture = selectedDate.isAfter(today)

    val (completedCount, totalCount) = remember(habits, selectedDate) {
        completionStats(habits, selectedDate)
    }
    val pct = completionPercent(completedCount, totalCount)
    val greeting = remember { greetingForHour(java.time.LocalTime.now().hour) }
    val dateLabel = remember(selectedDate) {
        selectedDate.format(DateTimeFormatter.ofPattern("MMMM d", Locale.ENGLISH))
    }
    val dayLabel = remember(selectedDate) {
        selectedDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
    }
    val overallStreak = remember(allHabits) {
        allHabits.maxOfOrNull { calculateStreak(it.records) } ?: 0
    }

    Scaffold(
        topBar = {
            HomeHeader(
                selectedDate = selectedDate,
                isToday = isToday,
                isPast = isPast,
                dateLabel = dateLabel,
                dayLabel = dayLabel,
                greeting = greeting,
                userDisplayName = userDisplayName,
                weekDays = viewModel.weekDays,
                allHabits = allHabits,
                onDateSelected = viewModel::onDateSelected,
            )
        },
        floatingActionButton = {
            if (isToday) {
                HabitFab(
                    onClick = onAdd,
                    icon = Icons.Default.Add,
                    contentDescription = stringResource(R.string.home_add_habit),
                )
            }
        },
        containerColor = HabitSurface,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 16.dp,
                bottom = 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
                item {
                    HomeProgressCard(
                        pct = pct,
                        completedCount = completedCount,
                        totalCount = totalCount,
                        isToday = isToday,
                        dayLabel = dayShortLabel(selectedDate),
                        habits = habits,
                        selectedDate = selectedDate,
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (isToday) {
                                stringResource(R.string.home_todays_habits)
                            } else {
                                stringResource(R.string.home_day_habits, dayShortLabel(selectedDate))
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = HabitOnSurface,
                        )
                        Text(
                            text = stringResource(R.string.home_total_count, totalCount),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = HabitOnSurfaceVariant,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(HabitField)
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                        )
                    }
                }

                if (habits.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.home_empty_habits),
                            style = MaterialTheme.typography.bodyMedium,
                            color = HabitOnSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                        )
                    }
                } else {
                    items(habits, key = { it.id }) { habit ->
                        HomeHabitCard(
                            habit = habit,
                            isDone = habit.isCompleted,
                            isToday = isToday,
                            onToggle = { viewModel.toggleComplete(habit) },
                        )
                    }
                }

                item {
                    when {
                        isToday -> HomeMotivationCard(overallStreak = overallStreak)
                        else -> HomeDayInfoCard(
                            pct = pct,
                            completedCount = completedCount,
                            totalCount = totalCount,
                            isPast = isPast,
                            isFuture = isFuture,
                        )
                    }
                }
        }
    }
}

@Composable
private fun HomeHeader(
    selectedDate: LocalDate,
    isToday: Boolean,
    isPast: Boolean,
    dateLabel: String,
    dayLabel: String,
    greeting: String,
    userDisplayName: String,
    weekDays: List<LocalDate>,
    allHabits: List<Habit>,
    onDateSelected: (LocalDate) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.linearGradient(listOf(HabitPrimary, HabitPrimaryLight)))
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp, bottom = 20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when {
                        isToday -> stringResource(R.string.home_today_label, dateLabel)
                        isPast -> stringResource(R.string.home_past_label, dateLabel)
                        else -> stringResource(R.string.home_upcoming_label, dateLabel)
                    },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.75f),
                )
                Text(
                    text = if (isToday) {
                        stringResource(R.string.home_greeting, greeting, firstName(userDisplayName))
                    } else {
                        dayLabel
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                )
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = userAvatarInitials(userDisplayName),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            weekDays.forEach { date ->
                val active = date == selectedDate
                val dayHabits = habitsForDate(allHabits, date)
                val dayCompleted = dayHabits.count { isHabitCompletedOnDate(it, date) }
                val dayPct = completionPercent(dayCompleted, dayHabits.size)

                WeekDayChip(
                    label = dayShortLabel(date),
                    dayNumber = date.dayOfMonth,
                    active = active,
                    dayPct = dayPct,
                    onClick = { onDateSelected(date) },
                )
            }
        }
    }
}

@Composable
private fun RowScope.WeekDayChip(
    label: String,
    dayNumber: Int,
    active: Boolean,
    dayPct: Int,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(if (active) Color.White else Color.White.copy(alpha = 0.15f))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            color = if (active) HabitPrimary else Color.White.copy(alpha = 0.7f),
        )
        Text(
            text = dayNumber.toString(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Black,
            color = if (active) HabitPrimary else Color.White,
        )
        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .size(6.dp)
                .clip(CircleShape)
                .background(
                    when {
                        dayPct == 100 -> Color(0xFF4ADE80)
                        dayPct > 0 -> if (active) HabitPrimary else Color.White.copy(alpha = 0.5f)
                        else -> Color.Transparent
                    }
                ),
        )
    }
}

@Composable
private fun HomeProgressCard(
    pct: Int,
    completedCount: Int,
    totalCount: Int,
    isToday: Boolean,
    dayLabel: String,
    habits: List<Habit>,
    selectedDate: LocalDate,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .border(1.dp, HabitPrimary.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            ProgressRing(
                progress = if (totalCount == 0) 0f else completedCount.toFloat() / totalCount,
                color = HabitPrimary,
            )
            Text(
                text = stringResource(R.string.home_percent, pct),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = HabitPrimary,
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.home_completed_ratio, completedCount, totalCount),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = HabitOnSurface,
            )
            Text(
                text = if (isToday) {
                    stringResource(R.string.home_completed_today)
                } else {
                    stringResource(R.string.home_completed_day, dayLabel)
                },
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = HabitOnSurfaceVariant,
            )
            if (habits.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    habits.forEach { habit ->
                        val done = isHabitCompletedOnDate(habit, selectedDate)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(CircleShape)
                                .background(
                                    if (done) habitAccentColor(habit) else HabitOutline
                                ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeHabitCard(
    habit: Habit,
    isDone: Boolean,
    isToday: Boolean,
    onToggle: () -> Unit,
) {
    val accentColor = habitAccentColor(habit)
    val streak = calculateStreak(habit.records)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (!isToday && !isDone) 0.55f else 1f)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .border(1.dp, HabitPrimary.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
            .clickable(enabled = isToday, onClick = onToggle)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(accentColor.copy(alpha = 0.09f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = habitEmoji(habit), fontSize = 24.sp)
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = habit.name,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (isDone) HabitOnSurfaceVariant else HabitOnSurface,
                textDecoration = if (isDone) TextDecoration.LineThrough else null,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(top = 2.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.LocalFireDepartment,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(12.dp),
                )
                Text(
                    text = stringResource(R.string.home_streak_days, streak),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = accentColor,
                )
                Text(
                    text = stringResource(R.string.home_time_separator, habitTimeLabel(habit)),
                    style = MaterialTheme.typography.labelSmall,
                    color = HabitOnSurfaceVariant,
                )
            }
        }

        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(if (isDone) accentColor else Color.Transparent)
                .border(
                    width = 2.5.dp,
                    color = if (isDone) accentColor else Color(0xFFCAC4D0),
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (isDone) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun HomeMotivationCard(overallStreak: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.horizontalGradient(listOf(HabitTermsBg, HabitOutline)))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(HabitPrimary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Bolt,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp),
            )
        }
        Column {
            Text(
                text = stringResource(R.string.home_keep_it_up),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
                color = Color(0xFF21005D),
            )
            Text(
                text = stringResource(R.string.home_overall_streak, overallStreak),
                style = MaterialTheme.typography.labelSmall,
                color = HabitTermsText,
            )
        }
    }
}

@Composable
private fun HomeDayInfoCard(
    pct: Int,
    completedCount: Int,
    totalCount: Int,
    isPast: Boolean,
    isFuture: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(HabitField)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = when {
                pct == 100 -> "🏆"
                pct >= 50 -> "📊"
                else -> "📅"
            },
            fontSize = 24.sp,
        )
        Column {
            Text(
                text = when {
                    pct == 100 -> stringResource(R.string.home_perfect_day)
                    isPast -> stringResource(R.string.home_past_record)
                    else -> stringResource(R.string.home_upcoming_day)
                },
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
                color = HabitOnSurface,
            )
            Text(
                text = when {
                    isFuture -> stringResource(R.string.home_future_message)
                    else -> stringResource(
                        R.string.home_past_message,
                        completedCount,
                        totalCount,
                        pct,
                    )
                },
                style = MaterialTheme.typography.labelSmall,
                color = HabitOnSurfaceVariant,
            )
        }
    }
}
