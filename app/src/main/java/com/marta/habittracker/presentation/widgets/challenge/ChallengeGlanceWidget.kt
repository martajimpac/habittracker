package com.marta.habittracker.presentation.widgets.challenge

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.ColorFilter
import androidx.glance.currentState
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.marta.habittracker.R
import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.model.ChallengeCard
import com.marta.habittracker.domain.widget.ChallengeWidgetLookup
import com.marta.habittracker.domain.widget.resolveChallengeWidgetSnapshot
import com.marta.habittracker.presentation.components.habitIconRes
import com.marta.habittracker.presentation.components.toComposeColor
import com.marta.habittracker.presentation.theme.HabitCream
import com.marta.habittracker.presentation.theme.HabitOnSurface
import com.marta.habittracker.presentation.theme.HabitOnSurfaceVariant
import com.marta.habittracker.presentation.theme.HabitOutline
import com.marta.habittracker.presentation.theme.HabitPrimaryLight
import com.marta.habittracker.presentation.theme.HabitSurface
import com.marta.habittracker.presentation.utils.parseHabitColor
import com.marta.habittracker.presentation.widgets.WidgetEntryPoint
import com.marta.habittracker.presentation.widgets.WidgetLaunchExtras
import com.marta.habittracker.presentation.widgets.WidgetLoad
import com.marta.habittracker.presentation.widgets.WidgetLoading
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
    // Added for the card design. Defaults keep snapshots saved by older versions readable;
    // they are filled on the next refresh with network.
    val habitIcon: String = "",
    val myDisplayName: String = "",
    val myAvatarColor: String = "",
    val opponentAvatarColor: String = "",
    val myScore: Int = 0,
    val theirScore: Int = 0,
    val scoreTotal: Int = 0,
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
        habitIcon = habitIcon,
        myDisplayName = me?.displayName.orEmpty(),
        myAvatarColor = me?.avatarColor.orEmpty(),
        opponentAvatarColor = opponent.avatarColor,
        myScore = myScore,
        theirScore = theirScore,
        scoreTotal = scoreTotal,
    )

class ChallengeGlanceWidget : GlanceAppWidget() {

    // Full card when there is room; compact card (no avatars / VS) on small widgets.
    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(ChallengeWidgetSizes.Compact, ChallengeWidgetSizes.Full),
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val json = entryPoint.json()
        provideContent {
            // Bumped by the config activity so a running session reloads the chosen challenge.
            val configRevision = currentState<Preferences>()[configRevisionKey]
            val state by produceState<WidgetLoad<ChallengeWidgetSnapshot?>>(
                WidgetLoad.Loading,
                appWidgetId,
                configRevision,
            ) {
                value = loadChallengeSnapshot(
                    appWidgetId = appWidgetId,
                    entryPoint = entryPoint,
                    json = json,
                )
            }
            when (val current = state) {
                WidgetLoad.Loading -> WidgetLoading(context.getString(R.string.widget_loading))
                is WidgetLoad.Ready -> ChallengeWidgetContent(
                    snapshot = current.value,
                    strings = ChallengeWidgetCardStrings(
                        me = context.getString(R.string.friends_me),
                        initialsFallback = context.getString(R.string.user_avatar_initials_fallback),
                        pending = context.getString(R.string.challenge_widget_status_pending),
                        lastDay = context.getString(R.string.friends_challenge_last_day),
                        daysRemaining = { days ->
                            context.resources.getQuantityString(R.plurals.friends_days_remaining, days, days)
                        },
                        score = { score, total -> context.getString(R.string.friends_challenge_score, score, total) },
                    ),
                    configureText = context.getString(R.string.challenge_widget_configure),
                    vsText = context.getString(R.string.friends_challenge_vs),
                    leaderDescription = context.getString(R.string.friends_challenge_leader),
                    scoreDescription = { name, score, total ->
                        context.getString(R.string.friends_challenge_score_description, name, score, total)
                    },
                    openAppAction = actionStartActivity(WidgetLaunchExtras.openFriendsIntent(context)),
                    configureAction = actionStartActivity(
                        ChallengeWidgetConfigActivity.configureIntent(context, appWidgetId),
                    ),
                )
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

    companion object {
        private const val TAG = "ChallengeWidget"

        /** Glance state key the config activity bumps after saving a challenge. */
        val configRevisionKey = longPreferencesKey("challenge_config_revision")
    }
}

@Composable
internal fun ChallengeWidgetContent(
    snapshot: ChallengeWidgetSnapshot?,
    strings: ChallengeWidgetCardStrings,
    configureText: String,
    vsText: String,
    leaderDescription: String,
    scoreDescription: (name: String, score: Int, total: Int) -> String,
    openAppAction: Action,
    configureAction: Action,
) {
    if (snapshot == null) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(HabitSurface)
                .clickable(configureAction)
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
    val card = snapshot.toCardUi(strings)
    val accent = parseHabitColor(card.habitColorHex)
    val size = LocalSize.current
    val full = size.width >= ChallengeWidgetSizes.Full.width && size.height >= ChallengeWidgetSizes.Full.height

    // Same look as the app's ChallengeVersusCard: light rounded card, no green header.
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .cornerRadius(24.dp)
            .background(ColorProvider(HabitCream))
            .clickable(openAppAction)
            .padding(12.dp),
    ) {
        ChallengeWidgetHeader(
            habitName = card.habitName,
            habitIcon = card.habitIcon,
            accent = accent,
            full = full,
        )
        if (full) {
            Spacer(GlanceModifier.height(8.dp))
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.Vertical.CenterVertically,
            ) {
                ChallengeWidgetParticipant(
                    participant = card.me,
                    accent = accent,
                    leaderDescription = leaderDescription,
                    modifier = GlanceModifier.defaultWeight(),
                )
                Text(
                    text = vsText,
                    modifier = GlanceModifier.padding(horizontal = 6.dp),
                    style = TextStyle(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorProvider(accent),
                    ),
                )
                ChallengeWidgetParticipant(
                    participant = card.opponent,
                    accent = accent,
                    leaderDescription = leaderDescription,
                    modifier = GlanceModifier.defaultWeight(),
                )
            }
        }
        Spacer(GlanceModifier.height(8.dp))
        ChallengeWidgetScoreBar(card.me, accent, scoreDescription)
        Spacer(GlanceModifier.height(6.dp))
        ChallengeWidgetScoreBar(card.opponent, accent, scoreDescription)
        Spacer(GlanceModifier.defaultWeight())
        ChallengeWidgetFooter(daysText = card.daysText, pendingText = card.pendingText, accent = accent)
    }
}

/** Widget sizes the challenge card switches between (RF-07). */
internal object ChallengeWidgetSizes {
    val Compact = DpSize(110.dp, 110.dp)
    val Full = DpSize(220.dp, 220.dp)
}

@Composable
private fun ChallengeWidgetHeader(habitName: String, habitIcon: String, accent: Color, full: Boolean) {
    val tile = if (full) 32.dp else 28.dp
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        Box(
            modifier = GlanceModifier
                .size(tile)
                .cornerRadius(10.dp)
                .background(ColorProvider(accent.copy(alpha = 0.12f))),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                provider = ImageProvider(habitIconRes(habitIcon)),
                contentDescription = null,
                modifier = GlanceModifier.size(if (full) 18.dp else 16.dp),
                colorFilter = ColorFilter.tint(ColorProvider(accent)),
            )
        }
        Text(
            text = habitName,
            modifier = GlanceModifier.defaultWeight().padding(start = 10.dp),
            style = TextStyle(
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = ColorProvider(HabitOnSurface),
            ),
            maxLines = if (full) 2 else 1,
        )
    }
}

@Composable
private fun ChallengeWidgetParticipant(
    participant: ChallengeWidgetParticipantUi,
    accent: Color,
    leaderDescription: String,
    modifier: GlanceModifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
    ) {
        // Glance has no offset: the trophy badge is a second layer aligned to the top end.
        Box(modifier = GlanceModifier.size(48.dp)) {
            Box(
                modifier = GlanceModifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = GlanceModifier
                        .size(42.dp)
                        .background(
                            imageProvider = ImageProvider(R.drawable.widget_check_circle),
                            colorFilter = ColorFilter.tint(
                                ColorProvider(if (participant.isLeader) accent else HabitOutline),
                            ),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = GlanceModifier
                            .size(38.dp)
                            .background(
                                imageProvider = ImageProvider(R.drawable.widget_check_circle),
                                colorFilter = ColorFilter.tint(ColorProvider(widgetAvatarColor(participant.avatarColorHex))),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = participant.initials,
                            style = TextStyle(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColorProvider(HabitCream),
                            ),
                            maxLines = 1,
                        )
                    }
                }
            }
            if (participant.isLeader) {
                Box(
                    modifier = GlanceModifier.fillMaxSize(),
                    contentAlignment = Alignment.TopEnd,
                ) {
                    Box(
                        modifier = GlanceModifier
                            .size(18.dp)
                            .background(
                                imageProvider = ImageProvider(R.drawable.widget_check_circle),
                                colorFilter = ColorFilter.tint(ColorProvider(accent)),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            provider = ImageProvider(R.drawable.ic_trophy),
                            contentDescription = leaderDescription,
                            modifier = GlanceModifier.size(11.dp),
                            colorFilter = ColorFilter.tint(ColorProvider(Color.White)),
                        )
                    }
                }
            }
        }
        Text(
            text = participant.name,
            modifier = GlanceModifier.padding(top = 2.dp),
            style = TextStyle(
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = ColorProvider(HabitOnSurface),
                textAlign = TextAlign.Center,
            ),
            maxLines = 1,
        )
    }
}

@Composable
private fun ChallengeWidgetScoreBar(
    participant: ChallengeWidgetParticipantUi,
    accent: Color,
    scoreDescription: (name: String, score: Int, total: Int) -> String,
) {
    val description = scoreDescription(participant.name, participant.score, participant.scoreTotal)
    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .semantics { contentDescription = description },
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            Text(
                text = participant.name,
                modifier = GlanceModifier.defaultWeight(),
                style = TextStyle(fontSize = 12.sp, color = ColorProvider(HabitOnSurface)),
                maxLines = 1,
            )
            Text(
                text = participant.scoreText,
                style = TextStyle(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorProvider(if (participant.emphasized) accent else HabitOnSurfaceVariant),
                ),
                maxLines = 1,
            )
        }
        LinearProgressIndicator(
            progress = participant.barFraction,
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(top = 3.dp)
                .height(8.dp)
                .cornerRadius(4.dp),
            color = ColorProvider(if (participant.emphasized) accent else accent.copy(alpha = 0.55f)),
            backgroundColor = ColorProvider(HabitOutline.copy(alpha = 0.35f)),
        )
    }
}

@Composable
private fun ChallengeWidgetFooter(daysText: String, pendingText: String?, accent: Color) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        Image(
            provider = ImageProvider(R.drawable.ic_widget_schedule),
            contentDescription = null,
            modifier = GlanceModifier.size(14.dp),
            colorFilter = ColorFilter.tint(ColorProvider(HabitOnSurfaceVariant)),
        )
        Text(
            text = daysText,
            modifier = GlanceModifier.defaultWeight().padding(start = 6.dp),
            style = TextStyle(
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = ColorProvider(HabitOnSurfaceVariant),
            ),
            maxLines = 1,
        )
        if (pendingText != null) {
            Text(
                text = pendingText,
                style = TextStyle(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorProvider(accent),
                ),
                maxLines = 1,
            )
        }
    }
}

/** Initials sit on the participant's avatar color; without one, on the app's default initials color. */
private fun widgetAvatarColor(hex: String): Color =
    if (hex.isBlank()) HabitPrimaryLight else hex.toComposeColor()
