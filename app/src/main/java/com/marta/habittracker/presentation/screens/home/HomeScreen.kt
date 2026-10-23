package com.marta.habittracker.presentation.screens.home

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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.marta.habittracker.R
import com.marta.habittracker.domain.model.Habit
import com.marta.habittracker.domain.model.countsInWeeks
import com.marta.habittracker.domain.usecase.HabitProtectionStatus
import com.marta.habittracker.domain.usecase.StreakOutcome
import com.marta.habittracker.presentation.components.HabitDialog
import com.marta.habittracker.presentation.components.HabitDialogTone
import com.marta.habittracker.presentation.components.StreakProtectionBadge
import com.marta.habittracker.presentation.utils.ObserveAsEvents
import com.marta.habittracker.presentation.components.ChallengeHabitMark
import com.marta.habittracker.presentation.components.HabitLineIcon
import com.marta.habittracker.presentation.components.ProfileAvatar
import com.marta.habittracker.presentation.theme.HabitCream
import com.marta.habittracker.presentation.theme.HabitField
import com.marta.habittracker.presentation.theme.HabitOnSurface
import com.marta.habittracker.presentation.theme.HabitOnSurfaceVariant
import com.marta.habittracker.presentation.theme.HabitOutline
import com.marta.habittracker.presentation.theme.HabitPrimary
import com.marta.habittracker.presentation.theme.HabitPrimaryLight
import com.marta.habittracker.presentation.theme.isHabitDarkTheme
import com.marta.habittracker.presentation.theme.SuccessColor
import com.marta.habittracker.presentation.utils.toHex
import com.marta.habittracker.presentation.components.ProgressFlower
import com.marta.habittracker.presentation.components.WiltedFlower
import com.marta.habittracker.presentation.utils.dayLabel
import com.marta.habittracker.presentation.utils.dayShortLabel
import com.marta.habittracker.presentation.utils.greetingForHour
import com.marta.habittracker.presentation.utils.habitAccentColor
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val habits by viewModel.habits.collectAsStateWithLifecycle()
    val allHabits by viewModel.allHabits.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val today by viewModel.today.collectAsStateWithLifecycle()
    val weekDays by viewModel.weekDays.collectAsStateWithLifecycle()
    val userDisplayName by viewModel.userDisplayName.collectAsStateWithLifecycle()
    val avatarUrl by viewModel.avatarUrl.collectAsStateWithLifecycle()
    val actionErrorRes by viewModel.actionErrorRes.collectAsStateWithLifecycle()
    val challengeHabitIds by viewModel.challengeHabitIds.collectAsStateWithLifecycle()
    val protection by viewModel.protection.collectAsStateWithLifecycle()
    var showProtectionEarned by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current

    ObserveAsEvents(viewModel.events) { event ->
        if (event is HomeEvent.ProtectionEarned) showProtectionEarned = true
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshDay()
                viewModel.refreshChallengeHabits()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(today) {
        val zone = ZoneId.systemDefault()
        val nextMidnight = today.plusDays(1).atStartOfDay(zone)
        val waitMs = Duration.between(ZonedDateTime.now(zone), nextMidnight).toMillis()
        if (waitMs > 0) delay(waitMs)
        viewModel.refreshDay()
    }

    LaunchedEffect(Unit) {
        viewModel.refreshDisplayName()
    }

    HomeContent(
        habits = habits,
        allHabits = allHabits,
        selectedDate = selectedDate,
        today = today,
        userDisplayName = userDisplayName,
        avatarUrl = avatarUrl,
        actionErrorRes = actionErrorRes,
        weekDays = weekDays,
        onDateSelected = viewModel::onDateSelected,
        onToggleHabit = viewModel::toggleComplete,
        onClearActionError = viewModel::clearActionError,
        challengeHabitIds = challengeHabitIds,
        protectionBalance = protection.balance,
        protectionByHabit = protection.habits.associateBy { it.habitId },
    )
    if (showProtectionEarned) {
        HabitDialog(
            onDismissRequest = { showProtectionEarned = false },
            title = stringResource(R.string.freeze_earned_title),
            message = stringResource(R.string.freeze_earned_message),
            confirmText = stringResource(R.string.dialog_ok),
            onConfirm = { showProtectionEarned = false },
            tone = HabitDialogTone.Positive,
        )
    }
}

@Composable
fun HomeContent(
    habits: List<Habit>,
    allHabits: List<Habit>,
    selectedDate: LocalDate,
    today: LocalDate,
    userDisplayName: String,
    avatarUrl: String?,
    actionErrorRes: Int?,
    weekDays: List<LocalDate>,
    onDateSelected: (LocalDate) -> Unit,
    onToggleHabit: (Habit) -> Unit,
    onClearActionError: () -> Unit,
    challengeHabitIds: Set<String> = emptySet(),
    protectionBalance: Int = 0,
    protectionByHabit: Map<String, HabitProtectionStatus> = emptyMap(),
) {
    val isToday = selectedDate == today
    val isPast = selectedDate.isBefore(today)

    val (completedCount, totalCount) = remember(habits, selectedDate) {
        completionStats(habits, selectedDate)
    }
    val pct = completionPercent(completedCount, totalCount)
    val greetingRes = greetingForHour(java.time.LocalTime.now().hour)
    val dateLabel = remember(selectedDate) {
        val locale = Locale.getDefault()
        if (locale.language == "es") {
            selectedDate.format(DateTimeFormatter.ofPattern("d 'de' MMMM", locale))
        } else {
            selectedDate.format(DateTimeFormatter.ofPattern("MMMM d", locale))
        }
    }
    val dayLabel = remember(selectedDate) {
        val locale = Locale.getDefault()
        selectedDate.dayOfWeek.getDisplayName(TextStyle.FULL, locale)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
    }
    val overallStreak = remember(allHabits, protectionByHabit) {
        val daily = allHabits.filter { !it.countsInWeeks() }
            .maxOfOrNull { protectionByHabit[it.id]?.streak ?: calculateStreak(it) } ?: 0
        if (daily > 0) {
            daily to false
        } else {
            val weekly = allHabits.filter { it.countsInWeeks() }
                .maxOfOrNull { protectionByHabit[it.id]?.streak ?: calculateStreak(it) } ?: 0
            weekly to true
        }
    }

    Scaffold(
        topBar = {
            HomeHeader(
                selectedDate = selectedDate,
                today = today,
                isToday = isToday,
                isPast = isPast,
                dateLabel = dateLabel,
                dayLabel = dayLabel,
                greetingRes = greetingRes,
                userDisplayName = userDisplayName,
                avatarUrl = avatarUrl,
                weekDays = weekDays,
                allHabits = allHabits,
                onDateSelected = onDateSelected,
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
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
                actionErrorRes?.let { errorRes ->
                    item {
                        Text(
                            text = stringResource(errorRes),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = onClearActionError),
                        )
                    }
                }

                item {
                    HomeProgressCard(
                        pct = pct,
                        completedCount = completedCount,
                        totalCount = totalCount,
                        isToday = isToday,
                        dayLabel = dayLabel(selectedDate),
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
                                stringResource(R.string.home_day_habits, dayLabel(selectedDate))
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface,
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
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            WiltedFlower()
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.home_empty_habits),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                } else {
                    items(habits, key = { it.id }) { habit ->
                        HomeHabitCard(
                            habit = habit,
                            isDone = habit.isCompleted,
                            isToday = isToday,
                            inChallenge = habit.id in challengeHabitIds,
                            protection = protectionByHabit[habit.id],
                            onToggle = { onToggleHabit(habit) },
                        )
                    }
                }

                when {
                    isToday && overallStreak.first > 2 -> item {
                        HomeMotivationCard(
                            overallStreak = overallStreak.first,
                            inWeeks = overallStreak.second,
                        )
                    }
                    isPast && pct < 100 -> item {
                        HomeDayInfoCard(
                            pct = pct,
                            completedCount = completedCount,
                            totalCount = totalCount,
                        )
                    }
                }
        }
    }
}

@Composable
private fun HomeHeader(
    selectedDate: LocalDate,
    today: LocalDate,
    isToday: Boolean,
    isPast: Boolean,
    dateLabel: String,
    dayLabel: String,
    greetingRes: Int,
    userDisplayName: String,
    avatarUrl: String?,
    weekDays: List<LocalDate>,
    allHabits: List<Habit>,
    onDateSelected: (LocalDate) -> Unit,
) {
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    val headerGreen = if (isHabitDarkTheme()) {
        Modifier.background(MaterialTheme.colorScheme.primary)
    } else {
        Modifier.background(Brush.linearGradient(listOf(HabitPrimary, HabitPrimaryLight)))
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(headerGreen)
            .statusBarsPadding()
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
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.SemiBold,
                    color = onPrimary.copy(alpha = 0.75f),
                )
                Text(
                    text = if (isToday) {
                        stringResource(
                            R.string.home_greeting,
                            stringResource(greetingRes),
                            firstName(userDisplayName),
                        )
                    } else {
                        dayLabel
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = onPrimary,
                )
            }
            ProfileAvatar(
                avatarUrl = avatarUrl,
                displayName = userDisplayName,
                avatarColor = HabitPrimaryLight.toHex(),
                size = 40.dp,
                contentDescription = stringResource(R.string.cd_profile_avatar),
                borderColor = HabitCream,
                borderWidth = 2.dp,
            )
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
                    isToday = date == today,
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
    isToday: Boolean,
    dayPct: Int,
    onClick: () -> Unit,
) {
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    val primary = MaterialTheme.colorScheme.primary
    Column(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(if (active) onPrimary else onPrimary.copy(alpha = 0.15f))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isToday) FontWeight.Black else FontWeight.Medium,
            fontSize = 10.sp,
            color = if (active) primary else onPrimary.copy(alpha = if (isToday) 1f else 0.7f),
        )
        Text(
            text = dayNumber.toString(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (isToday) FontWeight.Black else FontWeight.Medium,
            color = if (active) primary else onPrimary,
        )
        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .size(6.dp)
                .clip(CircleShape)
                .background(
                    when {
                        dayPct == 100 -> SuccessColor
                        dayPct > 0 -> if (active) primary else onPrimary.copy(alpha = 0.5f)
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
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .border(1.dp, HabitPrimary.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        ProgressFlower(
            progress = if (totalCount == 0) 0f else completedCount.toFloat() / totalCount,
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.home_completed_ratio, completedCount, totalCount),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = if (isToday) {
                    stringResource(R.string.home_completed_today)
                } else {
                    stringResource(R.string.home_completed_day, dayLabel)
                },
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
    inChallenge: Boolean,
    protection: HabitProtectionStatus?,
    onToggle: () -> Unit,
) {
    val accentColor = habitAccentColor(habit)
    val streak = protection?.streak ?: calculateStreak(habit)
    val inWeeks = habit.countsInWeeks()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (!isToday && !isDone) 0.55f else 1f)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .border(1.dp, HabitPrimary.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
            .clickable(enabled = isToday, onClick = onToggle)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(accentColor.copy(alpha = 0.09f)),
                contentAlignment = Alignment.Center,
            ) {
                HabitLineIcon(
                    habit = habit,
                    tint = accentColor,
                    size = 24.dp,
                )
            }
            if (inChallenge) {
                ChallengeHabitMark(
                    background = accentColor,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = (-4).dp, y = (-4).dp),
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = habit.name,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (isDone) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                textDecoration = if (isDone) TextDecoration.LineThrough else null,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(top = 2.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.LocalFireDepartment,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(12.dp),
                )
                Text(
                    text = if (inWeeks) {
                        pluralStringResource(R.plurals.streak_weeks, streak, streak)
                    } else {
                        stringResource(R.string.home_streak_days, streak)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = accentColor,
                )
                Text(
                    text = stringResource(R.string.home_time_separator, habitTimeLabel(habit)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            val protectionOutcome = protection?.outcome
            if (
                protectionOutcome == StreakOutcome.PROTECTED ||
                protectionOutcome == StreakOutcome.BROKEN
            ) {
                Text(
                    text = if (protectionOutcome == StreakOutcome.PROTECTED) {
                        stringResource(R.string.freeze_used)
                    } else {
                        stringResource(R.string.streak_broken)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    color = if (isDone) accentColor else MaterialTheme.colorScheme.outline,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (isDone) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun HomeMotivationCard(overallStreak: Int, inWeeks: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Bolt,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(18.dp),
            )
        }
        Column {
            Text(
                text = stringResource(R.string.home_keep_it_up),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(
                    if (inWeeks) R.string.home_overall_streak_weeks else R.string.home_overall_streak,
                    overallStreak,
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun HomeDayInfoCard(
    pct: Int,
    completedCount: Int,
    totalCount: Int,
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
        Icon(
            painter = painterResource(
                if (pct >= 50) R.drawable.ic_onboarding_chart else R.drawable.ic_home_calendar,
            ),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp),
        )
        Column {
            Text(
                text = stringResource(R.string.home_past_record),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
                color = HabitOnSurface,
            )
            Text(
                text = stringResource(
                    R.string.home_past_message,
                    completedCount,
                    totalCount,
                    pct,
                ),
                style = MaterialTheme.typography.labelSmall,
                color = HabitOnSurfaceVariant,
            )
        }
    }
}

// @Preview(showBackground = true, showSystemUi = true)
// @Composable
// private fun HomeContentPreview() {
    // HomeContent(
        // habits = emptyList(),
        // allHabits = emptyList(),
        // selectedDate = LocalDate.now(),
        // today = LocalDate.now(),
        // userDisplayName = "John",
        // avatarUrl = null,
        // actionErrorRes = null,
        // weekDays = (0..6).map { LocalDate.now().plusDays(it.toLong()) },
        // onDateSelected = {},
        // onToggleHabit = {},
        // onClearActionError = {},
    // )
// }
