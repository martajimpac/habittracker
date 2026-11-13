package com.marta.habittracker.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.marta.habittracker.R
import com.marta.habittracker.domain.model.ChallengeCard
import com.marta.habittracker.domain.model.ChallengeLeader
import com.marta.habittracker.domain.model.challengeLeader
import com.marta.habittracker.presentation.theme.HabitOutline
import com.marta.habittracker.presentation.theme.HabitPrimary

private val VersusCardShape = RoundedCornerShape(24.dp)
private val ParticipantAvatarSize = 56.dp

/** One side of a challenge, ready to draw. */
data class ChallengeParticipantUi(
    val name: String,
    val avatarUrl: String?,
    val avatarColor: String,
    val score: Int,
)

/**
 * Challenge card: habit name and icon, both participants face to face around a large "VS",
 * one progress bar per participant on a shared scale, and the days remaining.
 */
@Composable
fun ChallengeVersusCard(
    card: ChallengeCard,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val me = ChallengeParticipantUi(
        name = card.me?.displayName?.takeIf { it.isNotBlank() }
            ?: stringResource(R.string.friends_me),
        avatarUrl = card.me?.avatarUrl,
        avatarColor = card.me?.avatarColor.orEmpty(),
        score = card.myScore,
    )
    val opponent = ChallengeParticipantUi(
        name = card.opponent.displayName,
        avatarUrl = card.opponent.avatarUrl,
        avatarColor = card.opponent.avatarColor,
        score = card.theirScore,
    )
    ChallengeVersusCardContent(
        habitName = card.habitName,
        habitIcon = card.habitIcon,
        accent = card.habitColorHex.toComposeColor(),
        me = me,
        opponent = opponent,
        leader = challengeLeader(card.myProgress, card.theirProgress),
        scoreTotal = card.scoreTotal,
        daysLeft = card.daysLeft,
        modifier = modifier,
        onClick = onClick,
    )
}

@Composable
fun ChallengeVersusCardContent(
    habitName: String,
    habitIcon: String,
    accent: Color,
    me: ChallengeParticipantUi,
    opponent: ChallengeParticipantUi,
    leader: ChallengeLeader,
    scoreTotal: Int,
    daysLeft: Int,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .clip(VersusCardShape)
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .border(1.dp, HabitPrimary.copy(alpha = 0.08f), VersusCardShape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp),
    ) {
        ChallengeHeader(habitName = habitName, habitIcon = habitIcon, accent = accent)

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ChallengeParticipant(
                participant = me,
                isLeader = leader == ChallengeLeader.Me,
                accent = accent,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.friends_challenge_vs),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 0.dp),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = accent,
            )
            ChallengeParticipant(
                participant = opponent,
                isLeader = leader == ChallengeLeader.Opponent,
                accent = accent,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(16.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ChallengeScoreBar(
                name = me.name,
                score = me.score,
                total = scoreTotal,
                accent = accent,
                emphasized = leader != ChallengeLeader.Opponent,
            )
            ChallengeScoreBar(
                name = opponent.name,
                score = opponent.score,
                total = scoreTotal,
                accent = accent,
                emphasized = leader != ChallengeLeader.Me,
            )
        }

        Spacer(Modifier.height(14.dp))

        ChallengeDaysRemaining(daysLeft = daysLeft)
    }
}

@Composable
private fun ChallengeHeader(habitName: String, habitIcon: String, accent: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(accent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            HabitLineIcon(habitIcon, tint = accent, size = 22.dp)
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = habitName,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ChallengeParticipant(
    participant: ChallengeParticipantUi,
    isLeader: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box {
            ProfileAvatar(
                avatarUrl = participant.avatarUrl,
                displayName = participant.name,
                avatarColor = participant.avatarColor,
                size = ParticipantAvatarSize,
                borderColor = if (isLeader) accent else HabitOutline,
                contentDescription = participant.name,
            )
            if (isLeader) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 4.dp, y = (-4).dp)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(accent),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_trophy),
                        contentDescription = stringResource(R.string.friends_challenge_leader),
                        modifier = Modifier.size(13.dp),
                        tint = Color.White,
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = participant.name,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Named horizontal bar with "score/total"; every bar of a card shares the same [total]. */
@Composable
fun ChallengeScoreBar(
    name: String,
    score: Int,
    total: Int,
    accent: Color,
    modifier: Modifier = Modifier,
    emphasized: Boolean = true,
) {
    val description = stringResource(R.string.friends_challenge_score_description, name, score, total)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = description },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = name,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(R.string.friends_challenge_score, score, total),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (emphasized) accent else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LinearProgressIndicator(
            progress = { challengeBarFraction(score, total) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
                .height(8.dp)
                .clip(CircleShape),
            color = if (emphasized) accent else accent.copy(alpha = 0.55f),
            trackColor = HabitOutline.copy(alpha = 0.35f),
            gapSize = 0.dp,
            drawStopIndicator = {},
        )
    }
}

@Composable
private fun ChallengeDaysRemaining(daysLeft: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Rounded.Schedule,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = if (daysLeft > 0) {
                pluralStringResource(R.plurals.friends_days_remaining, daysLeft, daysLeft)
            } else {
                stringResource(R.string.friends_challenge_last_day)
            },
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Bar fill for [score] out of [total], clamped to 0..1. */
fun challengeBarFraction(score: Int, total: Int): Float =
    if (total <= 0) 0f else (score.toFloat() / total).coerceIn(0f, 1f)
