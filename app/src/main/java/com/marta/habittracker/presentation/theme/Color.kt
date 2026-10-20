package com.marta.habittracker.presentation.theme

import androidx.compose.ui.graphics.Color

// Habit Tracker — Custom palette

// ─────────────────────────────────────
// Brand
// ─────────────────────────────────────

// Forest green. Primary is the brand; PrimaryLight is the lighter tone for gradients.
// On dark surfaces the accent is lighter so it reads on black, with dark ink on top.
val HabitPrimary = Color(0xFF294536)
val HabitPrimaryLight = Color(0xFF3D6E50)
val HabitPrimaryOnDark = Color(0xFF8FCB97)
val HabitOnPrimaryDark = Color(0xFF143028)

val HabitSecondary = Color(0xFF294536)
val HabitSecondaryLight = Color(0xFF3D6E50)


// ─────────────────────────────────────
// Surfaces
// ─────────────────────────────────────

// Cream stand-in for white. Lightest tone is the reference; the rest step down from it.
val HabitCream = Color(0xFFFDF9F1)

val HabitSurface = Color(0xFFEFEBE3) //Fondo
val HabitOnSurface = Color(0xFF1A1D1F)

val HabitSurfaceVariant = Color(0xFFFDF7EF) //HABITO COMPLETADO
val HabitOnSurfaceVariant = Color(0xFF5D5D60) //HABITO COMPLETADO TEXTO

val HabitField = HabitCream //Tarjeta



val HabitOutline = Color(0xFF7D828A)


// ─────────────────────────────────────
// Habit colours
// ─────────────────────────────────────

// Saturated + distinctive, rainbow hue order
val HabitColor1 = Color(0xFFF04F45)
val HabitColor2 = Color(0xFFF27A24)
val HabitColor3 = Color(0xFFE8B923)
val HabitColor4 = Color(0xFFC5D63A)
val HabitColor5 = Color(0xFF83B735)
val HabitColor6 = Color(0xFF12CEA8)
val HabitColor7 = Color(0xFF45CFE7)
val HabitColor8 = Color(0xFF3A98EA)
val HabitColor9 = Color(0xFF4949CC)
val HabitColor10 = Color(0xFF8650B8)
val HabitColor11 = Color(0xFFEC5995)
val HabitColor12 = Color(0xFFBE184F)



// ─────────────────────────────────────
// Dark neutrals
// ─────────────────────────────────────

val Gray20 = Color(0xFF121212)
val Gray30 = Color(0xFF2F2F31)
val Gray50 = Color(0xFF8E8E93)
val Gray70 = Color(0xFFC8C8CC)


// ─────────────────────────────────────
// Status
// ─────────────────────────────────────

val ErrorColor = Color(0xFF79170D)
val ErrorColorLight = Color(0xFFEF5146)
val SuccessColor = Color(0xFF4ADE80)

// Cerrar sesión y eliminar cuenta. Coral, no el rol `error`.
private val BloomCoral = Color(0xFFFF7775)
private val BloomCoralDeep = Color(0xFFD94E4C)
private val BloomCoralInk = Color(0xFF5C2422)

val HabitDanger = BloomCoral
val HabitDangerContainer = Color(0xFFFFE8E6)
val HabitOnDanger = BloomCoralInk
val HabitDangerIcon = BloomCoralDeep
val HabitDangerContainerDark = Color(0xFF3F2626)
val HabitDangerBorderDark = BloomCoral
val HabitDangerButtonDark = BloomCoral

// ─────────────────────────────────────
// M3 ColorScheme containers
// ─────────────────────────────────────

val HabitPrimaryContainer = Color(0xFFDCE8E0)
val HabitOnPrimaryContainer = Color(0xFF294536)
val HabitSecondaryContainer = Color(0xFFE4EBE6)
val HabitOnSecondaryContainer = Color(0xFF294536)
val HabitTertiary = Color(0xFF1F6B45)
val HabitOnTertiary = HabitCream
val HabitTertiaryContainer = Color(0xFFC9E6D4)
val HabitOnTertiaryContainer = Color(0xFF0E2F1C)
val HabitErrorContainer = Color(0xFFFFDAD6)
val HabitOnErrorContainer = Color(0xFF410002)
val HabitSurfaceContainerLowest = HabitCream
val HabitSurfaceContainerLow = Color(0xFFEFEBE3)
val HabitSurfaceContainer = Color(0xFFEAE6DE)
val HabitSurfaceContainerHigh = Color(0xFFE4E0D8)
val HabitSurfaceContainerHighest = Color(0xFFDEDAD2)

val HabitPrimaryContainerDark = Color(0xFF1B3328)
val HabitOnPrimaryContainerDark = Color(0xFFDCE8E0)
val HabitSurfaceContainerDark = Color(0xFF2A2A2C)
val HabitSurfaceContainerLowestDark = Color(0xFF1C1C1E)
val HabitSurfaceContainerLowDark = Color(0xFF222224)
val HabitSurfaceContainerHighDark = Color(0xFF323234)
val HabitSurfaceContainerHighestDark = Color(0xFF3A3A3C)
val HabitErrorContainerDark = Color(0xFF93000A)
val HabitOnErrorContainerDark = Color(0xFFFFDAD6)
