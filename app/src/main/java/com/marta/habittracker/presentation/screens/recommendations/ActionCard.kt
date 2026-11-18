package com.marta.habittracker.presentation.screens.recommendations

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.marta.habittracker.R
import com.marta.habittracker.core.toJava
import com.marta.habittracker.domain.model.AssistantAction
import com.marta.habittracker.domain.model.AssistantActionStatus
import com.marta.habittracker.domain.model.ChallengeCriteria
import com.marta.habittracker.domain.model.ChallengeHabitDraft
import com.marta.habittracker.domain.model.ChallengeProposal
import com.marta.habittracker.domain.model.HabitSchedule
import com.marta.habittracker.domain.model.Profile
import com.marta.habittracker.presentation.components.HabitButton
import com.marta.habittracker.presentation.components.HabitButtonVariant
import com.marta.habittracker.presentation.components.HabitLineIcon
import com.marta.habittracker.presentation.components.ProfileAvatar
import com.marta.habittracker.presentation.theme.BloomTheme
import com.marta.habittracker.presentation.utils.parseHabitColor
import java.time.format.DateTimeFormatter
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate

private val CardShape = RoundedCornerShape(16.dp)

/**
 * Card for an action Bloom proposed. While [status] is pending it shows the confirm and cancel
 * buttons; [saving] disables them. Afterwards it shows the outcome as text. A sent challenge also
 * offers [onOpenChallenge] to open its detail.
 */
@Composable
fun ActionCard(
    action: AssistantAction,
    status: AssistantActionStatus,
    saving: Boolean,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenChallenge: (String) -> Unit = {},
) {
    val scheme = MaterialTheme.colorScheme
    val statusText = statusText(action, status, saving)
    val description = when (action) {
        is AssistantAction.CreateHabit -> stringResource(
            R.string.chat_action_cd_create,
            action.name,
            frequencyText(action.schedule, action.daysOfWeek, action.daysPerWeek),
            statusText,
        )
        is AssistantAction.SetCompletion -> stringResource(
            R.string.chat_action_cd_completion,
            completionLabel(action.completed),
            action.habitName,
            dayText(action.date),
            statusText,
        )
        is AssistantAction.CreateChallenge -> stringResource(
            R.string.chat_challenge_cd,
            action.friend.displayName,
            action.proposal.draft.name,
            frequencyText(action.proposal.draft),
            criteriaText(action.proposal.criteria),
            stringResource(R.string.friends_duration_days, action.proposal.durationDays),
            statusText,
        )
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = CardShape,
        color = scheme.surfaceContainer,
        contentColor = scheme.onSurface,
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // The body is read as one sentence; the buttons stay separate so each can be activated.
            Column(modifier = Modifier.clearAndSetSemantics { contentDescription = description }) {
                when (action) {
                    is AssistantAction.CreateHabit -> CreateBody(action)
                    is AssistantAction.SetCompletion -> CompletionBody(action)
                    is AssistantAction.CreateChallenge -> ChallengeBody(action.friend, action.proposal)
                }
                if (status != AssistantActionStatus.Pending || saving) {
                    StatusLine(text = statusText, status = status, saving = saving)
                }
            }
            if (status == AssistantActionStatus.Pending) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    HabitButton(
                        text = stringResource(R.string.chat_action_cancel),
                        onClick = onCancel,
                        modifier = Modifier.weight(1f),
                        enabled = !saving,
                        variant = HabitButtonVariant.Secondary,
                        compact = true,
                    )
                    HabitButton(
                        text = confirmLabel(action),
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        enabled = !saving,
                        loading = saving,
                        compact = true,
                    )
                }
            }
            if (action is AssistantAction.CreateChallenge && status == AssistantActionStatus.Done) {
                HabitButton(
                    text = stringResource(R.string.chat_challenge_open),
                    onClick = { onOpenChallenge(action.challengeId) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    variant = HabitButtonVariant.Secondary,
                    compact = true,
                )
            }
        }
    }
}

@Composable
private fun ChallengeBody(friend: Profile, proposal: ChallengeProposal) {
    Text(
        text = stringResource(R.string.chat_challenge_title),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Row(
        modifier = Modifier.padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProfileAvatar(
            avatarUrl = friend.avatarUrl,
            displayName = friend.displayName,
            avatarColor = friend.avatarColor,
            size = 32.dp,
        )
        Text(
            text = friend.displayName,
            modifier = Modifier.padding(start = 10.dp),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
    }
    HabitRow(
        icon = proposal.draft.icon,
        colorHex = proposal.draft.colorHex,
        title = proposal.draft.name,
        subtitle = frequencyText(proposal.draft),
    )
    Text(
        text = stringResource(
            R.string.chat_challenge_terms,
            criteriaText(proposal.criteria),
            stringResource(R.string.friends_duration_days, proposal.durationDays),
        ),
        modifier = Modifier.padding(top = 8.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun criteriaText(criteria: ChallengeCriteria): String = stringResource(
    when (criteria) {
        ChallengeCriteria.Streak -> R.string.friends_criteria_streak
        ChallengeCriteria.AllDays -> R.string.friends_criteria_all_days
        ChallengeCriteria.CompletionPct -> R.string.friends_criteria_completion
    },
)

@Composable
private fun frequencyText(draft: ChallengeHabitDraft): String = frequencyText(
    schedule = when (draft.scheduleMode) {
        "weekdays" -> HabitSchedule.WEEKDAYS
        "weekly_count" -> HabitSchedule.WEEKLY_COUNT
        else -> HabitSchedule.DAILY
    },
    days = draft.daysOfWeek,
    daysPerWeek = draft.daysPerWeek,
)

@Composable
private fun CreateBody(action: AssistantAction.CreateHabit) {
    Text(
        text = stringResource(R.string.chat_action_create_title),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    HabitRow(
        icon = action.icon,
        colorHex = action.colorHex,
        title = action.name,
        subtitle = frequencyText(action.schedule, action.daysOfWeek, action.daysPerWeek),
    )
    action.description?.let { description ->
        Text(
            text = description,
            modifier = Modifier.padding(top = 6.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CompletionBody(action: AssistantAction.SetCompletion) {
    Text(
        text = completionLabel(action.completed),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    HabitRow(
        icon = action.habitIcon,
        colorHex = action.habitColorHex,
        title = action.habitName,
        subtitle = dayText(action.date),
    )
}

@Composable
private fun HabitRow(icon: String, colorHex: String, title: String, subtitle: String) {
    val color = parseHabitColor(colorHex)
    Row(
        modifier = Modifier.padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(color.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            HabitLineIcon(iconKey = icon, tint = color)
        }
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StatusLine(text: String, status: AssistantActionStatus, saving: Boolean) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.padding(top = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!saving) {
            Icon(
                imageVector = if (status == AssistantActionStatus.Done) Icons.Rounded.Check else Icons.Rounded.Close,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = if (status == AssistantActionStatus.Done) scheme.primary else scheme.onSurfaceVariant,
            )
        }
        Text(
            text = text,
            modifier = Modifier.padding(start = if (saving) 0.dp else 6.dp),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (status == AssistantActionStatus.Done) scheme.primary else scheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun statusText(action: AssistantAction, status: AssistantActionStatus, saving: Boolean): String = when {
    saving -> stringResource(R.string.chat_action_saving)
    status == AssistantActionStatus.Pending -> stringResource(R.string.chat_action_status_pending)
    status == AssistantActionStatus.Cancelled -> stringResource(R.string.chat_action_status_cancelled)
    action is AssistantAction.CreateHabit -> stringResource(R.string.chat_action_status_created)
    action is AssistantAction.CreateChallenge -> stringResource(R.string.chat_challenge_status_sent)
    action is AssistantAction.SetCompletion && action.completed -> stringResource(R.string.chat_action_status_marked)
    else -> stringResource(R.string.chat_action_status_unmarked)
}

@Composable
private fun confirmLabel(action: AssistantAction): String = when (action) {
    is AssistantAction.CreateHabit -> stringResource(R.string.chat_action_create)
    is AssistantAction.SetCompletion -> completionLabel(action.completed)
    is AssistantAction.CreateChallenge -> stringResource(R.string.chat_challenge_confirm)
}

@Composable
private fun completionLabel(completed: Boolean): String =
    stringResource(if (completed) R.string.chat_action_mark_done else R.string.chat_action_unmark)

@Composable
private fun frequencyText(schedule: HabitSchedule, days: Set<DayOfWeek>, daysPerWeek: Int): String = when (schedule) {
    HabitSchedule.DAILY -> stringResource(R.string.frequency_daily)
    HabitSchedule.WEEKLY_COUNT -> pluralStringResource(
        R.plurals.frequency_days_per_week,
        daysPerWeek.coerceIn(1, 7),
        daysPerWeek.coerceIn(1, 7),
    )
    HabitSchedule.WEEKDAYS -> listOf(
        DayOfWeek.MONDAY to stringResource(R.string.activity_day_mon),
        DayOfWeek.TUESDAY to stringResource(R.string.activity_day_tue),
        DayOfWeek.WEDNESDAY to stringResource(R.string.activity_day_wed),
        DayOfWeek.THURSDAY to stringResource(R.string.activity_day_thu),
        DayOfWeek.FRIDAY to stringResource(R.string.activity_day_fri),
        DayOfWeek.SATURDAY to stringResource(R.string.activity_day_sat),
        DayOfWeek.SUNDAY to stringResource(R.string.activity_day_sun),
    ).filter { (day, _) -> day in days }
        .joinToString(" · ") { it.second }
}

/** Weekday and date in the app language, for example «jueves, 12 nov». */
@Composable
private fun dayText(date: LocalDate): String {
    val locale = LocalConfiguration.current.locales[0]
    return date.toJava()
        .format(DateTimeFormatter.ofPattern("EEEE, d MMM", locale))
        .replaceFirstChar { it.titlecase(locale) }
}

@Preview(showBackground = true)
@Composable
private fun ActionCardCreatePreview() {
    BloomTheme {
        ActionCard(
            action = AssistantAction.CreateHabit(
                habitId = "p",
                name = "Leer 20 minutos",
                description = "Antes de dormir",
                icon = "menu_book",
                colorHex = "#294536",
                schedule = HabitSchedule.WEEKDAYS,
                daysOfWeek = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
                daysPerWeek = 7,
            ),
            status = AssistantActionStatus.Pending,
            saving = false,
            onConfirm = {},
            onCancel = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ActionCardCompletionDonePreview() {
    BloomTheme {
        ActionCard(
            action = AssistantAction.SetCompletion(
                habitId = "p",
                habitName = "Leer",
                habitIcon = "menu_book",
                habitColorHex = "#FF7775",
                date = LocalDate(2026, 11, 11),
                completed = true,
            ),
            status = AssistantActionStatus.Done,
            saving = false,
            onConfirm = {},
            onCancel = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ActionCardChallengePreview() {
    BloomTheme {
        ActionCard(
            action = AssistantAction.CreateChallenge(
                challengeId = "c",
                habitId = "h",
                friend = Profile(id = "f", username = "laura", displayName = "Laura", avatarColor = "#FF7775"),
                proposal = ChallengeProposal(
                    friendQuery = "Laura",
                    draft = ChallengeHabitDraft(
                        name = "Meditar 10 minutos",
                        icon = "menu_book",
                        colorHex = "#294536",
                        daysOfWeek = DayOfWeek.entries.toSet(),
                        scheduleMode = "daily",
                    ),
                    criteria = ChallengeCriteria.Streak,
                    durationDays = 14,
                ),
            ),
            status = AssistantActionStatus.Done,
            saving = false,
            onConfirm = {},
            onCancel = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
