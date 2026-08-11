package com.marta.habittracker.presentation.widgets.challenge

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.lifecycle.lifecycleScope
import com.marta.habittracker.R
import com.marta.habittracker.core.network.NetworkChecker
import com.marta.habittracker.data.local.datastore.WidgetPreferencesDataSource
import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.model.ChallengeCard
import com.marta.habittracker.domain.model.ChallengeStatus
import com.marta.habittracker.domain.repository.FriendsRepository
import com.marta.habittracker.presentation.theme.InstaDevTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

@AndroidEntryPoint
class ChallengeWidgetConfigActivity : ComponentActivity() {

    @Inject
    lateinit var friendsRepository: FriendsRepository

    @Inject
    lateinit var networkChecker: NetworkChecker

    @Inject
    lateinit var widgetPreferencesDataSource: WidgetPreferencesDataSource

    @Inject
    lateinit var json: Json

    private var challenges by mutableStateOf<List<ChallengeCard>>(emptyList())
    private var isLoading by mutableStateOf(true)
    private var errorMessageResId by mutableStateOf<Int?>(null)
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)
        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        enableEdgeToEdge()
        setContent {
            InstaDevTheme {
                ChallengeWidgetConfigContent(
                    challenges = challenges,
                    isLoading = isLoading,
                    errorMessageResId = errorMessageResId,
                    onChallengeSelected = ::configureWidget,
                )
            }
        }

        loadChallenges()
    }

    private fun loadChallenges() {
        if (!networkChecker.isOnline()) {
            errorMessageResId = R.string.error_no_internet
            isLoading = false
            return
        }

        lifecycleScope.launch {
            try {
                when (val result = friendsRepository.getActiveChallenges()) {
                    is DataResult.Success -> challenges = result.data
                    is DataResult.Error -> errorMessageResId = R.string.challenge_widget_load_failed
                }
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to load challenges for widget configuration", exception)
                errorMessageResId = R.string.challenge_widget_load_failed
            }
            isLoading = false
        }
    }

    private fun configureWidget(challenge: ChallengeCard) {
        lifecycleScope.launch {
            widgetPreferencesDataSource.setChallengeWidget(
                appWidgetId = appWidgetId,
                challengeId = challenge.challenge.id,
                snapshotJson = encodeChallengeWidgetSnapshot(challenge, json),
            )
            val glanceId = GlanceAppWidgetManager(this@ChallengeWidgetConfigActivity)
                .getGlanceIdBy(appWidgetId)
            ChallengeGlanceWidget().update(this@ChallengeWidgetConfigActivity, glanceId)
            setResult(
                RESULT_OK,
                Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId),
            )
            finish()
        }
    }

    private companion object {
        const val TAG = "ChallengeWidgetConfig"
    }
}

@androidx.compose.runtime.Composable
private fun ChallengeWidgetConfigContent(
    challenges: List<ChallengeCard>,
    isLoading: Boolean,
    errorMessageResId: Int?,
    onChallengeSelected: (ChallengeCard) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.challenge_widget_config_title),
            style = MaterialTheme.typography.headlineSmall,
        )
        when {
            isLoading -> CircularProgressIndicator()
            errorMessageResId != null -> Text(text = stringResource(errorMessageResId))
            challenges.isEmpty() -> Text(text = stringResource(R.string.challenge_widget_empty_config))
            else -> LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(challenges, key = { it.challenge.id }) { challenge ->
                    ListItem(
                        headlineContent = { Text(challenge.habitName) },
                        supportingContent = {
                            Column {
                                Text(
                                    stringResource(
                                        R.string.challenge_widget_versus,
                                        challenge.opponent.displayName,
                                    ),
                                )
                                Text(
                                    text = stringResource(
                                        when (challenge.challenge.status) {
                                            ChallengeStatus.Pending ->
                                                R.string.challenge_widget_status_pending
                                            else -> R.string.challenge_widget_status_active
                                        },
                                    ),
                                )
                            }
                        },
                        modifier = Modifier.clickable { onChallengeSelected(challenge) },
                    )
                }
            }
        }
    }
}
