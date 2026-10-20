package com.marta.habittracker.presentation.screens.auth.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import com.marta.habittracker.presentation.utils.SyncStatusBarIcons
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.marta.habittracker.R
import com.marta.habittracker.presentation.theme.bloomLogoRes
import com.marta.habittracker.presentation.components.CustomTextField
import com.marta.habittracker.presentation.components.HabitButton
import com.marta.habittracker.presentation.components.HabitButtonVariant
import com.marta.habittracker.presentation.components.HabitDialog
import com.marta.habittracker.presentation.components.HabitDialogTone
import com.marta.habittracker.presentation.components.HabitLoadingIndicator
import com.marta.habittracker.presentation.navigation.LoginMessage
import com.marta.habittracker.presentation.utils.ObserveAsEvents

@Composable
fun LoginScreen(
    loginViewModel: LoginViewModel = hiltViewModel(),
    navigateToRegister: () -> Unit,
    navigateToForgotPassword: (String) -> Unit,
    navigateToHome: () -> Unit,
) {
    val uiState by loginViewModel.uiState.collectAsStateWithLifecycle()
    val prompt by loginViewModel.prompt.collectAsStateWithLifecycle()
    var showAccountNotConfirmed by remember { mutableStateOf(false) }

    ObserveAsEvents(flow = loginViewModel.navigateToHome) {
        navigateToHome()
    }
    ObserveAsEvents(flow = loginViewModel.events) { event ->
        when (event) {
            LoginEvent.AccountNotConfirmed -> showAccountNotConfirmed = true
        }
    }

    LoginContent(
        uiState = uiState,
        onEmailChanged = loginViewModel::onEmailChanged,
        onPasswordChanged = loginViewModel::onPasswordChanged,
        onTogglePasswordVisibility = loginViewModel::onTogglePasswordVisibility,
        onClickSelected = loginViewModel::onClickSelected,
        onForgotPassword = { navigateToForgotPassword(uiState.email) },
        onNavigateToRegister = navigateToRegister,
        messageRes = prompt
            ?.takeUnless { it == LoginMessage.AccountCreated || it == LoginMessage.EmailConfirmed }
            ?.toStringRes(),
    )

    if (prompt == LoginMessage.AccountCreated) {
        HabitDialog(
            onDismissRequest = loginViewModel::dismissPrompt,
            title = stringResource(R.string.register_account_created_title),
            message = stringResource(R.string.register_account_created_message),
            tone = HabitDialogTone.Positive,
            confirmText = stringResource(R.string.dialog_ok),
            onConfirm = loginViewModel::dismissPrompt,
        )
    }
    if (prompt == LoginMessage.EmailConfirmed) {
        HabitDialog(
            onDismissRequest = loginViewModel::dismissPrompt,
            title = stringResource(R.string.login_email_confirmed_title),
            message = stringResource(R.string.login_email_confirmed_message),
            tone = HabitDialogTone.Positive,
            confirmText = stringResource(R.string.dialog_ok),
            onConfirm = loginViewModel::dismissPrompt,
        )
    }
    if (showAccountNotConfirmed) {
        HabitDialog(
            onDismissRequest = { showAccountNotConfirmed = false },
            title = stringResource(R.string.login_email_not_verified_title),
            message = stringResource(R.string.error_login_email_not_verified),
            tone = HabitDialogTone.Negative,
            confirmText = stringResource(R.string.dialog_ok),
            onConfirm = { showAccountNotConfirmed = false },
        )
    }
}

@Composable
fun LoginContent(
    uiState: LoginUiState,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onClickSelected: () -> Unit,
    onForgotPassword: () -> Unit,
    onNavigateToRegister: () -> Unit,
    messageRes: Int? = null,
) {
    SyncStatusBarIcons(lightBackground = false)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                MaterialTheme.colorScheme.primary,
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(28.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                    .padding(horizontal = 20.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(
                    painter = painterResource(bloomLogoRes()),
                    contentDescription = stringResource(R.string.app_name),
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = stringResource(R.string.login_welcome_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.login_welcome_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(24.dp))

                messageRes?.let { successMessageRes ->
                    Text(
                        text = stringResource(successMessageRes),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                CustomTextField(
                    label = stringResource(R.string.login_email_label),
                    value = uiState.email,
                    onValueChange = onEmailChanged,
                    placeholder = stringResource(R.string.login_email_placeholder),
                    leadingIcon = {
                        Icon(Icons.Rounded.Email, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                )

                Spacer(modifier = Modifier.height(16.dp))

                CustomTextField(
                    label = stringResource(R.string.login_password_label),
                    value = uiState.password,
                    onValueChange = onPasswordChanged,
                    placeholder = stringResource(R.string.login_password_placeholder),
                    leadingIcon = {
                        Icon(Icons.Rounded.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    trailingIcon = {
                        IconButton(onClick = onTogglePasswordVisibility) {
                            Icon(
                                imageVector = if (uiState.isPasswordVisible) {
                                    Icons.Rounded.VisibilityOff
                                } else {
                                    Icons.Rounded.Visibility
                                },
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    visualTransformation = if (uiState.isPasswordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                )

                HabitButton(
                    text = stringResource(R.string.login_forgot_password),
                    onClick = onForgotPassword,
                    modifier = Modifier.align(Alignment.End),
                    variant = HabitButtonVariant.TextLink,
                    fillMaxWidth = false,
                )

                Spacer(modifier = Modifier.height(8.dp))

                uiState.errorMessageRes?.let { messageRes ->
                    Text(
                        text = stringResource(messageRes),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                HabitButton(
                    text = stringResource(
                        if (uiState.isLoading) R.string.login_signing_in else R.string.login_sign_in
                    ),
                    onClick = onClickSelected,
                    enabled = uiState.isLoginEnabled && !uiState.isLoading,
                    loading = uiState.isLoading,
                    variant = HabitButtonVariant.Primary,
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = stringResource(R.string.login_no_account),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(R.string.login_sign_up),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .clickable(onClick = onNavigateToRegister),
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        if (uiState.isLoading) {
            HabitLoadingIndicator(modifier = Modifier.align(Alignment.Center))
        }
    }
}

private fun LoginMessage.toStringRes(): Int = when (this) {
    LoginMessage.PasswordResetRequestSent -> R.string.login_password_reset_request_sent
    LoginMessage.PasswordUpdated -> R.string.login_password_updated
    LoginMessage.ResetLinkInvalid -> R.string.login_password_reset_link_invalid
    LoginMessage.AccountCreated -> R.string.register_account_created_message
    LoginMessage.EmailConfirmed -> R.string.login_email_confirmed_message
}

// @Preview(showBackground = true, showSystemUi = true)
// @Composable
// private fun LoginContentPreview() {
    // LoginContent(
        // uiState = LoginUiState(),
        // onEmailChanged = {},
        // onPasswordChanged = {},
        // onTogglePasswordVisibility = {},
        // onClickSelected = {},
        // onForgotPassword = {},
        // onNavigateToRegister = {},
    // )
// }
