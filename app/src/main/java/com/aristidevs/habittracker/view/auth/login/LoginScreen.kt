package com.aristidevs.habittracker.view.auth.login

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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aristidevs.habittracker.R
import com.aristidevs.habittracker.view.core.components.InstaButton
import com.aristidevs.habittracker.view.core.components.InstaButtonSecondary
import com.aristidevs.habittracker.view.core.components.InstaText
import com.aristidevs.habittracker.view.core.components.InstaTextField


@Composable
fun LoginScreen(
    loginViewModel: LoginViewModel = hiltViewModel(),
    navigateToRegister: () -> Unit,
    navigateToHome: () -> Unit
) {
    val uiState by loginViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isUserLogged) {
        if (uiState.isUserLogged) {
            navigateToHome()
        }
    }

    Scaffold { padding ->
        Box(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .background(MaterialTheme.colorScheme.background)
                    .padding(padding)
                    .padding(horizontal = 24.dp)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                InstaText(
                    text = stringResource(R.string.login_screen_header_text_spain),
                    modifier = Modifier.padding(top = 22.dp),
                )
                Spacer(Modifier.weight(1f))
                Image(
                    modifier = Modifier.size(56.dp),
                    painter = painterResource(R.drawable.instadev_logo),
                    contentDescription = "InstaDev logo header"
                )
                Spacer(Modifier.weight(1f))

                InstaTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = uiState.email,
                    label = stringResource(R.string.login_screen_textfield_email),
                    onValueChange = { loginViewModel.onEmailChanged(it) })

                Spacer(Modifier.height(10.dp))
                InstaTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = uiState.password,
                    label = stringResource(R.string.login_screen_textfield_password),
                    onValueChange = { loginViewModel.onPasswordChanged(it) })
                Spacer(Modifier.height(10.dp))

                InstaButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(R.string.login_screen_button_login),
                    onClick = { loginViewModel.onClickSelected() },
                    enabled = uiState.isLoginEnabled && !uiState.isLoading,
                )

                TextButton(onClick = {}) {
                    InstaText(
                        text = stringResource(R.string.login_screen_text_forgot_password),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.weight(1.3f))
                InstaButtonSecondary(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { navigateToRegister() },
                    title = stringResource(R.string.login_screen_button_register)
                )
                Icon(
                    modifier = Modifier
                        .width(60.dp)
                        .padding(vertical = 22.dp),
                    painter = painterResource(R.drawable.ic_meta),
                    contentDescription = "meta logo",
                    tint = MaterialTheme.colorScheme.onBackground
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