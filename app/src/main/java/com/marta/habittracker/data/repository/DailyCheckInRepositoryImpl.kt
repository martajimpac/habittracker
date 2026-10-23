package com.marta.habittracker.data.repository

import com.marta.habittracker.data.local.datastore.DailyCheckInPreferencesDataSource
import com.marta.habittracker.domain.model.DailyCheckInSettings
import com.marta.habittracker.domain.reminder.DailyCheckInScheduler
import com.marta.habittracker.domain.repository.DailyCheckInRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DailyCheckInRepositoryImpl @Inject constructor(
    private val dataSource: DailyCheckInPreferencesDataSource,
    private val scheduler: DailyCheckInScheduler,
) : DailyCheckInRepository {

    override fun settings(): Flow<DailyCheckInSettings> = dataSource.settings()

    override suspend fun setEnabled(enabled: Boolean) {
        val time = dataSource.settings().first().time
        dataSource.save(enabled, time)
        scheduler.schedule(enabled, time)
    }

    override suspend fun setTime(time: String) {
        val enabled = dataSource.settings().first().enabled
        dataSource.save(enabled, time)
        scheduler.schedule(enabled, time)
    }

    override suspend fun reschedule() {
        val settings = dataSource.settings().first()
        scheduler.schedule(settings.enabled, settings.time)
    }
}
