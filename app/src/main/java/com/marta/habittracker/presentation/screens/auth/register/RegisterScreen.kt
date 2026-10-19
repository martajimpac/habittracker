package com.marta.habittracker.presentation.screens.auth.register

import androidx.compose.foundation.background
import com.marta.habittracker.presentation.theme.isHabitDarkTheme
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
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
import com.marta.habittracker.presentation.theme.HabitField
import com.marta.habittracker.presentation.theme.HabitPrimary
import com.marta.habittracker.presentation.theme.HabitSurfaceVariant
import com.marta.habittracker.presentation.utils.ObserveAsEvents
import com.marta.habittracker.presentation.utils.SyncStatusBarIcons

@Composable
fun RegisterScreen(
    registerViewModel: RegisterViewModel = hiltViewModel(),
    navigateBack: () -> Unit,
    navigateToHome: () -> Unit = {},
) {
    val uiState by registerViewModel.uiState.collectAsStateWithLifecycle()

    ObserveAsEvents(flow = registerViewModel.navigateToHome) {
        navigateToHome()
    }

    RegisterContent(
        uiState = uiState,
        onBack = navigateBack,
        onNameChanged = registerViewModel::onNameChanged,
        onEmailChanged = registerViewModel::onEmailChanged,
        onPasswordChanged = registerViewModel::onPasswordChanged,
        onTogglePasswordVisibility = registerViewModel::onTogglePasswordVisibility,
        onTermsChecked = registerViewModel::onTermsChecked,
        onRegisterClicked = registerViewModel::onRegisterClicked,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterContent(
    uiState: RegisterUiState,
    onBack: () -> Unit,
    onNameChanged: (String) -> Unit,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onTermsChecked: (Boolean) -> Unit,
    onRegisterClicked: () -> Unit,
) {
    SyncStatusBarIcons(lightBackground = !isHabitDarkTheme())
    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        HabitIconButton(
                            onClick = onBack,
                            icon = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.register_back),
                        )
                    },
                    title = {},
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                )
            },
            containerColor = MaterialTheme.colorScheme.surface,
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
            ) {
                Text(
                    text = stringResource(R.string.register_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.register_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(24.dp))

                CustomTextField(
                    label = stringResource(R.string.register_name_label),
                    value = uiState.name,
                    onValueChange = onNameChanged,
                    placeholder = stringResource(R.string.register_name_placeholder),
                )

                Spacer(modifier = Modifier.height(16.dp))

                CustomTextField(
                    label = stringResource(R.string.register_email_label),
                    value = uiState.email,
                    onValueChange = onEmailChanged,
                    placeholder = stringResource(R.string.register_email_placeholder),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                )

                Spacer(modifier = Modifier.height(16.dp))

                CustomTextField(
                    label = stringResource(R.string.register_password_label),
                    value = uiState.password,
                    onValueChange = onPasswordChanged,
                    placeholder = stringResource(R.string.register_password_placeholder),
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
                    isError = uiState.passwordErrorRes != null,
                    supportingText = uiState.passwordErrorRes?.let { stringResource(it) }.orEmpty(),
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(HabitSurfaceVariant)
                        .clickable { onTermsChecked(!uiState.termsAccepted) }
                        .padding(12.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (uiState.termsAccepted) HabitPrimary else HabitField),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (uiState.termsAccepted) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.size(12.dp))
                    Text(
                        text = buildAnnotatedString {
                            append(stringResource(R.string.register_terms_prefix))
                            append(" ")
                            withStyle(
                                SpanStyle(
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    textDecoration = TextDecoration.Underline,
                                ),
                            ) {
                                append(stringResource(R.string.register_terms_of_service))
                            }
                            append(" ")
                            append(stringResource(R.string.register_terms_and))
                            append(" ")
                            withStyle(
                                SpanStyle(
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    textDecoration = TextDecoration.Underline,
                                ),
                            ) {
                                append(stringResource(R.string.register_privacy_policy))
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                uiState.errorMessageRes?.let { messageRes ->
                    Text(
                        text = stringResource(messageRes),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                HabitButton(
                    text = stringResource(
                        if (uiState.isLoading) {
                            R.string.register_creating_account
                        } else {
                            R.string.register_create_account
                        }
                    ),
                    onClick = onRegisterClicked,
                    enabled = uiState.isRegisterEnabled && !uiState.isLoading,
                    loading = uiState.isLoading,
                    variant = HabitButtonVariant.Primary,
                )

                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        if (uiState.isLoading) {
            HabitLoadingIndicator(modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun RegisterContentPreview() {
    RegisterContent(
        uiState = RegisterUiState(),
        onBack = {},
        onNameChanged = {},
        onEmailChanged = {},
        onPasswordChanged = {},
        onTogglePasswordVisibility = {},
        onTermsChecked = {},
        onRegisterClicked = {},
    )
}
