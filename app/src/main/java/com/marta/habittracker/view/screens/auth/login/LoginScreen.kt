package com.marta.habittracker.view.screens.auth.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.marta.habittracker.R
import com.marta.habittracker.view.core.components.CustomButton
import com.marta.habittracker.view.core.components.CustomButtonSecondary
import com.marta.habittracker.view.core.components.CustomText
import com.marta.habittracker.view.core.components.CustomTextField

@Composable
fun LoginScreen(
    loginViewModel: LoginViewModel = hiltViewModel(),
    navigateToRegister: () -> Unit,
    navigateToHome: () -> Unit,
) {
    val uiState by loginViewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.isUserLogged) {
        if (uiState.isUserLogged) {
            navigateToHome()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .background(MaterialTheme.colorScheme.background)
                    .padding(padding)
                    .padding(horizontal = 24.dp)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CustomText(
                    text = stringResource(R.string.login_screen_header_text_spain),
                    modifier = Modifier.padding(top = 22.dp),
                )
                Spacer(Modifier.weight(1f))
                Image(
                    modifier = Modifier.size(56.dp),
                    painter = painterResource(R.drawable.instadev_logo),
                    contentDescription = "InstaDev logo header",
                )
                Spacer(Modifier.weight(1f))

                CustomTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = uiState.email,
                    label = stringResource(R.string.login_screen_textfield_email),
                    onValueChange = loginViewModel::onEmailChanged,
                )

                Spacer(Modifier.height(10.dp))
                CustomTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = uiState.password,
                    label = stringResource(R.string.login_screen_textfield_password),
                    onValueChange = loginViewModel::onPasswordChanged,
                )
                Spacer(Modifier.height(10.dp))

                CustomButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(R.string.login_screen_button_login),
                    onClick = loginViewModel::onLoginClicked,
                    enabled = uiState.isLoginEnabled && !uiState.isLoading,
                )

                TextButton(onClick = {}) {
                    CustomText(
                        text = stringResource(R.string.login_screen_text_forgot_password),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.weight(1.3f))
                CustomButtonSecondary(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = navigateToRegister,
                    title = stringResource(R.string.login_screen_button_register),
                )
                Icon(
                    modifier = Modifier
                        .width(60.dp)
                        .padding(vertical = 22.dp),
                    painter = painterResource(R.drawable.ic_meta),
                    contentDescription = "meta logo",
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
        }

        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}

@Preview
@Composable
fun LoginScreenPreview() {
    LoginScreen(navigateToRegister = {}, navigateToHome = {})
}
