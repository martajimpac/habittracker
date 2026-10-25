package com.marta.habittracker.presentation.screens.friends

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.marta.habittracker.R
import com.marta.habittracker.domain.model.ChallengeCard
import com.marta.habittracker.domain.model.challengeLeaderLabel
import com.marta.habittracker.domain.model.ChallengeCriteria
import com.marta.habittracker.domain.model.ChallengeHabitDraft
import com.marta.habittracker.domain.model.HabitSchedule
import com.marta.habittracker.domain.model.storedValue
import com.marta.habittracker.domain.model.FriendListItem
import com.marta.habittracker.domain.model.PendingFriendRequest
import com.marta.habittracker.domain.model.Habit
import com.marta.habittracker.domain.model.Profile
import com.marta.habittracker.presentation.components.DaysPerWeekStepper
import com.marta.habittracker.presentation.components.HabitButton
import com.marta.habittracker.presentation.components.HabitButtonVariant
import com.marta.habittracker.presentation.components.HabitDialog
import com.marta.habittracker.presentation.components.HabitDialogTone
import com.marta.habittracker.presentation.components.HabitIconButton
import com.marta.habittracker.presentation.theme.HabitTopBarHorizontalPadding
import com.marta.habittracker.presentation.components.HabitLoadingIndicator
import com.marta.habittracker.presentation.components.HabitIconKeys
import com.marta.habittracker.presentation.components.HabitIconOptions
import com.marta.habittracker.presentation.components.HabitLineIcon
import com.marta.habittracker.presentation.components.ProfileAvatar
import com.marta.habittracker.presentation.components.toComposeColor
import kotlinx.datetime.DayOfWeek
import com.marta.habittracker.presentation.theme.HabitField
import com.marta.habittracker.presentation.theme.HabitOnSurface
import com.marta.habittracker.presentation.theme.HabitOutline
import com.marta.habittracker.presentation.theme.HabitPrimary
import com.marta.habittracker.presentation.theme.HabitPrimaryLight
import com.marta.habittracker.presentation.utils.HabitColorOptions
import com.marta.habittracker.presentation.utils.ObserveAsEvents
import com.marta.habittracker.presentation.utils.toHex


@Composable
fun FriendsScreen(
    onOpenChallenge: (String) -> Unit = {},
    viewModel: FriendsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showRequestSent by remember { mutableStateOf(false) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refresh()
    }
    ObserveAsEvents(flow = viewModel.events) { event ->
        when (event) {
            FriendsEvent.RequestSent -> showRequestSent = true
        }
    }
    FriendsContent(
        uiState = uiState,
        onOpenChallenge = onOpenChallenge,
        onAddFriend = viewModel::onAddFriendClicked,
        onDismissSheet = viewModel::onDismissSheet,
        onSearchQueryChanged = viewModel::onSearchQueryChanged,
        onSendFriendRequest = viewModel::sendFriendRequest,
        onRespondToRequest = viewModel::respondToRequest,
        onViewFriend = viewModel::onViewFriendClicked,
        onCreateChallenge = viewModel::onCreateChallengeClicked,
        onSendChallenge = viewModel::createChallenge,
    )
    if (showRequestSent) {
        HabitDialog(
            onDismissRequest = { showRequestSent = false },
            title = stringResource(R.string.friends_request_sent),
            confirmText = stringResource(R.string.dialog_ok),
            tone = HabitDialogTone.Positive,
            onConfirm = { showRequestSent = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsContent(
    uiState: FriendsUiState,
    onOpenChallenge: (String) -> Unit = {},
    onAddFriend: () -> Unit = {},
    onDismissSheet: () -> Unit = {},
    onSearchQueryChanged: (String) -> Unit = {},
    onSendFriendRequest: (String) -> Unit = {},
    onRespondToRequest: (String, Boolean) -> Unit = { _, _ -> },
    onViewFriend: (String) -> Unit = {},
    onCreateChallenge: (String) -> Unit = {},
    onSendChallenge: (String, ChallengeHabitDraft, ChallengeCriteria, Int) -> Unit = { _, _, _, _ -> },
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        FriendsHeader(onAddFriend = onAddFriend)
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                HabitLoadingIndicator()
            }
        } else {
            FriendsBody(
                uiState = uiState,
                onOpenChallenge = onOpenChallenge,
                onRespondToRequest = onRespondToRequest,
                onViewFriend = onViewFriend,
                onCreateChallenge = onCreateChallenge,
            )
        }
    }

    when (val sheet = uiState.sheet) {
        FriendsSheet.None -> Unit
        FriendsSheet.AddFriend -> ModalBottomSheet(
            onDismissRequest = onDismissSheet,
            dragHandle = null,
        ) {
            AddFriendSheet(
                uiState = uiState,
                onQueryChanged = onSearchQueryChanged,
                onSendRequest = onSendFriendRequest,
                onClose = onDismissSheet,
            )
        }

        is FriendsSheet.ViewFriend -> ModalBottomSheet(onDismissRequest = onDismissSheet) {
            FriendHabitsSheet(
                friend = uiState.friends.firstOrNull { it.profile.id == sheet.friendId }?.profile,
                habits = uiState.selectedFriendHabits,
            )
        }

        is FriendsSheet.CreateChallenge -> ModalBottomSheet(onDismissRequest = onDismissSheet) {
            ChallengeSheet(
                friend = uiState.friends.firstOrNull { it.profile.id == sheet.friendId }?.profile,
                onSendChallenge = { draft, criteria, duration ->
                    onSendChallenge(sheet.friendId, draft, criteria, duration)
                },
            )
        }
    }
}

@Composable
private fun FriendsHeader(onAddFriend: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = HabitTopBarHorizontalPadding, vertical = 16.dp)
            .heightIn(min = 44.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.friends_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurface,
        )
        FriendsAddButton(onClick = onAddFriend)
    }
}

@Composable
private fun FriendsBody(
    uiState: FriendsUiState,
    onOpenChallenge: (String) -> Unit,
    onRespondToRequest: (String, Boolean) -> Unit,
    onViewFriend: (String) -> Unit,
    onCreateChallenge: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(Modifier.padding(horizontal = 20.dp)) {
                FriendsSectionTitle(R.string.friends_active_challenges)
                Spacer(Modifier.height(10.dp))
                if (uiState.challenges.isEmpty()) {
                    FriendsInlineEmpty(R.string.friends_empty_challenges)
                }
            }
            if (uiState.challenges.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(uiState.challenges, key = { it.challenge.id }) { card ->
                        ChallengeCard(
                            card = card,
                            onClick = { onOpenChallenge(card.challenge.id) },
                        )
                    }
                }
            }
        }

        if (uiState.pendingRequests.isNotEmpty()) {
            item {
                Column(Modifier.padding(horizontal = 20.dp)) {
                    FriendsSectionTitle(R.string.friends_pending_requests)
                    Spacer(Modifier.height(10.dp))
                    uiState.pendingRequests.forEach { request ->
                        PendingRequestRow(
                            request = request,
                            onRespondToRequest = onRespondToRequest
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }

        item {
            Column(Modifier.padding(horizontal = 20.dp)) {
                FriendsSectionTitle(R.string.friends_list_title)
                Spacer(Modifier.height(10.dp))
                if (uiState.friends.isEmpty()) {
                    FriendsEmptyState()
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        uiState.friends.forEach { friend ->
                            FriendRow(
                                friend = friend,
                                onViewFriend = { onViewFriend(friend.profile.id) },
                                onCreateChallenge = { onCreateChallenge(friend.profile.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChallengeCard(
    card: ChallengeCard,
    onClick: () -> Unit = {},
) {
    val accent = card.habitColorHex.toComposeColor()
    Column(
        modifier = Modifier
            .width(220.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .border(1.dp, HabitPrimary.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accent.copy(alpha = 0.09f)),
                contentAlignment = Alignment.Center,
            ) {
                HabitLineIcon(card.habitIcon, tint = accent, size = 22.dp)
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = card.habitName,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(R.drawable.ic_trophy),
                contentDescription = stringResource(R.string.friends_challenge_leader),
                modifier = Modifier.size(16.dp),
                tint = accent,
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = challengeLeaderLabel(
                    myProgress = card.myProgress,
                    theirProgress = card.theirProgress,
                    meLabel = stringResource(R.string.friends_me),
                    opponentName = card.opponent.displayName,
                    tieLabel = stringResource(R.string.friends_challenge_tie),
                ),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelLarge,
                color = accent,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }
        Spacer(Modifier.height(12.dp))
        ChallengeProgress(stringResource(R.string.friends_me), card.myProgress, accent)
        Spacer(Modifier.height(8.dp))
        ChallengeProgress(card.opponent.displayName, card.theirProgress, accent)
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.friends_days_left, card.daysLeft),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun ChallengeProgress(label: String, progress: Int, accent: Color) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
    LinearProgressIndicator(
        progress = { progress.coerceIn(0, 100) / 100f },
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
            .clip(CircleShape),
        color = accent,
        trackColor = HabitOutline.copy(alpha = 0.35f),
        drawStopIndicator = {},
    )
}

@Composable
private fun PendingRequestRow(
    request: PendingFriendRequest,
    onRespondToRequest: (String, Boolean) -> Unit,
) {
    val profile = request.requester
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProfileAvatar(
            avatarUrl = profile.avatarUrl,
            displayName = profile.displayName,
            avatarColor = profile.avatarColor,
            size = 48.dp,
            contentDescription = stringResource(R.string.cd_profile_avatar),
        )
        Text(
            text = profile.displayName,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            HabitIconButton(
                onClick = { onRespondToRequest(request.id, true) },
                icon = Icons.Rounded.Check,
                contentDescription = stringResource(R.string.friends_accept),
            )
            HabitIconButton(
                onClick = { onRespondToRequest(request.id, false) },
                icon = Icons.Rounded.Close,
                contentDescription = stringResource(R.string.friends_decline),
                backgroundColor = HabitField,
                iconTint = HabitOnSurface,
                borderColor = HabitOutline,
            )
        }
    }
}

@Composable
private fun FriendRow(
    friend: FriendListItem,
    onViewFriend: () -> Unit,
    onCreateChallenge: () -> Unit,
) {
    val profile = friend.profile
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .clickable(onClick = onViewFriend),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProfileAvatar(
                avatarUrl = profile.avatarUrl,
                displayName = profile.displayName,
                avatarColor = profile.avatarColor,
                size = 48.dp,
                contentDescription = stringResource(R.string.cd_profile_avatar),
            )
            Column(modifier = Modifier.padding(horizontal = 12.dp)) {
                Text(
                    profile.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.friends_username, profile.username),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = if (friend.publicHabitsCount > 0) {
                        stringResource(R.string.friends_public_habits, friend.publicHabitsCount)
                    } else {
                        stringResource(R.string.friends_private_habits)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.friends_best_streak, friend.bestStreak),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        HabitIconButton(
            onClick = onCreateChallenge,
            icon = ImageVector.vectorResource(R.drawable.ic_swords),
            contentDescription = stringResource(R.string.friends_challenge),
        )
    }
}

@Composable
private fun AddFriendSheet(
    uiState: FriendsUiState,
    onQueryChanged: (String) -> Unit,
    onSendRequest: (String) -> Unit,
    onClose: () -> Unit,
) {

    SheetColumn(
        titleRes = R.string.friends_add_friend,
        showDivider = false,
        onClose = onClose,
    ) {
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = onQueryChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.friends_search_label)) },
            placeholder = { Text(stringResource(R.string.friends_search_hint)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                )
            },
            singleLine = true,
        )
        uiState.searchResults.forEach { profile ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ProfileAvatar(
                    avatarUrl = profile.avatarUrl,
                    displayName = profile.displayName,
                    avatarColor = profile.avatarColor,
                    size = 48.dp,
                    contentDescription = stringResource(R.string.cd_profile_avatar),
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                ) {
                    Text(
                        profile.displayName,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        stringResource(R.string.friends_username, profile.username),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                HabitButton(
                    text = stringResource(R.string.friends_send_request),
                    onClick = { onSendRequest(profile.id) },
                    fillMaxWidth = false,
                    compact = true,
                )
            }
        }
    }
}

@Composable
private fun FriendHabitsSheet(friend: Profile?, habits: List<Habit>) {
    SheetColumn(titleRes = R.string.friends_public_habits_title) {
        if (friend != null) {
            Text(
                friend.displayName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        if (habits.isEmpty()) {
            FriendsInlineEmpty(R.string.friends_no_public_habits)
        } else {
            habits.forEach { HabitSheetRow(it) }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FriendsHabitsSheet() {
    FriendHabitsSheet(
        friend = Profile(
            id = "1",
            username = "johndoe",
            displayName = "John Doe",
            avatarUrl = null,
            avatarColor = "6200EE",
        ),
        habits = listOf(
        )
    )
}

@Composable
private fun ChallengeSheet(
    friend: Profile?,
    onSendChallenge: (ChallengeHabitDraft, ChallengeCriteria, Int) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var showMissing by remember { mutableStateOf(false) }
    var icon by remember { mutableStateOf(HabitIconKeys.DEFAULT) }
    var color by remember { mutableStateOf(HabitColorOptions.first()) }
    var daysPerWeek by remember { mutableIntStateOf(7) }
    var schedule by remember { mutableStateOf(HabitSchedule.DAILY) }
    var selectedDays by remember { mutableStateOf(setOf<DayOfWeek>()) }
    var duration by remember { mutableIntStateOf(7) }
    var criteria by remember { mutableStateOf(ChallengeCriteria.CompletionPct) }
    var showCriteriaHelp by remember { mutableStateOf(false) }

    SheetColumn(titleRes = R.string.friends_create_challenge, addHorizontalPadding = false) {
        Column(
            modifier = Modifier
                .heightIn(max = 480.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (friend != null) {
                Text(
                    stringResource(R.string.friends_challenge_with, friend.displayName),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                label = { Text(stringResource(R.string.add_habit_name_label)) },
                singleLine = true,
                isError = showMissing && name.isBlank(),
                supportingText = if (showMissing && name.isBlank()) {
                    { Text(stringResource(R.string.add_habit_error_name_required)) }
                } else {
                    null
                },
            )
            Text(
                stringResource(
                    R.string.add_habit_choose_icon
                ),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                items(HabitIconOptions, key = { it }) { option ->
                    val selected = option == icon
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) color else HabitField)
                            .clickable { icon = option },
                        contentAlignment = Alignment.Center,
                    ) {
                        HabitLineIcon(
                            iconKey = option,
                            tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            size = 22.dp,
                        )
                    }
                }
            }
            Text(
                stringResource(R.string.add_habit_choose_color),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                items(HabitColorOptions, key = { it.toHex() }) { option ->
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(option)
                            .clickable { color = option },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (option == color) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }
            Text(
                stringResource(R.string.add_habit_frequency),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(
                    HabitSchedule.DAILY to R.string.frequency_daily,
                    HabitSchedule.WEEKDAYS to R.string.frequency_weekdays,
                    HabitSchedule.WEEKLY_COUNT to R.string.frequency_weekly_count,
                ).forEach { (mode, label) ->
                    val selected = schedule == mode
                    Text(
                        text = stringResource(label),
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary else HabitPrimaryLight.copy(
                                    alpha = 0.2f
                                ),
                            )
                            .clickable { schedule = mode }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            if (schedule == HabitSchedule.WEEKDAYS) {
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DayOfWeek.entries.forEach { day ->
                        val selected = day in selectedDays
                        Text(
                            text = stringResource(challengeWeekdayLabel(day)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primary else HabitPrimaryLight.copy(
                                        alpha = 0.2f
                                    ),
                                )
                                .clickable {
                                    selectedDays =
                                        if (selected) selectedDays - day else selectedDays + day
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
            if (schedule == HabitSchedule.WEEKLY_COUNT) {
                DaysPerWeekStepper(
                    days = daysPerWeek,
                    onDaysChange = { daysPerWeek = it },
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
            Text(
                stringResource(R.string.friends_duration),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp),
                color = MaterialTheme.colorScheme.onSurface
            )
            ChoiceRow(listOf(3, 7, 14, 30), duration, { duration = it }) {
                stringResource(R.string.friends_duration_days, it)
            }
            Row(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    stringResource(R.string.friends_criteria),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable { showCriteriaHelp = true },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.HelpOutline,
                        contentDescription = stringResource(R.string.friends_criteria_help),
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            val criteriaOptions = listOf(
                ChallengeCriteria.Streak, ChallengeCriteria.AllDays, ChallengeCriteria.CompletionPct
            )
            ChoiceRow(criteriaOptions, criteria, { criteria = it }) { criterion ->
                stringResource(
                    when (criterion) {
                        ChallengeCriteria.Streak -> R.string.friends_criteria_streak
                        ChallengeCriteria.AllDays -> R.string.friends_criteria_all_days
                        ChallengeCriteria.CompletionPct -> R.string.friends_criteria_completion
                    },
                )
            }
            HabitButton(
                text = stringResource(R.string.friends_send_challenge),
                onClick = {
                    if (name.isBlank() || (schedule == HabitSchedule.WEEKDAYS && selectedDays.isEmpty())) {
                        showMissing = true
                    } else {
                        onSendChallenge(
                            ChallengeHabitDraft(
                                name = name,
                                icon = icon,
                                colorHex = color.toHex(),
                                daysOfWeek = if (schedule == HabitSchedule.WEEKDAYS) {
                                    selectedDays
                                } else {
                                    DayOfWeek.entries.toSet()
                                },
                                daysPerWeek = if (schedule == HabitSchedule.WEEKLY_COUNT) daysPerWeek else 7,
                                scheduleMode = schedule.storedValue(),
                            ),
                            criteria,
                            duration,
                        )
                    }
                },
                enabled = name.isNotBlank() &&
                        (schedule != HabitSchedule.WEEKDAYS || selectedDays.isNotEmpty()),
                clickableWhenDisabled = true,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }
        if (showCriteriaHelp) {
            HabitDialog(
                onDismissRequest = { showCriteriaHelp = false },
                title = stringResource(R.string.friends_criteria),
                message = stringResource(R.string.friends_criteria_help_message),
                confirmText = stringResource(R.string.dialog_ok),
                onConfirm = { showCriteriaHelp = false },
            )
        }
    }
}

@Composable
private fun <T> ChoiceRow(
    options: List<T>, selected: T, onSelected: (T) -> Unit, label: @Composable (T) -> String
) {
    Row(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { option ->
            Text(
                text = label(option),
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (option == selected) MaterialTheme.colorScheme.primary else HabitPrimaryLight.copy(
                            alpha = 0.2f
                        )
                    )
                    .clickable { onSelected(option) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                color = if (option == selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun HabitSheetRow(habit: Habit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HabitLineIcon(habit, tint = habit.colorHex.toComposeColor())
        Text(
            habit.name,
            modifier = Modifier.padding(start = 12.dp),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SheetColumn(
    titleRes: Int,
    showDivider: Boolean = true,
    addHorizontalPadding: Boolean = true,
    onClose: (() -> Unit)? = null,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 620.dp)
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = HabitTopBarHorizontalPadding),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(titleRes),
                    modifier = Modifier
                        .weight(1f),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (onClose != null) {
                    HabitIconButton(
                        onClick = onClose,
                        icon = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.friends_sheet_close),
                    )
                }
            }
            if (showDivider) {
                HorizontalDivider(Modifier.padding(horizontal = 20.dp))
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (addHorizontalPadding) {
                            Modifier.padding(horizontal = 20.dp)
                        } else {
                            Modifier
                        }
                    ),
                content = content,
            )

            Spacer(Modifier.height(16.dp))
        },
    )
}

@Composable
private fun FriendsEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 36.dp, horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Rounded.People,
            null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(40.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.friends_empty_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            stringResource(R.string.friends_empty_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun FriendsInlineEmpty(messageRes: Int) {
    Text(
        text = stringResource(messageRes),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.72f))
            .padding(16.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun FriendsSectionTitle(titleRes: Int) {
    Text(
        stringResource(titleRes),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Black,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun FriendsAddButton(onClick: () -> Unit) {
    HabitButton(
        text = stringResource(R.string.friends_add),
        onClick = onClick,
        fillMaxWidth = false,
        compact = true,
        icon = Icons.Rounded.Add,
    )
}

private fun challengeWeekdayLabel(day: DayOfWeek): Int = when (day) {
    DayOfWeek.MONDAY -> R.string.activity_day_mon
    DayOfWeek.TUESDAY -> R.string.activity_day_tue
    DayOfWeek.WEDNESDAY -> R.string.activity_day_wed
    DayOfWeek.THURSDAY -> R.string.activity_day_thu
    DayOfWeek.FRIDAY -> R.string.activity_day_fri
    DayOfWeek.SATURDAY -> R.string.activity_day_sat
    DayOfWeek.SUNDAY -> R.string.activity_day_sun
}

// @Preview(showBackground = true)
// @Composable
// private fun AddFriendSheetPreview() {
// ChallengeSheet(
// friend = null,
// onSendChallenge = { _, _, _ -> },
// )
// }