package com.marta.habittracker.data.repository

import com.marta.habittracker.data.local.datastore.OnboardingPreferencesDataSource
import com.marta.habittracker.domain.repository.OnboardingRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OnboardingRepositoryImpl @Inject constructor(
    private val dataSource: OnboardingPreferencesDataSource,
) : OnboardingRepository {

    override suspend fun isCompleted(): Boolean = dataSource.isOnboardingCompleted()

    override suspend fun setCompleted() {
        dataSource.setOnboardingCompleted()
    }
}
