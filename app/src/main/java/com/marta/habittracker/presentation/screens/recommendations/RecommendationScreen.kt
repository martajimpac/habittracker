package com.marta.habittracker.presentation.screens.recommendations

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.marta.habittracker.domain.usecase.HabitWeekStat
import com.marta.habittracker.presentation.components.HabitButton
import com.marta.habittracker.presentation.components.HabitIconButton
import com.marta.habittracker.presentation.theme.habitTopBarButtonHorizontalPadding

@Composable
fun RecommendationScreen(
    onBack: () -> Unit,
    viewModel: RecommendationViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    RecommendationContent(
        uiState = uiState,
        onBack = onBack,
        onRequest = viewModel::request,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecommendationContent(
    uiState: RecommendationUiState,
    onBack: () -> Unit,
    onRequest: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.recommendation_title)) },
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
            when (uiState.phase) {
                RecommendationPhase.LoadingHabits -> LoadingBlock()
                RecommendationPhase.Empty -> EmptyBlock()
                RecommendationPhase.Idle,
                RecommendationPhase.Loading,
                RecommendationPhase.Ready,
                RecommendationPhase.Error,
                -> ResultBlock(uiState = uiState, onRequest = onRequest)
            }
        }
    }
}

@Composable
private fun LoadingBlock() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = stringResource(R.string.recommendation_loading))
    }
}

@Composable
private fun EmptyBlock() {
    Text(
        text = stringResource(R.string.recommendation_empty_title),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Black,
    )
    Text(
        text = stringResource(R.string.recommendation_empty_message),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ResultBlock(
    uiState: RecommendationUiState,
    onRequest: () -> Unit,
) {
    uiState.stats.forEach { stat ->
        StatLine(stat)
    }
    if (uiState.phase == RecommendationPhase.Loading) {
        LoadingBlock()
    }
    val errorRes = uiState.errorRes
    if (errorRes != null) {
        Text(
            text = stringResource(errorRes),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
    val suggestion = uiState.suggestion
    if (suggestion != null) {
        Text(
            text = stringResource(R.string.recommendation_suggestion_label),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
        )
        Text(
            text = suggestion,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
    val loading = uiState.phase == RecommendationPhase.Loading
    HabitButton(
        text = stringResource(
            if (uiState.phase == RecommendationPhase.Error) {
                R.string.recommendation_retry
            } else {
                R.string.recommendation_request
            },
        ),
        onClick = onRequest,
        enabled = !loading,
        loading = loading,
    )
}

@Composable
private fun StatLine(stat: HabitWeekStat) {
    Text(
        text = stringResource(
            R.string.recommendation_stat,
            stat.name,
            stat.completedDays,
            stat.totalDays,
        ),
        style = MaterialTheme.typography.bodyLarge,
    )
}
