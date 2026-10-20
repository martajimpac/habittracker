package com.marta.habittracker.di

import com.marta.habittracker.core.DefaultDispatchersProvider
import com.marta.habittracker.core.push.DeviceTokenRegistration
import com.marta.habittracker.core.push.DeviceTokenRegistrar
import com.marta.habittracker.core.network.AndroidNetworkChecker
import com.marta.habittracker.core.network.NetworkChecker
import com.marta.habittracker.core.reminder.AndroidHabitReminderScheduler
import com.marta.habittracker.data.remote.ProfileRemote
import com.marta.habittracker.data.remote.SocialRemoteDataSource
import com.marta.habittracker.data.repository.AuthRepositoryImpl
import com.marta.habittracker.data.repository.FriendsRepositoryImpl
import com.marta.habittracker.data.repository.HabitRepositoryImpl
import com.marta.habittracker.data.repository.OnboardingRepositoryImpl
import com.marta.habittracker.data.repository.ProfileRepositoryImpl
import com.marta.habittracker.data.repository.ThemeRepositoryImpl
import com.marta.habittracker.data.repository.UsernameAvailabilityRepositoryImpl
import com.marta.habittracker.domain.coroutines.DispatchersProvider
import com.marta.habittracker.domain.reminder.HabitReminderScheduler
import com.marta.habittracker.domain.repository.AuthRepository
import com.marta.habittracker.domain.repository.FriendsRepository
import com.marta.habittracker.domain.repository.HabitRepository
import com.marta.habittracker.domain.repository.OnboardingRepository
import com.marta.habittracker.domain.repository.ProfileRepository
import com.marta.habittracker.domain.repository.ThemeRepository
import com.marta.habittracker.domain.repository.UsernameAvailabilityRepository
import com.marta.habittracker.domain.widget.AppWidgetRefresher
import com.marta.habittracker.presentation.widgets.GlanceAppWidgetRefresher
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
    abstract fun bindAppWidgetRefresher(impl: GlanceAppWidgetRefresher): AppWidgetRefresher

    @Binds
    @Singleton
    abstract fun bindHabitReminderScheduler(impl: AndroidHabitReminderScheduler): HabitReminderScheduler

    @Binds
    @Singleton
    abstract fun bindFriendsRepository(impl: FriendsRepositoryImpl): FriendsRepository

    @Binds
    @Singleton
    abstract fun bindOnboardingRepository(impl: OnboardingRepositoryImpl): OnboardingRepository

    @Binds
    @Singleton
    abstract fun bindProfileRemote(impl: SocialRemoteDataSource): ProfileRemote

    @Binds
    @Singleton
    abstract fun bindProfileRepository(impl: ProfileRepositoryImpl): ProfileRepository

    @Binds
    @Singleton
    abstract fun bindThemeRepository(impl: ThemeRepositoryImpl): ThemeRepository

    @Binds
    @Singleton
    abstract fun bindUsernameAvailabilityRepository(
        impl: UsernameAvailabilityRepositoryImpl,
    ): UsernameAvailabilityRepository

    @Binds
    @Singleton
    abstract fun bindDeviceTokenRegistration(impl: DeviceTokenRegistrar): DeviceTokenRegistration

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
