package com.marta.habittracker.view.screens.auth.login

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.marta.habittracker.R
import com.marta.habittracker.ui.theme.HabitField
import com.marta.habittracker.ui.theme.HabitOnSurface
import com.marta.habittracker.ui.theme.HabitOnSurfaceVariant
import com.marta.habittracker.ui.theme.HabitPrimary
import com.marta.habittracker.ui.theme.HabitPrimaryLight
import com.marta.habittracker.ui.theme.HabitSurface

@Composable
fun LoginScreen(
    loginViewModel: LoginViewModel = hiltViewModel(),
    navigateToRegister: () -> Unit,
    navigateToHome: () -> Unit,
) {
    val uiState by loginViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isUserLogged) {
        if (uiState.isUserLogged) {
            navigateToHome()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(HabitSurface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(HabitPrimary),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "🎯", fontSize = 28.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.login_welcome_title),
                style = MaterialTheme.typography.headlineMedium,
                color = HabitOnSurface,
            )
            Text(
                text = stringResource(R.string.login_welcome_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = HabitOnSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(32.dp))

            AuthLabeledField(
                label = stringResource(R.string.login_email_label),
                value = uiState.email,
                onValueChange = loginViewModel::onEmailChanged,
                placeholder = stringResource(R.string.login_email_placeholder),
                leadingIcon = {
                    Icon(Icons.Outlined.Email, contentDescription = null, tint = HabitOnSurfaceVariant)
                },
                keyboardType = KeyboardType.Email,
            )

            Spacer(modifier = Modifier.height(16.dp))

            AuthLabeledField(
                label = stringResource(R.string.login_password_label),
                value = uiState.password,
                onValueChange = loginViewModel::onPasswordChanged,
                placeholder = stringResource(R.string.login_password_placeholder),
                leadingIcon = {
                    Icon(Icons.Outlined.Lock, contentDescription = null, tint = HabitOnSurfaceVariant)
                },
                trailingIcon = {
                    IconButton(onClick = loginViewModel::onTogglePasswordVisibility) {
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

            TextButton(
                onClick = { /* UI only */ },
                modifier = Modifier.align(Alignment.End),
            ) {
                Text(
                    text = stringResource(R.string.login_forgot_password),
                    style = MaterialTheme.typography.labelMedium,
                    color = HabitPrimary,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            uiState.errorMessage?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Button(
                onClick = loginViewModel::onClickSelected,
                enabled = uiState.isLoginEnabled && !uiState.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    disabledContainerColor = HabitPrimaryLight,
                    contentColor = Color.White,
                    disabledContentColor = Color.White,
                ),
                contentPadding = ButtonDefaults.ContentPadding,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (uiState.isLoading) {
                                Brush.linearGradient(listOf(HabitPrimaryLight, HabitPrimaryLight))
                            } else {
                                Brush.linearGradient(listOf(HabitPrimary, HabitPrimaryLight))
                            }
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(
                            if (uiState.isLoading) R.string.login_signing_in else R.string.login_sign_in
                        ),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier.padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = stringResource(R.string.login_no_account),
                    style = MaterialTheme.typography.bodyMedium,
                    color = HabitOnSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.login_sign_up),
                    style = MaterialTheme.typography.labelMedium,
                    color = HabitPrimary,
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .clickable(onClick = navigateToRegister),
                )
            }
        }

        if (uiState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Composable
fun AuthLabeledField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: (@Composable (() -> Unit))? = null,
    trailingIcon: (@Composable (() -> Unit))? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = HabitOnSurface,
        )
        Spacer(modifier = Modifier.height(6.dp))
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(HabitField)
                .border(2.dp, Color.Transparent, RoundedCornerShape(16.dp)),
            placeholder = {
                Text(text = placeholder, color = HabitOnSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            },
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            singleLine = true,
            visualTransformation = visualTransformation,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = HabitField,
                unfocusedContainerColor = HabitField,
                disabledContainerColor = HabitField,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                cursorColor = HabitPrimary,
            ),
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = HabitOnSurface),
        )
    }
}
