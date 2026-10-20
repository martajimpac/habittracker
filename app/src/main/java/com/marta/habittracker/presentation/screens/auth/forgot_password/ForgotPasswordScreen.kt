package com.marta.habittracker.presentation.screens.auth.forgot_password

import com.marta.habittracker.presentation.theme.isHabitDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
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
import com.marta.habittracker.presentation.utils.ObserveAsEvents
import com.marta.habittracker.presentation.utils.SyncStatusBarIcons

@Composable
fun ForgotPasswordScreen(
    initialEmail: String,
    navigateBack: () -> Unit,
    navigateToLogin: () -> Unit,
    viewModel: ForgotPasswordViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(initialEmail) {
        if (initialEmail.isNotBlank() && uiState.email != initialEmail) {
            viewModel.onEmailChanged(initialEmail)
        }
    }
    ObserveAsEvents(flow = viewModel.navigateToLogin) {
        navigateToLogin()
    }

    ForgotPasswordContent(
        uiState = uiState,
        onBack = navigateBack,
        onEmailChanged = viewModel::onEmailChanged,
        onSubmit = viewModel::submit,
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ForgotPasswordContent(
    uiState: ForgotPasswordUiState,
    onBack: () -> Unit,
    onEmailChanged: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    SyncStatusBarIcons(lightBackground = !isHabitDarkTheme())
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    HabitIconButton(
                        onClick = onBack,
                        icon = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.forgot_password_back),
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
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(R.string.forgot_password_title),
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.forgot_password_subtitle),
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.height(24.dp))
            CustomTextField(
                value = uiState.email,
                onValueChange = onEmailChanged,
                label = stringResource(R.string.forgot_password_email_label),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                ),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Email,
                        contentDescription = null,
                    )
                },
            )
            Spacer(modifier = Modifier.height(16.dp))
            uiState.errorMessageRes?.let { errorRes ->
                Text(
                    text = stringResource(errorRes),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            HabitButton(
                text = stringResource(
                    if (uiState.isLoading) {
                        R.string.forgot_password_sending
                    } else {
                        R.string.forgot_password_send
                    },
                ),
                onClick = onSubmit,
                enabled = uiState.isSubmitEnabled && !uiState.isLoading,
                loading = uiState.isLoading,
                variant = HabitButtonVariant.Primary,
            )
            if (uiState.isLoading) {
                Spacer(modifier = Modifier.height(16.dp))
                HabitLoadingIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }
        }
    }
}

// @Preview(showBackground = true, showSystemUi = true)
// @Composable
// private fun ForgotPasswordContentPreview() {
    // ForgotPasswordContent(
        // uiState = ForgotPasswordUiState(),
        // onBack = {},
        // onEmailChanged = {},
        // onSubmit = {},
    // )
// }
