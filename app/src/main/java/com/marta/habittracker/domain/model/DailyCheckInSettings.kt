package com.marta.habittracker.domain.model

data class DailyCheckInSettings(
    val enabled: Boolean = false,
    val time: String = "20:00",
)
