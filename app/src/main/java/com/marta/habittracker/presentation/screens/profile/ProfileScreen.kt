package com.marta.habittracker.presentation.screens.profile

import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.marta.habittracker.R
import com.marta.habittracker.presentation.components.HabitButton
import com.marta.habittracker.presentation.components.HabitButtonVariant
import com.marta.habittracker.presentation.components.HabitDialog
import com.marta.habittracker.presentation.components.HabitDialogTone
import com.marta.habittracker.presentation.components.ProfileAvatar
import com.marta.habittracker.domain.model.ThemeMode
import com.marta.habittracker.presentation.theme.HabitCream
import com.marta.habittracker.presentation.theme.HabitPrimaryLight
import com.marta.habittracker.presentation.theme.habitDangerColors
import com.marta.habittracker.presentation.theme.isHabitDarkTheme
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
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    var showSignOutConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    ObserveAsEvents(flow = viewModel.navigateToLogin) {
        onSignedOut()
    }

    ProfileContent(
        uiState = uiState,
        onSignOut = { showSignOutConfirm = true },
        onPreferences = onPreferences,
        onGoals = onGoals,
        themeMode = themeMode,
        onDarkModeChanged = viewModel::onDarkModeChanged,
    )

    if (showSignOutConfirm) {
        HabitDialog(
            onDismissRequest = { showSignOutConfirm = false },
            title = stringResource(R.string.profile_sign_out_title),
            message = stringResource(R.string.profile_sign_out_message),
            tone = HabitDialogTone.Negative,
            confirmText = stringResource(R.string.profile_sign_out),
            dismissText = stringResource(R.string.add_habit_cancel),
            onConfirm = {
                showSignOutConfirm = false
                viewModel.onSignOutClicked()
            },
        )
    }
}

@Composable
fun ProfileContent(
    uiState: ProfileUiState,
    onSignOut: () -> Unit = {},
    onPreferences: () -> Unit = {},
    onGoals: () -> Unit = {},
    themeMode: ThemeMode = ThemeMode.System,
    onDarkModeChanged: (Boolean) -> Unit = {},
) {
    if (!uiState.isReady) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface),
        )
        return
    }
    val darkMode = when (themeMode) {
        ThemeMode.Dark -> true
        ThemeMode.Light -> false
        ThemeMode.System -> isSystemInDarkTheme()
    }
    val menuItems = listOf(
        ProfileMenuItem(
            R.string.profile_preferences,
            Icons.Rounded.Settings,
            MaterialTheme.colorScheme.primary
        ),
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
                        else -> ({})
                    },
                )
            }

            ProfileDarkModeRow(
                checked = darkMode,
                onCheckedChange = onDarkModeChanged,
            )

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
    val primary = MaterialTheme.colorScheme.primary
    val stamp = MaterialTheme.colorScheme.primary.lighter(0.10f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(primary),
    ) {
        Image(
            painter = painterResource(R.drawable.flowers),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop,
            colorFilter = ColorFilter.tint(stamp, BlendMode.SrcIn),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ProfileAvatar(
                avatarUrl = uiState.avatarUrl,
                displayName = displayName,
                avatarColor = HabitPrimaryLight.toHex(),
                size = 80.dp,
                contentDescription = stringResource(R.string.cd_profile_avatar),
                borderColor = HabitCream,
                borderWidth = 3.dp,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = displayName.ifBlank { stringResource(R.string.user_fallback_name) },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = onPrimary,
            )

            if (uiState.username.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.profile_username, uiState.username),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = onPrimary,
                )
            }

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
                    modifier = Modifier.weight(1f),
                    value = uiState.dayStreak.toString(),
                    label = stringResource(R.string.profile_stat_day_streak),
                )
                ProfileStat(
                    modifier = Modifier.weight(1f),
                    value = uiState.completedCount.toString(),
                    label = stringResource(R.string.profile_stat_completed),
                )
                ProfileStat(
                    modifier = Modifier.weight(1f),
                    value = uiState.habitsCount.toString(),
                    label = stringResource(R.string.profile_stat_habits),
                )
            }
        }
    }
}

@Composable
private fun ProfileStat(
    value: String,
    label: String,
    modifier: Modifier,
) {
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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
            color = colors.onSurface,
        )

        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = colors.outline,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun ProfileDarkModeRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(colors.surfaceContainerLowest)
            .border(1.dp, colors.outline, RoundedCornerShape(24.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(colors.primary.copy(alpha = 0.09f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.DarkMode,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(18.dp),
            )
        }
        Text(
            text = stringResource(R.string.profile_dark_mode),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = colors.onSurface,
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = colors.primary,
                checkedThumbColor = colors.onPrimary,
            ),
        )
    }
}

@Composable
private fun ProfileSignOutRow(
    onClick: () -> Unit,
) {
    val danger = habitDangerColors()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(danger.container)
            .border(1.dp, danger.border, RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(danger.icon.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.Logout,
                contentDescription = null,
                tint = danger.icon,
                modifier = Modifier.size(18.dp),
            )
        }

        Text(
            text = stringResource(R.string.profile_sign_out),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = danger.content,
        )
    }
}

private fun Color.lighter(amount: Float): Color = Color(
    red = red + (1f - red) * amount,
    green = green + (1f - green) * amount,
    blue = blue + (1f - blue) * amount,
    alpha = alpha,
)

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun ProfileContentPreview() {
    ProfileContent(
        uiState = ProfileUiState(
            displayName = "John Doe",
            username = "johndoe",
            email = "john@example.com",
            avatarUrl = null,
            avatarColor = HabitPrimaryLight.toHex(),
            dayStreak = 7,
            completedCount = 42,
            habitsCount = 5,
            isReady = true,
        ),
        onPreferences = {},
        onGoals = {},
    )
}
