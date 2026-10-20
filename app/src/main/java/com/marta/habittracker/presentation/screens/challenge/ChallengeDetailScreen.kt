package com.marta.habittracker.presentation.screens.challenge

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.marta.habittracker.R
import com.marta.habittracker.core.toJava
import com.marta.habittracker.domain.model.ChallengeCard
import com.marta.habittracker.domain.model.ChallengeCriteria
import com.marta.habittracker.domain.model.ChallengeDayCell
import com.marta.habittracker.domain.model.ChallengeStatus
import com.marta.habittracker.domain.model.ChallengeWeekRow
import com.marta.habittracker.domain.model.Profile
import com.marta.habittracker.domain.model.challengeWeekRows
import com.marta.habittracker.presentation.components.HabitButton
import com.marta.habittracker.presentation.components.HabitButtonVariant
import com.marta.habittracker.presentation.components.HabitIconButton
import com.marta.habittracker.presentation.components.HabitLineIcon
import com.marta.habittracker.presentation.components.HabitLoadingIndicator
import com.marta.habittracker.presentation.components.toComposeColor
import com.marta.habittracker.presentation.theme.HabitOnSurface
import com.marta.habittracker.presentation.theme.HabitOutline
import com.marta.habittracker.presentation.utils.ObserveAsEvents
import kotlinx.datetime.LocalDate
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DetailCardShape = RoundedCornerShape(20.dp)

@Composable
fun ChallengeDetailScreen(
    onBack: () -> Unit,
    viewModel: ChallengeDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ObserveAsEvents(flow = viewModel.events) { event ->
        when (event) {
            ChallengeDetailEvent.NavigateBack -> onBack()
        }
    }

    ChallengeDetailContent(
        state = uiState,
        onBack = onBack,
        onAccept = viewModel::onAccept,
        onDecline = viewModel::onDecline,
    )
}

@Composable
fun ChallengeDetailContent(
    state: ChallengeDetailUiState,
    onBack: () -> Unit,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HabitIconButton(
                onClick = onBack,
                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = stringResource(R.string.detail_back),
                enabled = !state.isSubmitting,
            )
        }

        when {
            state.isLoading -> {
                HabitLoadingIndicator(modifier = Modifier.fillMaxSize())
            }

            state.card == null && state.errorRes != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(state.errorRes),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 24.dp),
                ) {
                    if (state.errorRes != null) {
                        Text(
                            text = stringResource(state.errorRes),
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }

                    state.card?.let { card ->
                        ChallengeOverviewCard(card = card)
                        ChallengeActivityCard(card = card)

                        if (state.canRespond) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                HabitButton(
                                    text = stringResource(R.string.challenge_decline),
                                    onClick = onDecline,
                                    enabled = !state.isSubmitting,
                                    variant = HabitButtonVariant.Secondary,
                                    fillMaxWidth = false,
                                    modifier = Modifier.weight(1f),
                                )
                                HabitButton(
                                    text = stringResource(R.string.challenge_accept),
                                    onClick = onAccept,
                                    enabled = !state.isSubmitting,
                                    fillMaxWidth = false,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChallengeOverviewCard(card: ChallengeCard) {
    val accent = card.habitColorHex.toComposeColor()
    Column(
        modifier = Modifier
            .padding(top = 4.dp)
            .detailCard(contentPadding = 14.dp),
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
                    .background(accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                HabitLineIcon(
                    iconKey = card.habitIcon,
                    tint = accent,
                    size = 22.dp,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = card.habitName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(
                        R.string.friends_criteria_value,
                        stringResource(criteriaLabelRes(card.challenge.criteria)),
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Text(
            text = stringResource(statusLabelRes(card.challenge.status)),
            modifier = Modifier.padding(top = 10.dp),
            style = MaterialTheme.typography.labelMedium,
            color = accent,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = stringResource(R.string.friends_days_left, card.daysLeft),
            modifier = Modifier.padding(top = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ChallengeActivityCard(card: ChallengeCard) {
    val accent = card.habitColorHex.toComposeColor()
    val weeks = remember(card.challenge.startsAt, card.challenge.endsAt) {
        challengeWeekRows(card.challenge.startsAt, card.challenge.endsAt)
    }
    if (weeks.isEmpty()) return
    val dayLabels = listOf(
        stringResource(R.string.activity_day_mon),
        stringResource(R.string.activity_day_tue),
        stringResource(R.string.activity_day_wed),
        stringResource(R.string.activity_day_thu),
        stringResource(R.string.activity_day_fri),
        stringResource(R.string.activity_day_sat),
        stringResource(R.string.activity_day_sun),
    )
    val weekFormatter = remember {
        DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
    }

    Column(
        modifier = Modifier
            .padding(top = 12.dp)
            .detailCard(),
    ) {
        Text(
            text = stringResource(R.string.challenge_detail_activity),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Row(
            modifier = Modifier.padding(top = 8.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ChallengeMarkLegend(
                label = stringResource(R.string.challenge_detail_start),
                accent = accent,
                isStart = true,
            )
            ChallengeMarkLegend(
                label = stringResource(R.string.challenge_detail_end),
                accent = accent,
                isEnd = true,
            )
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(Modifier.width(72.dp))
            dayLabels.forEach { label ->
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        weeks.forEach { week ->
            Text(
                text = week.days.first().date.toJava().format(weekFormatter),
                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ChallengePersonRow(
                name = stringResource(R.string.friends_me),
                week = week,
                completedDates = card.myCompletedDates,
                accent = accent,
            )
            ChallengePersonRow(
                name = card.opponent.displayName,
                week = week,
                completedDates = card.theirCompletedDates,
                accent = accent,
            )
        }
    }
}

@Composable
private fun ChallengeMarkLegend(
    label: String,
    accent: Color,
    isStart: Boolean = false,
    isEnd: Boolean = false,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .then(
                    if (isStart) {
                        Modifier.border(1.5.dp, accent, RoundedCornerShape(4.dp))
                    } else {
                        Modifier
                    },
                ),
        ) {
            if (isEnd) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(accent),
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ChallengePersonRow(
    name: String,
    week: ChallengeWeekRow,
    completedDates: Set<LocalDate>,
    accent: Color,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = name,
            modifier = Modifier.width(72.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        week.days.forEach { day ->
            ChallengeDayBox(
                cell = day,
                completed = day.date in completedDates,
                accent = accent,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ChallengeDayBox(
    cell: ChallengeDayCell,
    completed: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(6.dp)
    val onAccent = if (accent.luminance() > 0.6f) HabitOnSurface else Color.White
    val startLabel = stringResource(R.string.challenge_detail_start)
    val endLabel = stringResource(R.string.challenge_detail_end)
    val doneLabel = stringResource(R.string.challenge_day_done)
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(shape)
            .background(
                when {
                    !cell.inChallenge -> Color.Transparent
                    completed -> accent
                    else -> MaterialTheme.colorScheme.surfaceContainerHighest
                },
            )
            .then(
                if (cell.isStart) {
                    Modifier.border(
                        width = 1.5.dp,
                        color = if (completed) onAccent else accent,
                        shape = shape,
                    )
                } else {
                    Modifier
                },
            )
            .semantics {
                if (cell.inChallenge) {
                    contentDescription = listOfNotNull(
                        cell.date.toString(),
                        startLabel.takeIf { cell.isStart },
                        endLabel.takeIf { cell.isEnd },
                        doneLabel.takeIf { completed },
                    ).joinToString(", ")
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = cell.date.toJava().dayOfMonth.toString(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (cell.inChallenge) FontWeight.SemiBold else FontWeight.Normal,
            color = when {
                !cell.inChallenge -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                completed -> onAccent
                else -> MaterialTheme.colorScheme.onSurface
            },
        )
        if (cell.isEnd) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(if (completed) onAccent else accent),
            )
        }
    }
}

@Composable
private fun Modifier.detailCard(contentPadding: Dp = 20.dp): Modifier = this
    .fillMaxWidth()
    .padding(horizontal = 20.dp)
    .clip(DetailCardShape)
    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
    .border(1.dp, HabitOutline, DetailCardShape)
    .padding(contentPadding)

@StringRes
private fun criteriaLabelRes(criteria: ChallengeCriteria): Int = when (criteria) {
    ChallengeCriteria.Streak -> R.string.friends_criteria_streak
    ChallengeCriteria.AllDays -> R.string.friends_criteria_all_days
    ChallengeCriteria.CompletionPct -> R.string.friends_criteria_completion
}

@StringRes
private fun statusLabelRes(status: ChallengeStatus): Int = when (status) {
    ChallengeStatus.Pending -> R.string.challenge_status_pending
    ChallengeStatus.Active -> R.string.challenge_status_active
    ChallengeStatus.Completed -> R.string.challenge_status_completed
    ChallengeStatus.Declined -> R.string.challenge_status_declined
    ChallengeStatus.Cancelled -> R.string.challenge_status_cancelled
}

// @Preview(showBackground = true)
// @Composable
// private fun ChallengeDetailContentPreview() {
    // val card = ChallengeCard(
        // challenge = com.marta.habittracker.domain.model.Challenge(
            // id = "c1",
            // challengerId = "a",
            // challengedId = "b",
            // challengerHabitId = "h1",
            // challengedHabitId = "h2",
            // criteria = ChallengeCriteria.CompletionPct,
            // startsAt = Instant.parse("2026-01-01T00:00:00Z"),
            // endsAt = Instant.parse("2026-01-08T00:00:00Z"),
            // status = ChallengeStatus.Pending,
        // ),
        // opponent = Profile(
            // id = "b",
            // username = "alex",
            // displayName = "Alex",
            // avatarUrl = null,
            // avatarColor = "#3D6E50",
        // ),
        // myProgress = 40,
        // theirProgress = 60,
        // daysLeft = 5,
        // habitName = "Morning run",
        // habitIcon = "run",
        // habitColorHex = "#294536",
        // myCompletedDates = setOf(LocalDate(2026, 1, 2), LocalDate(2026, 1, 4)),
        // theirCompletedDates = setOf(LocalDate(2026, 1, 1), LocalDate(2026, 1, 3)),
    // )
    // ChallengeDetailContent(
        // state = ChallengeDetailUiState(
            // isLoading = false,
            // card = card,
            // canRespond = true,
        // ),
        // onBack = {},
        // onAccept = {},
        // onDecline = {},
    // )
// }
