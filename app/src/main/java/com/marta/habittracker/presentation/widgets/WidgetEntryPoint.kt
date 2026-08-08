package com.marta.habittracker.presentation.widgets

import com.marta.habittracker.data.local.datastore.WidgetPreferencesDataSource
import com.marta.habittracker.core.network.NetworkChecker
import com.marta.habittracker.domain.repository.FriendsRepository
import com.marta.habittracker.domain.repository.HabitRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun habitRepository(): HabitRepository

    fun friendsRepository(): FriendsRepository

    fun networkChecker(): NetworkChecker

    fun json(): Json

    fun widgetPreferencesDataSource(): WidgetPreferencesDataSource
}
