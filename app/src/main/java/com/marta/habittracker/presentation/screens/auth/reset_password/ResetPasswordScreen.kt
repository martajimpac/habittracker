package com.marta.habittracker.presentation.screens.auth.reset_password

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.marta.habittracker.R
import com.marta.habittracker.presentation.components.CustomTextField
import com.marta.habittracker.presentation.components.HabitButton
import com.marta.habittracker.presentation.components.HabitButtonVariant
import com.marta.habittracker.presentation.utils.CollectAsEffect

@Composable
fun ResetPasswordScreen(
    navigateToLogin: () -> Unit,
    viewModel: ResetPasswordViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    CollectAsEffect(viewModel.navigateToLogin) {
        navigateToLogin()
    }

    ResetPasswordContent(
        uiState = uiState,
        onNewPasswordChanged = viewModel::onNewPasswordChanged,
        onConfirmPasswordChanged = viewModel::onConfirmPasswordChanged,
        onSubmit = viewModel::submit,
    )
}

@Composable
fun ResetPasswordContent(
    uiState: ResetPasswordUiState,
    onNewPasswordChanged: (String) -> Unit,
    onConfirmPasswordChanged: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isConfirmationVisible by remember { mutableStateOf(false) }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(R.string.reset_password_title),
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.reset_password_subtitle),
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.height(24.dp))
            CustomTextField(
                value = uiState.newPassword,
                onValueChange = onNewPasswordChanged,
                label = stringResource(R.string.reset_password_new_label),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                ),
                visualTransformation = if (isPasswordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                leadingIcon = {
                    Icon(imageVector = Icons.Outlined.Lock, contentDescription = null)
                },
                trailingIcon = {
                    IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                        Icon(
                            imageVector = if (isPasswordVisible) {
                                Icons.Outlined.VisibilityOff
                            } else {
                                Icons.Outlined.Visibility
                            },
                            contentDescription = null,
                        )
                    }
                },
            )
            Spacer(modifier = Modifier.height(16.dp))
            CustomTextField(
                value = uiState.confirmPassword,
                onValueChange = onConfirmPasswordChanged,
                label = stringResource(R.string.reset_password_confirm_label),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                ),
                visualTransformation = if (isConfirmationVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                leadingIcon = {
                    Icon(imageVector = Icons.Outlined.Lock, contentDescription = null)
                },
                trailingIcon = {
                    IconButton(onClick = { isConfirmationVisible = !isConfirmationVisible }) {
                        Icon(
                            imageVector = if (isConfirmationVisible) {
                                Icons.Outlined.VisibilityOff
                            } else {
                                Icons.Outlined.Visibility
                            },
                            contentDescription = null,
                        )
                    }
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
                        R.string.reset_password_saving
                    } else {
                        R.string.reset_password_save
                    },
                ),
                onClick = onSubmit,
                enabled = uiState.isSubmitEnabled && !uiState.isLoading,
                loading = uiState.isLoading,
                variant = HabitButtonVariant.Primary,
            )
            if (uiState.isLoading) {
                Spacer(modifier = Modifier.height(16.dp))
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }
        }
    }
}
