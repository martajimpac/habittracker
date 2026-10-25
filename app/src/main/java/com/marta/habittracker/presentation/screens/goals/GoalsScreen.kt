package com.marta.habittracker.presentation.screens.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.marta.habittracker.R
import com.marta.habittracker.domain.usecase.WeeklyGoalDay
import com.marta.habittracker.presentation.components.HabitIconButton
import com.marta.habittracker.presentation.theme.habitTopBarButtonHorizontalPadding
import java.time.DayOfWeek

@Composable
fun GoalsScreen(
    onBack: () -> Unit,
    viewModel: GoalsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    GoalsContent(uiState = uiState, onBack = onBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsContent(
    uiState: GoalsUiState,
    onBack: () -> Unit,
) {
    val shown = uiState.completedDays.coerceAtMost(uiState.targetDays)
    val fraction = if (uiState.targetDays == 0) 0f else shown.toFloat() / uiState.targetDays

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.profile_goals)) },
                navigationIcon = {
                    HabitIconButton(
                        onClick = onBack,
                        modifier = Modifier.habitTopBarButtonHorizontalPadding(),
                        icon = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.preferences_back),
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.profileCard()) {
                Text(
                    text = stringResource(R.string.goals_progress, shown, uiState.targetDays),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                )
                Spacer(modifier = Modifier.height(16.dp))
                LinearProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(RoundedCornerShape(99.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    strokeCap = StrokeCap.Round,
                    drawStopIndicator = {},
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.goals_rule),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (uiState.completedDays >= uiState.targetDays) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.goals_reached),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            if (uiState.days.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .profileCard()
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    uiState.days.forEach { day ->
                        GoalDay(day = day)
                    }
                }
            }
        }
    }
}

@Composable
private fun Modifier.profileCard(): Modifier {
    val shape = RoundedCornerShape(24.dp)
    return this
        .clip(shape)
        .background(MaterialTheme.colorScheme.surfaceContainerLowest)
        .border(1.dp, MaterialTheme.colorScheme.outline, shape)
        .padding(16.dp)
}

@Composable
private fun GoalDay(day: WeeklyGoalDay) {
    val letter = stringResource(dayLabelRes(day.dayOfWeek))
    val counts = day.counts
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(
                if (counts) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHighest
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = letter,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = if (counts) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

private fun dayLabelRes(dayOfWeek: DayOfWeek): Int = when (dayOfWeek) {
    DayOfWeek.MONDAY -> R.string.activity_day_mon
    DayOfWeek.TUESDAY -> R.string.activity_day_tue
    DayOfWeek.WEDNESDAY -> R.string.activity_day_wed
    DayOfWeek.THURSDAY -> R.string.activity_day_thu
    DayOfWeek.FRIDAY -> R.string.activity_day_fri
    DayOfWeek.SATURDAY -> R.string.activity_day_sat
    DayOfWeek.SUNDAY -> R.string.activity_day_sun
}

// @Preview(showBackground = true)
// @Composable
// private fun GoalsContentPreview() {
    // InstaDevTheme {
        // GoalsContent(
            // uiState = GoalsUiState(
                // completedDays = 2,
                // targetDays = 5,
                // days = DayOfWeek.entries.map { day ->
                    // WeeklyGoalDay(dayOfWeek = day, counts = day == DayOfWeek.MONDAY)
                // },
            // ),
            // onBack = {},
        // )
    // }
// }
