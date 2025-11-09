package com.aristidevs.habittracker.ui.theme

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
    primary = InstaBlue,
    onPrimary = Color.White,
    background = Gray20,
    onBackground = Gray70,
    onSurface = Color.White,
    onSurfaceVariant = Color.White,
    tertiary = Red60
)

private val LightColorScheme = lightColorScheme(
    primary = InstaBlue,
    onPrimary = Color.White,
    background = Gray100,
    onSurface = Color.Black,
    onBackground = Gray80,
    onSurfaceVariant = Gray30,
    tertiary = Red60
)


// Local para nuestros colores personalizados
val LocalStateColors = staticCompositionLocalOf<StateColors> {
    error("No StateColors provided")
}


@Composable
fun InstaDevTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
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

    val stateColors = if (darkTheme) {
        DarkStateColors
    } else {
        LightStateColors
    }

    CompositionLocalProvider(
        LocalStateColors provides stateColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography  = Typography,
            shapes      = shapes,
            content     = content
        )
    }
}