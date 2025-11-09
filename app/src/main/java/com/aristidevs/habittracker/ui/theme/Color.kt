package com.aristidevs.habittracker.ui.theme

import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

//InstaDev


val Red60 = Color(0xFFF4323E)
val InstaBlue = Color(0xFF0B5FD5)
val Gray20 = Color(0xFF152125)
val Gray30 = Color(0xFF3B3D3E)
val Gray70 = Color(0xFF8EA0B0)
val Gray80 = Color(0xFF576877)
val Gray100 = Color(0xFFF4F4F4)


// ---- Colores de estado personalizados (LIGHT) ----
val StatusOccupiedLight     = Color(0xFFEC3900)
val StatusFreeLight         = Color(0xFF0F8A15)
val StatusNotAvailableLight = Color(0xFF676767)


// ---- Colores de estado personalizados (DARK) ----
val StatusOccupiedDark     = Color(0xFFFF6E40)
val StatusFreeDark         = Color(0xFF81C784)
val StatusNotAvailableDark = Color(0xFFBDBDBD)

// Clase que agrupa nuestros colores de estado
data class StateColors(
    val occupied: Color,
    val free: Color,
    val notAvailable: Color
)

// Instancias para light y dark
val LightStateColors = StateColors(
    occupied     = StatusOccupiedLight,
    free         = StatusFreeLight,
    notAvailable = StatusNotAvailableLight
)

val DarkStateColors = StateColors(
    occupied     = StatusOccupiedDark,
    free         = StatusFreeDark,
    notAvailable = StatusNotAvailableDark
)




