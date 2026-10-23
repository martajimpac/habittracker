package com.marta.habittracker.domain.reminder

interface DailyCheckInScheduler {
    fun schedule(enabled: Boolean, time: String)
}
