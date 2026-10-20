package com.marta.habittracker.presentation.widgets.challenge

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.marta.habittracker.R
import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.model.ChallengeCard
import com.marta.habittracker.domain.model.challengeLeaderLabel
import com.marta.habittracker.domain.widget.ChallengeWidgetLookup
import com.marta.habittracker.domain.widget.resolveChallengeWidgetSnapshot
import com.marta.habittracker.presentation.theme.HabitOnSurface
import com.marta.habittracker.presentation.theme.HabitOnSurfaceVariant
import com.marta.habittracker.presentation.theme.HabitSurface
import com.marta.habittracker.presentation.utils.parseHabitColor
import com.marta.habittracker.presentation.widgets.WidgetEntryPoint
import com.marta.habittracker.presentation.widgets.WidgetLaunchExtras
import com.marta.habittracker.presentation.widgets.WidgetLoad
import com.marta.habittracker.presentation.widgets.WidgetLoading
import com.marta.habittracker.presentation.widgets.WidgetProgressBar
import com.marta.habittracker.presentation.widgets.WidgetScreen
import dagger.hilt.android.EntryPointAccessors
import kotlinx.serialization.Serializable
import kotlinx.coroutines.CancellationException
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
        val json = entryPoint.json()
        provideContent {
            val state by produceState<WidgetLoad<ChallengeWidgetSnapshot?>>(WidgetLoad.Loading, appWidgetId) {
                value = loadChallengeSnapshot(
                    appWidgetId = appWidgetId,
                    entryPoint = entryPoint,
                    json = json,
                )
            }
            when (val current = state) {
                WidgetLoad.Loading -> WidgetLoading(context.getString(R.string.widget_loading))
                is WidgetLoad.Ready -> {
                    val snapshot = current.value
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
                        leaderText = snapshot?.let {
                            challengeLeaderLabel(
                                myProgress = it.myProgress,
                                theirProgress = it.theirProgress,
                                meLabel = context.getString(R.string.friends_me),
                                opponentName = it.opponentDisplayName,
                                tieLabel = context.getString(R.string.friends_challenge_tie),
                            )
                        }.orEmpty(),
                        leaderDescription = context.getString(R.string.friends_challenge_leader),
                        statusText = snapshot
                            ?.takeIf { it.status == "pending" }
                            ?.let { context.getString(R.string.challenge_widget_status_pending) }
                            .orEmpty(),
                        openAppAction = actionStartActivity(WidgetLaunchExtras.openFriendsIntent(context)),
                    )
                }
            }
        }
    }

    private suspend fun loadChallengeSnapshot(
        appWidgetId: Int,
        entryPoint: WidgetEntryPoint,
        json: Json,
    ): WidgetLoad<ChallengeWidgetSnapshot?> = try {
        val prefs = entryPoint.widgetPreferencesDataSource().getChallengeWidget(appWidgetId)
            ?: return WidgetLoad.Ready(null)
        val hasSession = entryPoint.authRepository().getCurrentUserId() != null
        val snapshotJson = refreshSnapshotIfOnline(
            challengeId = prefs.challengeId,
            appWidgetId = appWidgetId,
            entryPoint = entryPoint,
            fallbackSnapshotJson = prefs.snapshotJson,
            hasSession = hasSession,
            json = json,
        ) ?: return WidgetLoad.Ready(null)
        val snapshot = runCatching { decodeChallengeWidgetSnapshot(snapshotJson, json) }
            .onFailure { exception ->
                Log.w(TAG, "Could not decode challenge widget snapshot for widget $appWidgetId", exception)
            }
            .getOrNull()
        WidgetLoad.Ready(snapshot)
    } catch (exception: Exception) {
        Log.e(TAG, "Challenge widget load failed for widget $appWidgetId", exception)
        WidgetLoad.Ready(null)
    }

    private suspend fun refreshSnapshotIfOnline(
        challengeId: String,
        appWidgetId: Int,
        entryPoint: WidgetEntryPoint,
        fallbackSnapshotJson: String,
        hasSession: Boolean,
        json: Json,
    ): String? {
        val lookup = if (!hasSession || !entryPoint.networkChecker().isOnline()) {
            ChallengeWidgetLookup.Unavailable
        } else {
            try {
                when (val result = entryPoint.friendsRepository().getActiveChallenges()) {
                    is DataResult.Success -> {
                        val challenge = result.data.firstOrNull { it.challenge.id == challengeId }
                        if (challenge == null) {
                            ChallengeWidgetLookup.MissingFromAccount
                        } else {
                            ChallengeWidgetLookup.Found(
                                encodeChallengeWidgetSnapshot(challenge, json),
                            )
                        }
                    }

                    is DataResult.Error -> {
                        Log.w(TAG, "Could not refresh challenge widget snapshot for widget $appWidgetId")
                        ChallengeWidgetLookup.Unavailable
                    }
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to refresh challenge widget snapshot for widget $appWidgetId", exception)
                ChallengeWidgetLookup.Unavailable
            }
        }
        val resolved = resolveChallengeWidgetSnapshot(
            hasSession = hasSession,
            cachedJson = fallbackSnapshotJson,
            lookup = lookup,
        )
        when (lookup) {
            is ChallengeWidgetLookup.Found -> {
                entryPoint.widgetPreferencesDataSource().setChallengeWidget(
                    appWidgetId = appWidgetId,
                    challengeId = challengeId,
                    snapshotJson = lookup.snapshotJson,
                )
                Log.d(TAG, "Refreshed challenge widget snapshot for widget $appWidgetId")
            }

            ChallengeWidgetLookup.MissingFromAccount -> {
                entryPoint.widgetPreferencesDataSource().clearWidget(appWidgetId)
                Log.d(TAG, "Cleared challenge widget $appWidgetId; challenge is not on this account")
            }

            ChallengeWidgetLookup.Unavailable -> Unit
        }
        return resolved
    }

    private companion object {
        const val TAG = "ChallengeWidget"
    }
}

@SuppressLint("RestrictedApi")
@Composable
internal fun ChallengeWidgetContent(
    snapshot: ChallengeWidgetSnapshot?,
    configureText: String,
    versusText: String,
    progressText: String,
    daysLeftText: String,
    leaderText: String,
    leaderDescription: String,
    statusText: String,
    openAppAction: androidx.glance.action.Action,
) {
    if (snapshot == null) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(HabitSurface)
                .clickable(openAppAction)
                .padding(16.dp),
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            Text(
                text = configureText,
                style = TextStyle(color = ColorProvider(HabitOnSurface)),
            )
        }
        return
    }
    val accent = parseHabitColor(snapshot.habitColorHex)
    WidgetScreen(
        headerTitle = snapshot.habitName,
        headerSubtitle = versusText,
        openAppAction = openAppAction,
    ) {
        Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
            Image(
                provider = ImageProvider(R.drawable.ic_trophy),
                contentDescription = leaderDescription,
                modifier = GlanceModifier.size(16.dp),
                colorFilter = ColorFilter.tint(ColorProvider(accent)),
            )
            Text(
                text = leaderText,
                modifier = GlanceModifier.padding(start = 6.dp),
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    color = ColorProvider(HabitOnSurface),
                ),
                maxLines = 1,
            )
        }
        Text(
            text = progressText,
            style = TextStyle(
                fontWeight = FontWeight.Bold,
                color = ColorProvider(HabitOnSurface),
            ),
        )
        WidgetProgressBar(progress = snapshot.myProgress, accent = accent)
        WidgetProgressBar(progress = snapshot.theirProgress, accent = accent.copy(alpha = 0.55f))
        Text(
            text = daysLeftText,
            style = TextStyle(color = ColorProvider(HabitOnSurfaceVariant)),
        )
        if (statusText.isNotBlank()) {
            Text(
                text = statusText,
                modifier = GlanceModifier.padding(top = 4.dp),
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    color = ColorProvider(accent),
                ),
            )
        }
    }
}
