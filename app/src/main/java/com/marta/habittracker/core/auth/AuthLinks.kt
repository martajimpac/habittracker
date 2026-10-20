package com.marta.habittracker.core.auth

object AuthLinks {
    const val SCHEME = "habittracker"
    const val HOST = "auth"
    const val RESET_PATH = "/reset"
    const val CONFIRM_PATH = "/confirm"
    const val RESET_URL = "$SCHEME://$HOST$RESET_PATH"
    const val CONFIRM_URL = "$SCHEME://$HOST$CONFIRM_PATH"
}

enum class AuthDeeplinkTarget {
    Reset,
    Confirm,
    Ignore,
}

fun authDeeplinkTarget(scheme: String?, host: String?, path: String?): AuthDeeplinkTarget {
    if (scheme != AuthLinks.SCHEME || host != AuthLinks.HOST) return AuthDeeplinkTarget.Ignore
    val normalized = path.orEmpty()
    return when {
        normalized.startsWith(AuthLinks.RESET_PATH) -> AuthDeeplinkTarget.Reset
        normalized.startsWith(AuthLinks.CONFIRM_PATH) -> AuthDeeplinkTarget.Confirm
        else -> AuthDeeplinkTarget.Ignore
    }
}
