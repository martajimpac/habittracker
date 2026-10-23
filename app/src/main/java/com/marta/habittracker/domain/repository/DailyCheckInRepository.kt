package com.marta.habittracker.domain.repository

import com.marta.habittracker.domain.model.DailyCheckInSettings
import kotlinx.coroutines.flow.Flow

interface DailyCheckInRepository {
    fun settings(): Flow<DailyCheckInSettings>
    suspend fun setEnabled(enabled: Boolean)
    suspend fun setTime(time: String)
    suspend fun reschedule()
}
