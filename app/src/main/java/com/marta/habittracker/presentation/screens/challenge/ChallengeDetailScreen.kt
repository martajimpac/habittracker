package com.marta.habittracker.presentation.screens.challenge

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.marta.habittracker.R
import com.marta.habittracker.domain.model.ChallengeCard
import com.marta.habittracker.domain.model.ChallengeCriteria
import com.marta.habittracker.domain.model.ChallengeStatus
import com.marta.habittracker.domain.model.Profile
import com.marta.habittracker.presentation.components.HabitIconButton
import com.marta.habittracker.presentation.components.HabitLineIcon
import com.marta.habittracker.presentation.components.HabitLoadingIndicator
import com.marta.habittracker.presentation.components.toComposeColor
import com.marta.habittracker.presentation.theme.HabitOnSurface
import com.marta.habittracker.presentation.theme.HabitOnSurfaceVariant
import com.marta.habittracker.presentation.utils.ObserveAsEvents
import java.time.Instant

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChallengeDetailContent(
    state: ChallengeDetailUiState,
    onBack: () -> Unit,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
) {
    val surfaceColor = MaterialTheme.colorScheme.surface

    Scaffold(
        containerColor = surfaceColor,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.challenge_detail_title),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                },
                navigationIcon = {
                    HabitIconButton(
                        onClick = onBack,
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.detail_back),
                        enabled = !state.isSubmitting,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = surfaceColor,
                ),
            )
        },
    ) { innerPadding ->
        when {
            state.isLoading -> {
                HabitLoadingIndicator(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                )
            }

            state.card == null && state.errorRes != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(state.errorRes),
                        style = MaterialTheme.typography.bodyLarge,
                        color = HabitOnSurfaceVariant,
                    )
                }
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    if (state.errorRes != null) {
                        Text(
                            text = stringResource(state.errorRes),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }

                    state.card?.let { card ->
                        ChallengeDetailCard(card = card)

                        if (state.canRespond) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                OutlinedButton(
                                    onClick = onDecline,
                                    enabled = !state.isSubmitting,
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text(stringResource(R.string.challenge_decline))
                                }
                                Button(
                                    onClick = onAccept,
                                    enabled = !state.isSubmitting,
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text(stringResource(R.string.challenge_accept))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChallengeDetailCard(card: ChallengeCard) {
    val cardColor = card.habitColorHex.toComposeColor()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            HabitLineIcon(card.habitIcon, tint = cardColor, size = 24.dp)
            Spacer(Modifier.width(10.dp))
            Text(
                text = card.habitName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = HabitOnSurface,
                modifier = Modifier.weight(1f),
            )
        }

        Text(
            text = stringResource(criteriaLabelRes(card.challenge.criteria)),
            style = MaterialTheme.typography.labelLarge,
            color = HabitOnSurfaceVariant,
            fontWeight = FontWeight.Bold,
        )

        Text(
            text = stringResource(statusLabelRes(card.challenge.status)),
            style = MaterialTheme.typography.labelMedium,
            color = cardColor,
            fontWeight = FontWeight.Bold,
        )

        ChallengeDetailProgress(
            label = stringResource(R.string.friends_me),
            progress = card.myProgress,
            accent = cardColor,
        )
        ChallengeDetailProgress(
            label = card.opponent.displayName,
            progress = card.theirProgress,
            accent = cardColor,
        )

        Text(
            text = stringResource(R.string.friends_days_left, card.daysLeft),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = HabitOnSurface,
        )
    }
}

@Composable
private fun ChallengeDetailProgress(
    label: String,
    progress: Int,
    accent: Color,
) {
    Text(label, style = MaterialTheme.typography.labelMedium, color = HabitOnSurface)
    LinearProgressIndicator(
        progress = { progress.coerceIn(0, 100) / 100f },
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        color = accent,
        trackColor = accent.copy(alpha = 0.2f),
    )
}

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

@Preview(showBackground = true)
@Composable
private fun ChallengeDetailContentPreview() {
    val card = ChallengeCard(
        challenge = com.marta.habittracker.domain.model.Challenge(
            id = "c1",
            challengerId = "a",
            challengedId = "b",
            challengerHabitId = "h1",
            challengedHabitId = "h2",
            criteria = ChallengeCriteria.CompletionPct,
            startsAt = Instant.parse("2026-01-01T00:00:00Z"),
            endsAt = Instant.parse("2026-01-08T00:00:00Z"),
            status = ChallengeStatus.Pending,
        ),
        opponent = Profile(
            id = "b",
            username = "alex",
            displayName = "Alex",
            avatarUrl = null,
            avatarColor = "#1D4EA6",
        ),
        myProgress = 40,
        theirProgress = 60,
        daysLeft = 5,
        habitName = "Morning run",
        habitIcon = "run",
        habitColorHex = "#021C4B",
    )
    ChallengeDetailContent(
        state = ChallengeDetailUiState(
            isLoading = false,
            card = card,
            canRespond = true,
        ),
        onBack = {},
        onAccept = {},
        onDecline = {},
    )
}
