package com.marta.habittracker.presentation.screens.auth.register

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import com.marta.habittracker.presentation.theme.bloomLogoRes
import com.marta.habittracker.presentation.theme.isHabitDarkTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
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
import com.marta.habittracker.presentation.theme.habitTopBarButtonHorizontalPadding
import com.marta.habittracker.presentation.components.HabitLoadingIndicator
import com.marta.habittracker.presentation.theme.HabitSurfaceVariant
import com.marta.habittracker.presentation.utils.ObserveAsEvents
import com.marta.habittracker.presentation.utils.SyncStatusBarIcons
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch

@Composable
fun RegisterScreen(
    registerViewModel: RegisterViewModel = hiltViewModel(),
    navigateBack: () -> Unit,
    navigateToLogin: (email: String, password: String) -> Unit = { _, _ -> },
    navigateToHome: () -> Unit = {},
) {
    val uiState by registerViewModel.uiState.collectAsStateWithLifecycle()

    ObserveAsEvents(flow = registerViewModel.navigateToHome) {
        navigateToHome()
    }
    ObserveAsEvents(flow = registerViewModel.accountCreated) { account ->
        navigateToLogin(account.email, account.password)
    }

    RegisterContent(
        uiState = uiState,
        onBack = navigateBack,
        onNameChanged = registerViewModel::onNameChanged,
        onUsernameChanged = registerViewModel::onUsernameChanged,
        onUsernameSuggestionSelected = registerViewModel::onUsernameSuggestionSelected,
        onEmailChanged = registerViewModel::onEmailChanged,
        onPasswordChanged = registerViewModel::onPasswordChanged,
        onTogglePasswordVisibility = registerViewModel::onTogglePasswordVisibility,
        onTermsChecked = registerViewModel::onTermsChecked,
        onRegisterClicked = registerViewModel::onRegisterClicked,
        focusField = registerViewModel.focusField,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RegisterContent(
    uiState: RegisterUiState,
    onBack: () -> Unit,
    onNameChanged: (String) -> Unit,
    onUsernameChanged: (String) -> Unit = {},
    onUsernameSuggestionSelected: (String) -> Unit = {},
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onTermsChecked: (Boolean) -> Unit,
    onRegisterClicked: () -> Unit,
    focusField: Flow<RegisterField> = emptyFlow(),
) {
    SyncStatusBarIcons(lightBackground = !isHabitDarkTheme())
    val nameFocus = remember { FocusRequester() }
    val usernameFocus = remember { FocusRequester() }
    val emailFocus = remember { FocusRequester() }
    val passwordFocus = remember { FocusRequester() }
    val termsFocus = remember { FocusRequester() }
    val nameBringIntoView = remember { BringIntoViewRequester() }
    val usernameBringIntoView = remember { BringIntoViewRequester() }
    val emailBringIntoView = remember { BringIntoViewRequester() }
    val passwordBringIntoView = remember { BringIntoViewRequester() }
    val termsBringIntoView = remember { BringIntoViewRequester() }
    val focusScope = rememberCoroutineScope()
    ObserveAsEvents(flow = focusField) { field ->
        focusScope.launch {
            when (field) {
                RegisterField.Name -> {
                    nameBringIntoView.bringIntoView()
                    nameFocus.requestFocus()
                }
                RegisterField.Username -> {
                    usernameBringIntoView.bringIntoView()
                    usernameFocus.requestFocus()
                }
                RegisterField.Email -> {
                    emailBringIntoView.bringIntoView()
                    emailFocus.requestFocus()
                }
                RegisterField.Password -> {
                    passwordBringIntoView.bringIntoView()
                    passwordFocus.requestFocus()
                }
                RegisterField.Terms -> {
                    termsBringIntoView.bringIntoView()
                    termsFocus.requestFocus()
                }
            }
        }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        HabitIconButton(
                            modifier = Modifier.habitTopBarButtonHorizontalPadding(),
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
                Image(
                    painter = painterResource(bloomLogoRes()),
                    contentDescription = stringResource(R.string.app_name),
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(24.dp))
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
                    modifier = Modifier
                        .focusRequester(nameFocus)
                        .bringIntoViewRequester(nameBringIntoView),
                    isError = uiState.nameErrorRes != null,
                    supportingText = uiState.nameErrorRes?.let { stringResource(it) }.orEmpty(),
                )

                Spacer(modifier = Modifier.height(16.dp))

                CustomTextField(
                    label = stringResource(R.string.register_username_label),
                    value = uiState.username,
                    onValueChange = onUsernameChanged,
                    placeholder = stringResource(R.string.register_username_placeholder),
                    modifier = Modifier
                        .focusRequester(usernameFocus)
                        .bringIntoViewRequester(usernameBringIntoView),
                    isError = uiState.usernameErrorRes != null,
                    supportingText = uiState.usernameErrorRes?.let { stringResource(it) }.orEmpty(),
                )
                if (uiState.usernameSuggestions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        uiState.usernameSuggestions.forEach { suggestion ->
                            Text(
                                text = stringResource(R.string.profile_username, suggestion),
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                    .clickable { onUsernameSuggestionSelected(suggestion) }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                CustomTextField(
                    label = stringResource(R.string.register_email_label),
                    value = uiState.email,
                    onValueChange = onEmailChanged,
                    placeholder = stringResource(R.string.register_email_placeholder),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier
                        .focusRequester(emailFocus)
                        .bringIntoViewRequester(emailBringIntoView),
                    isError = uiState.emailErrorRes != null,
                    supportingText = uiState.emailErrorRes?.let { stringResource(it) }.orEmpty(),
                )

                Spacer(modifier = Modifier.height(16.dp))

                CustomTextField(
                    label = stringResource(R.string.register_password_label),
                    value = uiState.password,
                    onValueChange = onPasswordChanged,
                    placeholder = stringResource(R.string.register_password_placeholder),
                    modifier = Modifier
                        .focusRequester(passwordFocus)
                        .bringIntoViewRequester(passwordBringIntoView),
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
                        .focusRequester(termsFocus)
                        .bringIntoViewRequester(termsBringIntoView)
                        .clip(RoundedCornerShape(16.dp))
                        .background(HabitSurfaceVariant)
                        .clickable { onTermsChecked(!uiState.termsAccepted) }
                        .focusable()
                        .padding(12.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    FlowerCheck(
                        checked = uiState.termsAccepted,
                        isError = uiState.termsErrorRes != null,
                    )
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                uiState.termsErrorRes?.let { messageRes ->
                    Text(
                        text = stringResource(messageRes),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp),
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
                    clickableWhenDisabled = !uiState.isLoading,
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

@Composable
private fun FlowerCheck(
    checked: Boolean,
    isError: Boolean,
    modifier: Modifier = Modifier,
) {
    val petal = MaterialTheme.colorScheme.primary
    val center = MaterialTheme.colorScheme.onPrimary
    val outline = if (isError) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.outline
    }
    Canvas(modifier = modifier.size(28.dp)) {
        val middle = Offset(size.width / 2f, size.height / 2f)
        val petalRadius = size.minDimension * 0.22f
        val orbit = size.minDimension * 0.26f
        val stroke = Stroke(width = 1.5.dp.toPx())
        repeat(5) { index ->
            val angle = Math.toRadians(-90.0 + index * 72.0)
            val petalCenter = Offset(
                x = middle.x + cos(angle).toFloat() * orbit,
                y = middle.y + sin(angle).toFloat() * orbit,
            )
            if (checked) {
                drawCircle(color = petal, radius = petalRadius, center = petalCenter)
            } else {
                drawCircle(color = outline, radius = petalRadius, center = petalCenter, style = stroke)
            }
        }
        if (checked) {
            drawCircle(color = center, radius = size.minDimension * 0.12f, center = middle)
        } else {
            drawCircle(
                color = outline,
                radius = size.minDimension * 0.12f,
                center = middle,
                style = stroke,
            )
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
