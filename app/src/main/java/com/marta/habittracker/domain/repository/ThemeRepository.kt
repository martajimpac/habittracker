package com.marta.habittracker.domain.repository

import com.marta.habittracker.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface ThemeRepository {
    fun themeMode(): Flow<ThemeMode>
    suspend fun setThemeMode(mode: ThemeMode)
}
