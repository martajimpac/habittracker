package com.marta.habittracker.data.local.datastore

import androidx.datastore.preferences.core.stringPreferencesKey

object ThemePreferencesKeys {
    val THEME_MODE = stringPreferencesKey("theme_mode")
    const val LIGHT = "light"
    const val DARK = "dark"
}
