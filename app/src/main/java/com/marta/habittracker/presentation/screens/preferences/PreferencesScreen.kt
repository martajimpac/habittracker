package com.marta.habittracker.presentation.screens.preferences

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
                    )
                },
                navigationIcon = {
                    HabitIconButton(
                        onClick = onBack,
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.preferences_back),
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
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            ProfileAvatar(
                avatarUrl = uiState.avatarUrl,
                displayName = uiState.displayName,
                avatarColor = uiState.avatarColor,
                size = 88.dp,
                contentDescription = stringResource(R.string.preferences_avatar_cd),
            )
            Spacer(modifier = Modifier.height(16.dp))
            HabitButton(
                text = stringResource(R.string.preferences_change_photo),
                onClick = onChangePhoto,
                enabled = !isBusy,
                variant = HabitButtonVariant.Secondary,
            )
            if (uiState.avatarUrl != null) {
                Spacer(modifier = Modifier.height(8.dp))
                HabitButton(
                    text = stringResource(R.string.preferences_remove_photo),
                    onClick = onRemovePhoto,
                    enabled = !isBusy,
                    variant = HabitButtonVariant.TextLink,
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
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
            if (uiState.isLoading) {
                Spacer(modifier = Modifier.height(16.dp))
                HabitLoadingIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }
            Spacer(modifier = Modifier.weight(1f))
            HabitButton(
                text = stringResource(R.string.preferences_delete_account),
                onClick = onDeleteAccount,
                enabled = !isBusy,
                variant = HabitButtonVariant.TextLink,
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
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

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun PreferencesContentWithPhotoPreview() {
    PreferencesContent(
        uiState = PreferencesUiState(
            displayName = "Marta",
            avatarUrl = "https://example.com/avatar.jpg",
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
