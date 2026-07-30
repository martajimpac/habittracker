package com.marta.habittracker.presentation.screens.detail

import androidx.compose.foundation.background
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.marta.habittracker.R
import com.marta.habittracker.domain.model.Habit
import com.marta.habittracker.presentation.components.ActivityHeatmap
import com.marta.habittracker.presentation.components.HabitLineIcon
import com.marta.habittracker.presentation.components.buildRollingFourWeeks
import com.marta.habittracker.presentation.components.habitDayIntensity
import com.marta.habittracker.presentation.components.toHeatmapWeeks
import com.marta.habittracker.presentation.screens.home.parseHabitColor
import com.marta.habittracker.presentation.theme.HabitOnSurfaceVariant
import com.marta.habittracker.presentation.theme.HabitPrimary
import java.time.LocalDate
import kotlinx.coroutines.launch

@Composable
fun HabitDetailScreen(
    onBack: () -> Unit,
    viewModel: HabitDetailViewModel = hiltViewModel(),
) {
    val habit by viewModel.habit.collectAsStateWithLifecycle()
    val completionRate by viewModel.completionPercentage.collectAsStateWithLifecycle()
    val habitName = habit?.name ?: stringResource(R.string.detail_loading)
    val iconKey = habit?.icon
    val accent = parseHabitColor(habit?.colorHex ?: "#6750A4")
    val reminder = habit?.reminderTime?.takeIf { it.isNotBlank() }
        ?: stringResource(R.string.home_all_day)

    val tabs = listOf(
        stringResource(R.string.detail_tab_stats),
        stringResource(R.string.detail_tab_calendar),
        stringResource(R.string.detail_tab_settings),
    )
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        HabitDetailHeader(
            name = habitName,
            iconKey = iconKey,
            accentColor = accent,
            reminderTime = reminder,
            onBack = onBack,
        )

        SecondaryTabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = accent,
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    text = { Text(text = title) },
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.Top,
        ) { pageIndex ->
            when (pageIndex) {
                0 -> StatisticsTab(completionRate = completionRate, accentColor = accent)
                1 -> CalendarTab(habit = habit)
                2 -> SettingsTab()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitDetailHeader(
    name: String,
    iconKey: String?,
    accentColor: Color,
    reminderTime: String,
    onBack: () -> Unit,
) {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    HabitLineIcon(
                        iconKey = iconKey,
                        tint = accentColor,
                        size = 22.dp,
                    )
                }
                Column {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = reminderTime,
                        style = MaterialTheme.typography.labelMedium,
                        color = HabitOnSurfaceVariant,
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.detail_back),
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
        ),
    )
}

@Composable
fun StatisticsTab(completionRate: Float, accentColor: Color) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.detail_overall_performance),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )

        Spacer(modifier = Modifier.height(32.dp))

        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { completionRate / 100f },
                modifier = Modifier.size(200.dp),
                strokeWidth = 16.dp,
                color = accentColor,
                trackColor = accentColor.copy(alpha = 0.2f),
                strokeCap = StrokeCap.Round,
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${completionRate.toInt()}%",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = accentColor,
                )
                Text(
                    text = stringResource(R.string.detail_completed),
                    style = MaterialTheme.typography.labelMedium,
                    color = HabitOnSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun CalendarTab(habit: Habit?) {
    val today = remember { LocalDate.now() }
    val weeks = remember(habit?.id, habit?.records, today) {
        val current = habit
        toHeatmapWeeks(buildRollingFourWeeks(today)) { date ->
            if (current == null) 0f else habitDayIntensity(current, date)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
    ) {
        ActivityHeatmap(
            weeks = weeks,
            today = today,
            filledColor = habit?.let { parseHabitColor(it.colorHex) } ?: HabitPrimary,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
fun SettingsTab() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text(stringResource(R.string.detail_edit_habit))
    }
}
