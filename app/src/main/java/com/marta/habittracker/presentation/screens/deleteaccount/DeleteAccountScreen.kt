package com.marta.habittracker.presentation.screens.deleteaccount

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
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
import com.marta.habittracker.presentation.theme.habitDangerColors
import com.marta.habittracker.presentation.utils.ObserveAsEvents
import kotlinx.coroutines.launch

@Composable
fun DeleteAccountScreen(
    onBack: () -> Unit,
    onSignedOut: () -> Unit,
    viewModel: DeleteAccountViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    BackHandler(enabled = uiState.isDeleting) { }

    ObserveAsEvents(flow = viewModel.events) { event ->
        when (event) {
            DeleteAccountEvent.AccountDeleted -> onSignedOut()
            is DeleteAccountEvent.ShowError -> {
                scope.launch {
                    snackbarHostState.showSnackbar(context.getString(event.messageRes))
                }
            }
        }
    }

    DeleteAccountContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onConfirmationChanged = viewModel::onConfirmationChanged,
        onDeleteConfirmed = viewModel::onDeleteConfirmed,
        onCancel = onBack,
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun DeleteAccountContent(
    uiState: DeleteAccountUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onConfirmationChanged: (String) -> Unit,
    onDeleteConfirmed: () -> Unit,
    onCancel: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.preferences_delete_title),
                    )
                },
                navigationIcon = {
                    HabitIconButton(
                        onClick = onBack,
                        icon = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.preferences_back),
                        enabled = !uiState.isDeleting,
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
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            val danger = habitDangerColors()
            Text(
                text = stringResource(R.string.preferences_delete_explanation),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(danger.container)
                    .border(1.dp, danger.border, RoundedCornerShape(24.dp))
                    .padding(16.dp),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = danger.content,
            )
            Spacer(modifier = Modifier.height(16.dp))
            CustomTextField(
                value = uiState.confirmation,
                onValueChange = onConfirmationChanged,
                label = stringResource(R.string.preferences_delete_confirm_label),
            )
            Spacer(modifier = Modifier.height(16.dp))
            HabitButton(
                text = stringResource(
                    if (uiState.isDeleting) {
                        R.string.preferences_delete_deleting
                    } else {
                        R.string.preferences_delete_confirm_action
                    },
                ),
                onClick = onDeleteConfirmed,
                enabled = uiState.isDeleteEnabled && !uiState.isDeleting,
                loading = uiState.isDeleting,
                variant = HabitButtonVariant.Destructive,
            )
            Spacer(modifier = Modifier.height(8.dp))
            HabitButton(
                text = stringResource(R.string.preferences_delete_cancel),
                onClick = onCancel,
                enabled = !uiState.isDeleting,
                variant = HabitButtonVariant.Secondary,
            )
        }
    }
}

// @Preview(showBackground = true, showSystemUi = true)
// @Composable
// private fun DeleteAccountContentPreview() {
    // DeleteAccountContent(
        // uiState = DeleteAccountUiState(),
        // snackbarHostState = remember { SnackbarHostState() },
        // onBack = {},
        // onConfirmationChanged = {},
        // onDeleteConfirmed = {},
        // onCancel = {},
    // )
// }

// @Preview(showBackground = true, showSystemUi = true)
// @Composable
// private fun DeleteAccountContentReadyPreview() {
    // DeleteAccountContent(
        // uiState = DeleteAccountUiState(
            // confirmation = "DELETE",
            // isDeleteEnabled = true,
        // ),
        // snackbarHostState = remember { SnackbarHostState() },
        // onBack = {},
        // onConfirmationChanged = {},
        // onDeleteConfirmed = {},
        // onCancel = {},
    // )
// }
