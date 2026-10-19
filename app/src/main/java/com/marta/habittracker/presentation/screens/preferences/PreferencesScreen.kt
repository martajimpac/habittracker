package com.marta.habittracker.presentation.screens.preferences

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.marta.habittracker.R
import com.marta.habittracker.presentation.components.CustomTextField
import com.marta.habittracker.presentation.components.HabitButton
import com.marta.habittracker.presentation.components.HabitButtonVariant
import com.marta.habittracker.presentation.components.HabitIconButton
import com.marta.habittracker.presentation.components.HabitLoadingIndicator
import com.marta.habittracker.presentation.components.ProfileAvatar
import com.marta.habittracker.presentation.theme.HabitPrimary
import com.marta.habittracker.presentation.theme.habitDangerColors
import com.marta.habittracker.presentation.utils.ObserveAsEvents
import com.marta.habittracker.presentation.utils.toHex
import kotlinx.coroutines.launch

@Composable
fun PreferencesScreen(
    onBack: () -> Unit,
    onDeleteAccount: () -> Unit,
    viewModel: PreferencesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        uri?.let(viewModel::onAvatarPicked)
    }

    ObserveAsEvents(flow = viewModel.events) { event ->
        when (event) {
            PreferencesEvent.NavigateBack -> onBack()
            is PreferencesEvent.ShowError -> {
                scope.launch {
                    snackbarHostState.showSnackbar(context.getString(event.messageRes))
                }
            }
        }
    }

    PreferencesContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onDisplayNameChanged = viewModel::onDisplayNameChanged,
        onSave = viewModel::save,
        onChangePhoto = {
            photoPicker.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
            )
        },
        onRemovePhoto = viewModel::onRemovePhoto,
        onDeleteAccount = onDeleteAccount,
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun PreferencesContent(
    uiState: PreferencesUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onDisplayNameChanged: (String) -> Unit,
    onSave: () -> Unit,
    onChangePhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
    onDeleteAccount: () -> Unit,
) {
    val isBusy = uiState.isLoading || uiState.isSaving

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.profile_preferences),
                        modifier = Modifier.padding(start = 4.dp),
                    )
                },
                navigationIcon = {
                    HabitIconButton(
                        onClick = onBack,
                        icon = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.preferences_back),
                        modifier = Modifier.padding(start = 20.dp),
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(
                modifier = Modifier.profileCard(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box {
                    ProfileAvatar(
                        avatarUrl = uiState.avatarUrl,
                        displayName = uiState.displayName,
                        avatarColor = uiState.avatarColor,
                        size = 88.dp,
                        contentDescription = stringResource(R.string.preferences_avatar_cd),
                    )
                    ChangePhotoBadge(
                        enabled = !isBusy,
                        onClick = onChangePhoto,
                        modifier = Modifier.align(Alignment.TopEnd),
                    )
                }
                if (uiState.avatarUrl != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    HabitButton(
                        text = stringResource(R.string.preferences_remove_photo),
                        onClick = onRemovePhoto,
                        enabled = !isBusy,
                        variant = HabitButtonVariant.TextLink,
                    )
                }
            }
            Column(modifier = Modifier.profileCard()) {
                CustomTextField(
                    value = uiState.displayName,
                    onValueChange = onDisplayNameChanged,
                    label = stringResource(R.string.preferences_display_name_label),
                )
                Spacer(modifier = Modifier.height(16.dp))
                HabitButton(
                    text = stringResource(
                        if (uiState.isSaving) {
                            R.string.preferences_saving
                        } else {
                            R.string.preferences_save
                        },
                    ),
                    onClick = onSave,
                    enabled = !isBusy,
                    loading = uiState.isSaving,
                    variant = HabitButtonVariant.Primary,
                )
            }
            if (uiState.isLoading) {
                HabitLoadingIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }
            DeleteAccountEntry(
                enabled = !isBusy,
                onClick = onDeleteAccount,
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ChangePhotoBadge(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .size(32.dp)
            .alpha(if (enabled) 1f else 0.4f)
            .border(2.dp, colors.surfaceContainerLowest, CircleShape)
            .clip(CircleShape)
            .background(colors.primary)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.Edit,
            contentDescription = stringResource(R.string.preferences_change_photo),
            tint = colors.onPrimary,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun Modifier.profileCard(): Modifier {
    val shape = RoundedCornerShape(24.dp)
    return this
        .fillMaxWidth()
        .clip(shape)
        .background(MaterialTheme.colorScheme.surfaceContainerLowest)
        .border(1.dp, MaterialTheme.colorScheme.outline, shape)
        .padding(16.dp)
}

@Composable
private fun DeleteAccountEntry(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val danger = habitDangerColors()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(danger.container)
            .border(1.dp, danger.border, RoundedCornerShape(24.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = Icons.Rounded.Delete,
            contentDescription = null,
            tint = danger.icon,
        )
        Text(
            text = stringResource(R.string.preferences_delete_account),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = danger.content,
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun PreferencesContentPreview() {
    PreferencesContent(
        uiState = PreferencesUiState(
            displayName = "Marta",
            avatarUrl = null,
            avatarColor = HabitPrimary.toHex(),
        ),
        snackbarHostState = remember { SnackbarHostState() },
        onBack = {},
        onDisplayNameChanged = {},
        onSave = {},
        onChangePhoto = {},
        onRemovePhoto = {},
        onDeleteAccount = {},
    )
}

