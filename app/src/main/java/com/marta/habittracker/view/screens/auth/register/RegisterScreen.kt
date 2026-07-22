package com.marta.habittracker.view.screens.auth.register

import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.marta.habittracker.R
import com.marta.habittracker.ui.theme.HabitField
import com.marta.habittracker.ui.theme.HabitOnSurface
import com.marta.habittracker.ui.theme.HabitOnSurfaceVariant
import com.marta.habittracker.ui.theme.HabitPrimary
import com.marta.habittracker.ui.theme.HabitSurface
import com.marta.habittracker.ui.theme.HabitTermsBg
import com.marta.habittracker.ui.theme.HabitTermsText
import com.marta.habittracker.view.core.components.HabitButton
import com.marta.habittracker.view.core.components.HabitButtonVariant
import com.marta.habittracker.view.core.components.HabitIconButton
import com.marta.habittracker.view.screens.auth.login.AuthLabeledField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    registerViewModel: RegisterViewModel = hiltViewModel(),
    navigateBack: () -> Unit,
    navigateToHome: () -> Unit = {},
) {
    val uiState by registerViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        registerViewModel.navigateToHome.collect {
            navigateToHome()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        HabitIconButton(
                            onClick = navigateBack,
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.register_back),
                        )
                    },
                    title = {},
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = HabitSurface,
                    ),
                )
            },
            containerColor = HabitSurface,
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
                    color = HabitOnSurface,
                )
                Text(
                    text = stringResource(R.string.register_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = HabitOnSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(24.dp))

                AuthLabeledField(
                    label = stringResource(R.string.register_name_label),
                    value = uiState.name,
                    onValueChange = registerViewModel::onNameChanged,
                    placeholder = stringResource(R.string.register_name_placeholder),
                )

                Spacer(modifier = Modifier.height(16.dp))

                AuthLabeledField(
                    label = stringResource(R.string.register_email_label),
                    value = uiState.email,
                    onValueChange = registerViewModel::onEmailChanged,
                    placeholder = stringResource(R.string.register_email_placeholder),
                    keyboardType = KeyboardType.Email,
                )

                Spacer(modifier = Modifier.height(16.dp))

                AuthLabeledField(
                    label = stringResource(R.string.register_password_label),
                    value = uiState.password,
                    onValueChange = registerViewModel::onPasswordChanged,
                    placeholder = stringResource(R.string.register_password_placeholder),
                    trailingIcon = {
                        IconButton(onClick = registerViewModel::onTogglePasswordVisibility) {
                            Icon(
                                imageVector = if (uiState.isPasswordVisible) {
                                    Icons.Outlined.VisibilityOff
                                } else {
                                    Icons.Outlined.Visibility
                                },
                                contentDescription = null,
                                tint = HabitOnSurfaceVariant,
                            )
                        }
                    },
                    visualTransformation = if (uiState.isPasswordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    keyboardType = KeyboardType.Password,
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(HabitTermsBg)
                        .clickable { registerViewModel.onTermsChecked(!uiState.termsAccepted) }
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
                                imageVector = Icons.Outlined.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.size(12.dp))
                    Text(
                        text = buildAnnotatedString {
                            append(stringResource(R.string.register_terms_prefix))
                            append(" ")
                            withStyle(SpanStyle(color = HabitPrimary, fontWeight = FontWeight.Bold)) {
                                append(stringResource(R.string.register_terms_of_service))
                            }
                            append(" ")
                            append(stringResource(R.string.register_terms_and))
                            append(" ")
                            withStyle(SpanStyle(color = HabitPrimary, fontWeight = FontWeight.Bold)) {
                                append(stringResource(R.string.register_privacy_policy))
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = HabitTermsText,
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                uiState.errorMessage?.let { message ->
                    Text(
                        text = message,
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
                    onClick = registerViewModel::onRegisterClicked,
                    enabled = uiState.isRegisterEnabled && !uiState.isLoading,
                    loading = uiState.isLoading,
                    variant = HabitButtonVariant.Primary,
                )

                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        if (uiState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
    }
}
