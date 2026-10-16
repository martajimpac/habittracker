package com.marta.habittracker.presentation.screens.profile

import android.R.style.Theme
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.marta.habittracker.R
import com.marta.habittracker.presentation.components.ProfileAvatar
import com.marta.habittracker.presentation.theme.HabitOnSurface
import com.marta.habittracker.presentation.theme.HabitPrimary
import com.marta.habittracker.presentation.utils.ObserveAsEvents
import com.marta.habittracker.presentation.utils.toHex

private data class ProfileMenuItem(
    val labelRes: Int,
    val icon: ImageVector,
    val tint: Color,
)

@Composable
fun ProfileScreen(
    onSignedOut: () -> Unit,
    onPreferences: () -> Unit,
    onGoals: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    ObserveAsEvents(flow = viewModel.navigateToLogin) {
        onSignedOut()
    }

    ProfileContent(
        uiState = uiState,
        onSignOut = viewModel::onSignOutClicked,
        onPreferences = onPreferences,
        onGoals = onGoals,
    )
}

@Composable
fun ProfileContent(
    uiState: ProfileUiState,
    onSignOut: () -> Unit = {},
    onPreferences: () -> Unit = {},
    onGoals: () -> Unit = {},
) {
    val menuItems = listOf(
        ProfileMenuItem(R.string.profile_notifications, Icons.Outlined.Notifications, MaterialTheme.colorScheme.primary),
        ProfileMenuItem(R.string.profile_goals, Icons.Outlined.TrackChanges, MaterialTheme.colorScheme.primary),
        ProfileMenuItem(R.string.profile_preferences, Icons.Outlined.Settings, MaterialTheme.colorScheme.primary),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        ProfileHeader(uiState = uiState)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 20.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            menuItems.forEach { item ->
                ProfileMenuRow(
                    label = stringResource(item.labelRes),
                    icon = item.icon,
                    tint = item.tint,
                    onClick = when (item.labelRes) {
                        R.string.profile_preferences -> onPreferences
                        R.string.profile_goals -> onGoals
                        else -> ({})
                    },
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            ProfileSignOutRow(onClick = onSignOut)
        }
    }
}

@Composable
private fun ProfileHeader(
    uiState: ProfileUiState,
) {
    val displayName = uiState.displayName
    val email = uiState.email
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                HabitPrimary,
            )
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(top = 8.dp, bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ProfileAvatar(
            avatarUrl = uiState.avatarUrl,
            displayName = displayName,
            avatarColor = uiState.avatarColor,
            size = 80.dp,
            contentDescription = stringResource(R.string.cd_profile_avatar),
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = displayName.ifBlank { stringResource(R.string.user_fallback_name) },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            color = onPrimary,
        )

        Text(
            text = email,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = onPrimary.copy(alpha = 0.75f),
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            ProfileStat(
                value = uiState.dayStreak.toString(),
                label = stringResource(R.string.profile_stat_day_streak),
            )
            ProfileStat(
                value = uiState.completedCount.toString(),
                label = stringResource(R.string.profile_stat_completed),
            )
            ProfileStat(
                value = uiState.habitsCount.toString(),
                label = stringResource(R.string.profile_stat_habits),
            )
        }
    }
}

@Composable
private fun ProfileStat(
    value: String,
    label: String,
) {
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            color = onPrimary,
        )
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = onPrimary.copy(alpha = 0.75f),
        )
    }
}

@Composable
private fun ProfileMenuRow(
    label: String,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(colors.surfaceContainerLowest)
            .border(1.dp, colors.outline, RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(tint.copy(alpha = 0.09f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(18.dp),
            )
        }

        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = HabitOnSurface,
        )

        Icon(
            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = colors.outline,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun ProfileSignOutRow(
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(colors.errorContainer)
            .border(1.dp, colors.error.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(colors.error.copy(alpha = 0.09f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.Logout,
                contentDescription = null,
                tint = colors.error,
                modifier = Modifier.size(18.dp),
            )
        }

        Text(
            text = stringResource(R.string.profile_sign_out),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = colors.error,
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun ProfileContentPreview() {
    ProfileContent(
        uiState = ProfileUiState(
            displayName = "John Doe",
            email = "john@example.com",
            avatarUrl = null,
            avatarColor = HabitPrimary.toHex(),
            dayStreak = 7,
            completedCount = 42,
            habitsCount = 5,
        ),
        onPreferences = {},
        onGoals = {},
    )
}
