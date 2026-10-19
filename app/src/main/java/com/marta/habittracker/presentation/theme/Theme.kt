package com.marta.habittracker.presentation.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.marta.habittracker.domain.model.ThemeMode
import androidx.compose.runtime.getValue

private val DarkColorScheme = darkColorScheme(
    primary = HabitPrimaryLight,
    onPrimary = Color.White,
    primaryContainer = HabitPrimaryContainerDark,
    onPrimaryContainer = HabitOnPrimaryContainerDark,

    secondary = HabitSecondaryLight,
    onSecondary = Color.White,
    secondaryContainer = Gray30,
    onSecondaryContainer = HabitOnPrimaryContainerDark,

    tertiary = HabitTertiary,
    onTertiary = HabitOnTertiary,
    tertiaryContainer = HabitOnTertiaryContainer,
    onTertiaryContainer = HabitTertiaryContainer,

    background = Gray20,
    onBackground = Color.White,

    surface = Gray20,
    onSurface = Color.White,

    surfaceVariant = Gray30,
    onSurfaceVariant = Gray70,

    surfaceContainerLowest = HabitSurfaceContainerLowestDark,
    surfaceContainerLow = HabitSurfaceContainerLowDark,
    surfaceContainer = HabitSurfaceContainerDark,
    surfaceContainerHigh = HabitSurfaceContainerHighDark,
    surfaceContainerHighest = HabitSurfaceContainerHighestDark,

    outline = Gray50,

    error = ErrorColor,
    onError = Color.White,
    errorContainer = HabitErrorContainerDark,
    onErrorContainer = HabitOnErrorContainerDark,
)

private val LightColorScheme = lightColorScheme(
    primary = HabitPrimary,
    onPrimary = HabitCream,
    primaryContainer = HabitPrimaryContainer,
    onPrimaryContainer = HabitOnPrimaryContainer,

    secondary = HabitSecondary,
    onSecondary = HabitCream,
    secondaryContainer = HabitSecondaryContainer,
    onSecondaryContainer = HabitOnSecondaryContainer,

    tertiary = HabitTertiary,
    onTertiary = HabitOnTertiary,
    tertiaryContainer = HabitTertiaryContainer,
    onTertiaryContainer = HabitOnTertiaryContainer,

    background = HabitSurface,
    onBackground = HabitOnSurface,

    surface = HabitSurface,
    onSurface = HabitOnSurface,

    surfaceVariant = HabitSurfaceVariant,
    onSurfaceVariant = HabitOnSurfaceVariant,

    surfaceContainerLowest = HabitSurfaceContainerLowest,
    surfaceContainerLow = HabitSurfaceContainerLow,
    surfaceContainer = HabitSurfaceContainer,
    surfaceContainerHigh = HabitSurfaceContainerHigh,
    surfaceContainerHighest = HabitSurfaceContainerHighest,

    outline = HabitOutline,

    error = ErrorColorLight,
    onError = Color.White,
    errorContainer = HabitErrorContainer,
    onErrorContainer = HabitOnErrorContainer,
)


val LocalHabitDarkTheme = staticCompositionLocalOf { false }

@Composable
fun isHabitDarkTheme(): Boolean = LocalHabitDarkTheme.current

data class HabitDangerColors(
    val container: Color,
    val border: Color,
    val content: Color,
    val icon: Color,
    val button: Color,
    val onButton: Color,
)

@Composable
fun habitDangerColors(): HabitDangerColors = if (isHabitDarkTheme()) {
    HabitDangerColors(
        container = HabitDangerContainerDark,
        border = HabitDangerBorderDark,
        content = Color.White,
        icon = Color.White,
        button = HabitDangerButtonDark,
        onButton = HabitOnDanger,
    )
} else {
    HabitDangerColors(
        container = HabitDangerContainer,
        border = HabitDangerIcon,
        content = HabitOnSurface,
        icon = HabitDangerIcon,
        button = HabitDanger,
        onButton = HabitOnDanger,
    )
}

@Composable
fun ThemeMode.isDarkTheme(): Boolean = when (this) {
    ThemeMode.System -> isSystemInDarkTheme()
    ThemeMode.Light -> false
    ThemeMode.Dark -> true
}

@Composable
fun HabitAppTheme(
    viewModel: AppThemeViewModel = hiltViewModel(),
    content: @Composable () -> Unit,
) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    InstaDevTheme(darkTheme = themeMode.isDarkTheme(), content = content)
}

@Composable
fun InstaDevTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    CompositionLocalProvider(LocalHabitDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = shapes,
            content = content
        )
    }
}

@Preview(name = "Light chrome", showBackground = true)
@Composable
private fun LightThemePreview() {
    InstaDevTheme(darkTheme = false) {
        androidx.compose.material3.Surface(color = MaterialTheme.colorScheme.surface) {
            androidx.compose.material3.Text("surface", color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Preview(name = "Dark chrome", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DarkThemePreview() {
    InstaDevTheme(darkTheme = true) {
        androidx.compose.material3.Surface(color = MaterialTheme.colorScheme.surface) {
            androidx.compose.material3.Text("surface", color = MaterialTheme.colorScheme.onSurface)
        }
    }
}
