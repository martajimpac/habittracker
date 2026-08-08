package com.marta.habittracker.presentation.widgets.challenge

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.marta.habittracker.R
import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.model.ChallengeCard
import com.marta.habittracker.presentation.screens.home.parseHabitColor
import com.marta.habittracker.presentation.theme.HabitSurface
import com.marta.habittracker.presentation.widgets.WidgetEntryPoint
import com.marta.habittracker.presentation.widgets.WidgetLaunchExtras
import dagger.hilt.android.EntryPointAccessors
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class ChallengeWidgetSnapshot(
    val challengeId: String,
    val habitName: String,
    val habitColorHex: String,
    val opponentDisplayName: String,
    val myProgress: Int,
    val theirProgress: Int,
    val daysLeft: Int,
    val status: String = "active",
)

internal fun encodeChallengeWidgetSnapshot(
    challenge: ChallengeCard,
    json: Json,
): String = json.encodeToString(challenge.toChallengeWidgetSnapshot())

internal fun decodeChallengeWidgetSnapshot(
    snapshotJson: String,
    json: Json,
): ChallengeWidgetSnapshot = json.decodeFromString(snapshotJson)

private fun ChallengeCard.toChallengeWidgetSnapshot(): ChallengeWidgetSnapshot =
    ChallengeWidgetSnapshot(
        challengeId = challenge.id,
        habitName = habitName,
        habitColorHex = habitColorHex,
        opponentDisplayName = opponent.displayName,
        myProgress = myProgress,
        theirProgress = theirProgress,
        daysLeft = daysLeft,
        status = challenge.status.name.lowercase(),
    )

class ChallengeGlanceWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val preferencesDataSource = entryPoint.widgetPreferencesDataSource()
        val json = entryPoint.json()
        val prefs = preferencesDataSource.getChallengeWidget(appWidgetId)
        val snapshot = prefs?.let { savedPrefs ->
            refreshSnapshotIfOnline(
                challengeId = savedPrefs.challengeId,
                appWidgetId = appWidgetId,
                entryPoint = entryPoint,
                fallbackSnapshotJson = savedPrefs.snapshotJson,
                json = json,
            )
        }?.let { snapshotJson ->
            runCatching { decodeChallengeWidgetSnapshot(snapshotJson, json) }
                .onFailure { exception ->
                    Log.w(TAG, "Could not decode challenge widget snapshot for widget $appWidgetId", exception)
                }
                .getOrNull()
        }

        provideContent {
            ChallengeWidgetContent(
                snapshot = snapshot,
                configureText = context.getString(R.string.challenge_widget_configure),
                versusText = snapshot?.let {
                    context.getString(R.string.challenge_widget_versus, it.opponentDisplayName)
                }.orEmpty(),
                progressText = snapshot?.let {
                    context.getString(
                        R.string.challenge_widget_progress,
                        it.myProgress,
                        it.opponentDisplayName,
                        it.theirProgress,
                    )
                }.orEmpty(),
                daysLeftText = snapshot?.let {
                    context.getString(R.string.challenge_widget_days_left, it.daysLeft)
                }.orEmpty(),
                statusText = snapshot
                    ?.takeIf { it.status == "pending" }
                    ?.let { context.getString(R.string.challenge_widget_status_pending) }
                    .orEmpty(),
                openAppAction = actionStartActivity(WidgetLaunchExtras.openFriendsIntent(context)),
            )
        }
    }

    private suspend fun refreshSnapshotIfOnline(
        challengeId: String,
        appWidgetId: Int,
        entryPoint: WidgetEntryPoint,
        fallbackSnapshotJson: String,
        json: Json,
    ): String {
        if (!entryPoint.networkChecker().isOnline()) return fallbackSnapshotJson

        return try {
            when (val result = entryPoint.friendsRepository().getActiveChallenges()) {
                is DataResult.Success -> {
                    val challenge = result.data.firstOrNull { it.challenge.id == challengeId }
                        ?: return fallbackSnapshotJson
                    val refreshedSnapshotJson = encodeChallengeWidgetSnapshot(challenge, json)
                    entryPoint.widgetPreferencesDataSource().setChallengeWidget(
                        appWidgetId = appWidgetId,
                        challengeId = challengeId,
                        snapshotJson = refreshedSnapshotJson,
                    )
                    Log.d(TAG, "Refreshed challenge widget snapshot for widget $appWidgetId")
                    refreshedSnapshotJson
                }

                is DataResult.Error -> {
                    Log.w(TAG, "Could not refresh challenge widget snapshot for widget $appWidgetId")
                    fallbackSnapshotJson
                }
            }
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to refresh challenge widget snapshot for widget $appWidgetId", exception)
            fallbackSnapshotJson
        }
    }

    private companion object {
        const val TAG = "ChallengeWidget"
    }
}

@Composable
private fun ChallengeWidgetContent(
    snapshot: ChallengeWidgetSnapshot?,
    configureText: String,
    versusText: String,
    progressText: String,
    daysLeftText: String,
    statusText: String,
    openAppAction: androidx.glance.action.Action,
) {
    val accent = snapshot?.let { parseHabitColor(it.habitColorHex) }
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(accent?.copy(alpha = 0.12f) ?: HabitSurface)
            .clickable(openAppAction)
            .padding(16.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically,
        horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
    ) {
        if (snapshot == null) {
            Text(text = configureText)
        } else {
            Text(
                text = snapshot.habitName,
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    color = androidx.glance.unit.ColorProvider(requireNotNull(accent)),
                ),
            )
            Text(text = versusText)
            Text(text = progressText)
            Text(text = daysLeftText)
            if (statusText.isNotBlank()) {
                Text(text = statusText)
            }
        }
    }
}
