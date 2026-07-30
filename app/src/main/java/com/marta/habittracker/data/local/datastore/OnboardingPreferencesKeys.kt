package com.marta.habittracker.data.local.datastore

import androidx.datastore.preferences.core.booleanPreferencesKey

object OnboardingPreferencesKeys {
    val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
}
