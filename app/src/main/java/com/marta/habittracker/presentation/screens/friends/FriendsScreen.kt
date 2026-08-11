package com.marta.habittracker.presentation.screens.friends

import android.graphics.Color.parseColor
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.People
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.marta.habittracker.R
import com.marta.habittracker.domain.model.ChallengeCard
import com.marta.habittracker.domain.model.ChallengeCriteria
import com.marta.habittracker.domain.model.FriendListItem
import com.marta.habittracker.domain.model.Friendship
import com.marta.habittracker.domain.model.Habit
import com.marta.habittracker.domain.model.Profile
import com.marta.habittracker.presentation.components.HabitLineIcon
import com.marta.habittracker.presentation.theme.HabitOnSurface
import com.marta.habittracker.presentation.theme.HabitOnSurfaceVariant
import com.marta.habittracker.presentation.theme.HabitPrimary
import com.marta.habittracker.presentation.theme.HabitPrimaryLight

private val FriendsBackground = Color(0xFFF6F0FF)

@Composable
fun FriendsScreen(
    viewModel: FriendsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refresh()
    }
    FriendsContent(
        uiState = uiState,
        onAddFriend = viewModel::onAddFriendClicked,
        onDismissSheet = viewModel::onDismissSheet,
        onSearchQueryChanged = viewModel::onSearchQueryChanged,
        onSendFriendRequest = viewModel::sendFriendRequest,
        onRespondToRequest = viewModel::respondToRequest,
        onViewFriend = viewModel::onViewFriendClicked,
        onCreateChallenge = viewModel::onCreateChallengeClicked,
        onSendChallenge = viewModel::createChallenge,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsContent(
    uiState: FriendsUiState,
    onAddFriend: () -> Unit = {},
    onDismissSheet: () -> Unit = {},
    onSearchQueryChanged: (String) -> Unit = {},
    onSendFriendRequest: (String) -> Unit = {},
    onRespondToRequest: (String, Boolean) -> Unit = { _, _ -> },
    onViewFriend: (String) -> Unit = {},
    onCreateChallenge: (String) -> Unit = {},
    onSendChallenge: (String, String, String, ChallengeCriteria, Int) -> Unit = { _, _, _, _, _ -> },
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FriendsBackground),
    ) {
        FriendsHeader(onAddFriend = onAddFriend)
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = HabitPrimary)
            }
        } else {
            FriendsBody(
                uiState = uiState,
                onRespondToRequest = onRespondToRequest,
                onViewFriend = onViewFriend,
                onCreateChallenge = onCreateChallenge,
            )
        }
    }

    when (val sheet = uiState.sheet) {
        FriendsSheet.None -> Unit
        FriendsSheet.AddFriend -> ModalBottomSheet(onDismissRequest = onDismissSheet) {
            AddFriendSheet(
                uiState = uiState,
                onQueryChanged = onSearchQueryChanged,
                onSendRequest = onSendFriendRequest,
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
                friendHabits = uiState.selectedFriendHabits,
                myHabits = uiState.myHabits,
                onSendChallenge = { myHabit, friendHabit, criteria, duration ->
                    onSendChallenge(sheet.friendId, myHabit.id, friendHabit.id, criteria, duration)
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
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.friends_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = HabitOnSurface,
        )
        FriendsAddButton(onClick = onAddFriend)
    }
}

@Composable
private fun FriendsBody(
    uiState: FriendsUiState,
    onRespondToRequest: (String, Boolean) -> Unit,
    onViewFriend: (String) -> Unit,
    onCreateChallenge: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 20.dp, end = 20.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            FriendsSectionTitle(R.string.friends_active_challenges)
            Spacer(Modifier.height(10.dp))
            if (uiState.challenges.isEmpty()) {
                FriendsInlineEmpty(R.string.friends_empty_challenges)
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(uiState.challenges, key = { it.challenge.id }) { ChallengeCard(it) }
                }
            }
        }

        if (uiState.pendingRequests.isNotEmpty()) {
            item {
                FriendsSectionTitle(R.string.friends_pending_requests)
                Spacer(Modifier.height(10.dp))
                uiState.pendingRequests.forEach { request ->
                    PendingRequestRow(request = request, onRespondToRequest = onRespondToRequest)
                    Spacer(Modifier.height(8.dp))
                }
            }
        }

        item {
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

@Composable
private fun ChallengeCard(card: ChallengeCard) {
    val cardColor = card.habitColorHex.toComposeColor()
    Column(
        modifier = Modifier
            .width(220.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(cardColor, HabitPrimaryLight)))
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            HabitLineIcon(card.habitIcon, tint = Color.White, size = 20.dp)
            Spacer(Modifier.width(8.dp))
            Text(
                text = card.habitName,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                fontWeight = FontWeight.Black,
            )
        }
        Spacer(Modifier.height(16.dp))
        ChallengeProgress(stringResource(R.string.friends_me), card.myProgress)
        Spacer(Modifier.height(8.dp))
        ChallengeProgress(card.opponent.displayName, card.theirProgress)
        Spacer(Modifier.height(14.dp))
        Text(
            text = stringResource(R.string.friends_days_left, card.daysLeft),
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun ChallengeProgress(label: String, progress: Int) {
    Text(label, style = MaterialTheme.typography.labelMedium, color = Color.White)
    LinearProgressIndicator(
        progress = { progress.coerceIn(0, 100) / 100f },
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        color = Color.White,
        trackColor = Color.White.copy(alpha = 0.28f),
    )
}

@Composable
private fun PendingRequestRow(
    request: Friendship,
    onRespondToRequest: (String, Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape).background(HabitPrimaryLight),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.People, null, tint = Color.White)
        }
        Text(
            text = stringResource(R.string.friends_pending_request),
            modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = HabitOnSurface,
        )
        TextButton(onClick = { onRespondToRequest(request.id, false) }) {
            Text(stringResource(R.string.friends_decline))
        }
        Button(onClick = { onRespondToRequest(request.id, true) }) {
            Text(stringResource(R.string.friends_accept))
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
            .background(Color.White)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(profile = profile)
        Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(profile.displayName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black, color = HabitOnSurface)
            Text(
                text = stringResource(R.string.friends_username, profile.username),
                style = MaterialTheme.typography.bodySmall,
                color = HabitOnSurfaceVariant,
            )
            Text(
                text = if (friend.publicHabitsCount > 0) {
                    stringResource(R.string.friends_public_habits, friend.publicHabitsCount)
                } else {
                    stringResource(R.string.friends_private_habits)
                },
                style = MaterialTheme.typography.labelSmall,
                color = HabitOnSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.friends_best_streak, friend.bestStreak),
                style = MaterialTheme.typography.labelSmall,
                color = HabitPrimary,
                fontWeight = FontWeight.Bold,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            TextButton(onClick = onViewFriend) { Text(stringResource(R.string.friends_view)) }
            TextButton(onClick = onCreateChallenge) { Text(stringResource(R.string.friends_challenge)) }
        }
    }
}

@Composable
private fun Avatar(profile: Profile) {
    Box(
        modifier = Modifier.size(48.dp).clip(CircleShape).background(profile.avatarColor.toComposeColor()),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = profile.displayName.split(" ").filter(String::isNotBlank).take(2).joinToString("") { it.first().uppercase() },
            style = MaterialTheme.typography.titleSmall,
            color = Color.White,
            fontWeight = FontWeight.Black,
        )
    }
}

@Composable
private fun AddFriendSheet(
    uiState: FriendsUiState,
    onQueryChanged: (String) -> Unit,
    onSendRequest: (String) -> Unit,
) {
    SheetColumn(titleRes = R.string.friends_add_friend) {
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = onQueryChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.friends_search_label)) },
            placeholder = { Text(stringResource(R.string.friends_search_hint)) },
            singleLine = true,
        )
        if (uiState.requestSent) {
            Text(stringResource(R.string.friends_request_sent), color = HabitPrimary, fontWeight = FontWeight.Bold)
        }
        uiState.searchResults.forEach { profile ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Avatar(profile)
                Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(profile.displayName, fontWeight = FontWeight.Bold, color = HabitOnSurface)
                    Text(stringResource(R.string.friends_username, profile.username), color = HabitOnSurfaceVariant)
                }
                Button(onClick = { onSendRequest(profile.id) }) {
                    Text(stringResource(R.string.friends_send_request))
                }
            }
        }
    }
}

@Composable
private fun FriendHabitsSheet(friend: Profile?, habits: List<Habit>) {
    SheetColumn(titleRes = R.string.friends_public_habits_title) {
        if (friend != null) {
            Text(friend.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = HabitOnSurface)
        }
        if (habits.isEmpty()) {
            FriendsInlineEmpty(R.string.friends_no_public_habits)
        } else {
            habits.forEach { HabitSheetRow(it) }
        }
    }
}

@Composable
private fun ChallengeSheet(
    friend: Profile?,
    friendHabits: List<Habit>,
    myHabits: List<Habit>,
    onSendChallenge: (Habit, Habit, ChallengeCriteria, Int) -> Unit,
) {
    var selectedFriendHabit by remember(friendHabits) { mutableStateOf(friendHabits.firstOrNull()) }
    var selectedMyHabit by remember(myHabits, selectedFriendHabit) {
        mutableStateOf(myHabits.firstOrNull { it.name.equals(selectedFriendHabit?.name, ignoreCase = true) } ?: myHabits.firstOrNull())
    }
    var duration by remember { mutableIntStateOf(7) }
    var criteria by remember { mutableStateOf(ChallengeCriteria.CompletionPct) }

    SheetColumn(titleRes = R.string.friends_create_challenge) {
        if (friend != null) {
            Text(stringResource(R.string.friends_challenge_with, friend.displayName), color = HabitOnSurfaceVariant)
        }
        Text(stringResource(R.string.friends_their_habit), fontWeight = FontWeight.Bold, color = HabitOnSurface)
        HabitPicker(friendHabits, selectedFriendHabit, { selectedFriendHabit = it })
        Text(stringResource(R.string.friends_my_habit), fontWeight = FontWeight.Bold, color = HabitOnSurface)
        HabitPicker(myHabits, selectedMyHabit, { selectedMyHabit = it })
        Text(stringResource(R.string.friends_duration), fontWeight = FontWeight.Bold, color = HabitOnSurface)
        ChoiceRow(listOf(3, 7, 14, 30), duration, { duration = it }) {
            stringResource(R.string.friends_duration_days, it)
        }
        Text(stringResource(R.string.friends_criteria), fontWeight = FontWeight.Bold, color = HabitOnSurface)
        val criteriaOptions = listOf(ChallengeCriteria.Streak, ChallengeCriteria.AllDays, ChallengeCriteria.CompletionPct)
        ChoiceRow(criteriaOptions, criteria, { criteria = it }) { criterion ->
            stringResource(
                when (criterion) {
                    ChallengeCriteria.Streak -> R.string.friends_criteria_streak
                    ChallengeCriteria.AllDays -> R.string.friends_criteria_all_days
                    ChallengeCriteria.CompletionPct -> R.string.friends_criteria_completion
                },
            )
        }
        Button(
            onClick = { onSendChallenge(selectedMyHabit!!, selectedFriendHabit!!, criteria, duration) },
            enabled = selectedMyHabit != null && selectedFriendHabit != null,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.friends_send_challenge))
        }
    }
}

@Composable
private fun HabitPicker(habits: List<Habit>, selectedHabit: Habit?, onSelected: (Habit) -> Unit) {
    if (habits.isEmpty()) {
        FriendsInlineEmpty(R.string.friends_no_public_habits)
    } else {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(habits, key = { it.id }) { habit ->
                Text(
                    text = habit.name,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (habit == selectedHabit) HabitPrimary else HabitPrimaryLight.copy(alpha = 0.2f))
                        .clickable { onSelected(habit) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    color = if (habit == selectedHabit) Color.White else HabitOnSurface,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun <T> ChoiceRow(options: List<T>, selected: T, onSelected: (T) -> Unit, label: @Composable (T) -> String) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { option ->
            Text(
                text = label(option),
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (option == selected) HabitPrimary else HabitPrimaryLight.copy(alpha = 0.2f))
                    .clickable { onSelected(option) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                color = if (option == selected) Color.White else HabitOnSurface,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun HabitSheetRow(habit: Habit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        HabitLineIcon(habit, tint = habit.colorHex.toComposeColor())
        Text(habit.name, modifier = Modifier.padding(start = 12.dp), fontWeight = FontWeight.Bold, color = HabitOnSurface)
    }
}

@Composable
private fun SheetColumn(titleRes: Int, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().heightIn(max = 620.dp).padding(horizontal = 24.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = {
            Text(stringResource(titleRes), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = HabitOnSurface)
            HorizontalDivider()
            content()
            Spacer(Modifier.height(16.dp))
        },
    )
}

@Composable
private fun FriendsEmptyState() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Outlined.People, null, tint = HabitPrimary, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.friends_empty_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = HabitOnSurface)
        Text(
            stringResource(R.string.friends_empty_message),
            style = MaterialTheme.typography.bodyMedium,
            color = HabitOnSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun FriendsInlineEmpty(messageRes: Int) {
    Text(
        text = stringResource(messageRes),
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.72f)).padding(16.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = HabitOnSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun FriendsSectionTitle(titleRes: Int) {
    Text(stringResource(titleRes), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = HabitOnSurface)
}

@Composable
private fun FriendsAddButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(listOf(HabitPrimary, HabitPrimaryLight)))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(Icons.Filled.Add, null, tint = Color.White, modifier = Modifier.size(18.dp))
        Text(stringResource(R.string.friends_add), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

private fun String.toComposeColor(): Color = runCatching { Color(parseColor(this)) }.getOrDefault(HabitPrimary)
