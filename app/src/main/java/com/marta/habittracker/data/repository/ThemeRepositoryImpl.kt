package com.marta.habittracker.data.repository

import com.marta.habittracker.data.local.datastore.ThemePreferencesDataSource
import com.marta.habittracker.domain.model.ThemeMode
import com.marta.habittracker.domain.repository.ThemeRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThemeRepositoryImpl @Inject constructor(
    private val dataSource: ThemePreferencesDataSource,
) : ThemeRepository {
    override fun themeMode(): Flow<ThemeMode> = dataSource.themeMode()

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataSource.setThemeMode(mode)
    }
}
