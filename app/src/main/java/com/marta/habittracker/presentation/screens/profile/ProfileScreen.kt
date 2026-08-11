package com.marta.habittracker.presentation.screens.profile

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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.marta.habittracker.R
import com.marta.habittracker.presentation.theme.HabitOnSurface
import com.marta.habittracker.presentation.theme.HabitSurface
import com.marta.habittracker.presentation.screens.home.userAvatarInitials
import com.marta.habittracker.presentation.utils.CollectAsEffect

private val ProfileGradientStart = Color(0xFF7C3AED)
private val ProfileGradientEnd = Color(0xFFA78BFA)
private val ProfileHeaderMuted = Color(0xFFE9D5FF)
private val ProfileMenuBorder = Color(0x146750A4)
private val ProfileChevron = Color(0xFFCAC4D0)
private val ProfileSignOutBg = Color(0xFFFFF1F0)
private val ProfileSignOutBorder = Color(0x1FB3261E)
private val ProfileSignOut = Color(0xFFB3261E)

private data class ProfileMenuItem(
    val labelRes: Int,
    val icon: ImageVector,
    val tint: Color,
)

@Composable
fun ProfileScreen(
    onSignedOut: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    CollectAsEffect(viewModel.navigateToLogin) {
        onSignedOut()
    }

    ProfileContent(
        uiState = uiState,
        onSignOut = viewModel::onSignOutClicked,
    )
}

@Composable
fun ProfileContent(
    uiState: ProfileUiState,
    onSignOut: () -> Unit = {},
) {
    val menuItems = listOf(
        ProfileMenuItem(R.string.profile_notifications, Icons.Outlined.Notifications, Color(0xFF6750A4)),
        ProfileMenuItem(R.string.profile_goals, Icons.Outlined.TrackChanges, Color(0xFF0D9488)),
        ProfileMenuItem(R.string.profile_reminders, Icons.Outlined.CalendarMonth, Color(0xFFD97706)),
        ProfileMenuItem(R.string.profile_preferences, Icons.Outlined.Settings, Color(0xFF7C3AED)),
        ProfileMenuItem(R.string.profile_achievements, Icons.Outlined.EmojiEvents, Color(0xFF059669)),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HabitSurface),
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
                    onClick = {},
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(ProfileGradientStart, ProfileGradientEnd),
                ),
            )
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(top = 8.dp, bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = userAvatarInitials(displayName),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = Color.White,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = displayName.ifBlank { stringResource(R.string.user_fallback_name) },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            color = Color.White,
        )

        Text(
            text = email,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = ProfileHeaderMuted,
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
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            color = Color.White,
        )
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = ProfileHeaderMuted,
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .border(1.dp, ProfileMenuBorder, RoundedCornerShape(24.dp))
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
            tint = ProfileChevron,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun ProfileSignOutRow(
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(ProfileSignOutBg)
            .border(1.dp, ProfileSignOutBorder, RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(ProfileSignOut.copy(alpha = 0.09f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.Logout,
                contentDescription = null,
                tint = ProfileSignOut,
                modifier = Modifier.size(18.dp),
            )
        }

        Text(
            text = stringResource(R.string.profile_sign_out),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = ProfileSignOut,
        )
    }
}
