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

private val DarkColorScheme = darkColorScheme(
    primary = HabitPrimaryLight,
    onPrimary = Color.White,

    secondary = HabitSecondaryLight,
    onSecondary = Color.White,

    background = Gray20,
    onBackground = Color.White,

    surface = Gray20,
    onSurface = Color.White,

    surfaceVariant = Gray30,
    onSurfaceVariant = Gray70,

    outline = Gray50,

    error = ErrorColor,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = HabitPrimary,
    onPrimary = Color.White,

    secondary = HabitSecondary,
    onSecondary = Color.White,

    background = HabitSurface,
    onBackground = HabitOnSurface,

    surface = HabitSurface,
    onSurface = HabitOnSurface,

    surfaceVariant = HabitSurfaceVariant,
    onSurfaceVariant = HabitOnSurfaceVariant,

    outline = HabitOutline,

    error = ErrorColorLight,
    onError = Color.White
)


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


    CompositionLocalProvider() {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = shapes,
            content = content
        )
    }
}
