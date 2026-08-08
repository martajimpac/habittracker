package com.marta.habittracker.di

import com.marta.habittracker.core.DefaultDispatchersProvider
import com.marta.habittracker.core.network.AndroidNetworkChecker
import com.marta.habittracker.core.network.NetworkChecker
import com.marta.habittracker.data.repository.AuthRepositoryImpl
import com.marta.habittracker.data.repository.FriendsRepositoryImpl
import com.marta.habittracker.data.repository.HabitRepositoryImpl
import com.marta.habittracker.data.repository.OnboardingRepositoryImpl
import com.marta.habittracker.domain.coroutines.DispatchersProvider
import com.marta.habittracker.domain.repository.AuthRepository
import com.marta.habittracker.domain.repository.FriendsRepository
import com.marta.habittracker.domain.repository.HabitRepository
import com.marta.habittracker.domain.repository.OnboardingRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindDispatchersProvider(impl: DefaultDispatchersProvider): DispatchersProvider

    @Binds
    @Singleton
    abstract fun bindNetworkChecker(impl: AndroidNetworkChecker): NetworkChecker

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindHabitRepository(impl: HabitRepositoryImpl): HabitRepository

    @Binds
    @Singleton
    abstract fun bindFriendsRepository(impl: FriendsRepositoryImpl): FriendsRepository

    @Binds
    @Singleton
    abstract fun bindOnboardingRepository(impl: OnboardingRepositoryImpl): OnboardingRepository

    companion object {
        @Provides
        @Singleton
        fun provideJson(): Json {
            return Json {
                ignoreUnknownKeys = true
                isLenient = true
            }
        }
    }
}
