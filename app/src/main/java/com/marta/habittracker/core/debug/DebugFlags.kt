package com.marta.habittracker.core.debug

/**
 * Switches for local testing. Edit a value and rerun the debug build.
 * Release builds ignore these flags.
 */
object DebugFlags {
    /** When true, debug builds open onboarding on every cold start. */
    const val ALWAYS_SHOW_ONBOARDING = false

    fun onboardingAlreadyCompleted(
        storedCompleted: Boolean,
        isDebugBuild: Boolean,
        alwaysShowOnboarding: Boolean = ALWAYS_SHOW_ONBOARDING,
    ): Boolean = if (isDebugBuild && alwaysShowOnboarding) false else storedCompleted
}
