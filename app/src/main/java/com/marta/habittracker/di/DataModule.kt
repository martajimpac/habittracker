package com.marta.habittracker.di

import com.marta.habittracker.data.coroutines.DefaultDispatchersProvider
import com.marta.habittracker.data.local.database.HabitRepositoryImpl
import com.marta.habittracker.data.network.AuthRepositoryImpl
import com.marta.habittracker.domain.coroutines.DispatchersProvider
import com.marta.habittracker.domain.repository.AuthRepository
import com.marta.habittracker.domain.repository.HabitRepository
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
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindHabitRepository(impl: HabitRepositoryImpl): HabitRepository

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
