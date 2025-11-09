package com.aristidevs.habittracker.view.auth.register

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    registerViewModel: RegisterViewModel = hiltViewModel(),
    navigateBack: () -> Unit,
) {
    val uiState by registerViewModel.uiState.collectAsStateWithLifecycle()

    val title: String
    val subtitle: String
    val label: String
    val changeModeTitle: String
    when(uiState.mode){
        MY_MODE.PHONE -> {
            title = stringResource(R.string.register_screen_title_phone)
            subtitle = stringResource(R.string.register_screen_subtitle_phone)
            label = stringResource(R.string.register_screen_textfield_register_phone)
            changeModeTitle = stringResource(R.string.register_screen_button_register_with_email)
        }
        MY_MODE.EMAIL -> {
            title = stringResource(R.string.register_screen_title_email)
            subtitle = stringResource(R.string.register_screen_subtitle_email)
            label = stringResource(R.string.register_screen_textfield_register_email)
            changeModeTitle = stringResource(R.string.register_screen_button_register_with_phone)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    MaterialTheme.colorScheme.background
                ),
                title = {},
                navigationIcon = {
                    IconButton(
                        onClick = { navigateBack() }
                    ){
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "back",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.clickable { navigateBack() }
                        )
                    }
                }
            )

        }
    ) { padding ->
        Box(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .background(MaterialTheme.colorScheme.background)
                    .padding(padding)
                    .padding(horizontal = 16.dp)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                //hacer una animación para el título
                AnimatedContent(title) {
                    InstaText(
                        text = it,
                        modifier = Modifier.padding(top = 22.dp),
                        style = MaterialTheme.typography.headlineLarge
                    )
                }
                Spacer(Modifier.height(4.dp))
                InstaText(
                    text = subtitle,
                    modifier = Modifier.padding(top = 22.dp),
                    style = MaterialTheme.typography.bodyLarge
                )

                InstaTextField(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(30.dp),
                    value = uiState.inputValue,
                    label = label,
                    onValueChange = { registerViewModel.onRegisterChanged(it) })

                Spacer(Modifier.height(8.dp))

                InstaButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(R.string.login_screen_button_login),
                    onClick = { },
                    enabled = uiState.isRegisterEnabled && !uiState.isLoading,
                )


                InstaButtonSecondary(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { registerViewModel.onChangeMode() },
                    title = stringResource(R.string.login_screen_button_register)
                )

                Spacer(Modifier.weight(1.3f))

                TextButton(onClick = {}) {
                    InstaText(
                        text = stringResource(R.string.login_screen_text_forgot_password),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

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
fun RegisterScreenPreview() {
    RegisterScreen(navigateBack = {})
}