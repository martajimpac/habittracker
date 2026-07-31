package com.marta.habittracker.presentation.theme

import androidx.compose.ui.graphics.Color

// Habit Tracker Figma palette
val HabitPrimary = Color(0xFF6750A4)
val HabitPrimaryLight = Color(0xFF9B7FD4)
val HabitTeal = Color(0xFF0D9488)
val HabitTealLight = Color(0xFF5EEAD4)
val HabitAmber = Color(0xFFD97706)
val HabitAmberLight = Color(0xFFFBBF24)
val HabitSurface = Color(0xFFFFFBFE)
val HabitOnSurface = Color(0xFF1C1B1F)
val HabitOnSurfaceVariant = Color(0xFF79747E)
val HabitField = Color(0xFFF4EFF4)
val HabitOutline = Color(0xFFE8DEF8)
val HabitTermsBg = Color(0xFFEEE8F4)
val HabitTermsText = Color(0xFF49454F)

val LoginGradientStart = Color(0xFF2563EB)
val LoginGradientEnd = Color(0xFF22D3EE)

val Red60 = Color(0xFFF4323E)
val InstaBlue = Color(0xFF0B5FD5)
val Gray20 = Color(0xFF152125)
val Gray30 = Color(0xFF3B3D3E)
val Gray70 = Color(0xFF8EA0B0)
val Gray80 = Color(0xFF576877)
val Gray100 = Color(0xFFF4F4F4)

val StatusOccupiedLight = Color(0xFFEC3900)
val StatusFreeLight = Color(0xFF0F8A15)
val StatusNotAvailableLight = Color(0xFF676767)

val StatusOccupiedDark = Color(0xFFFF6E40)
val StatusFreeDark = Color(0xFF81C784)
val StatusNotAvailableDark = Color(0xFFBDBDBD)

data class StateColors(
    val occupied: Color,
    val free: Color,
    val notAvailable: Color
)

val LightStateColors = StateColors(
    occupied = StatusOccupiedLight,
    free = StatusFreeLight,
    notAvailable = StatusNotAvailableLight
)

val DarkStateColors = StateColors(
    occupied = StatusOccupiedDark,
    free = StatusFreeDark,
    notAvailable = StatusNotAvailableDark
)
