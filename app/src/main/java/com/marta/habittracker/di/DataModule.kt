package com.marta.habittracker.di

import com.marta.habittracker.BuildConfig
import com.marta.habittracker.data.coroutines.DefaultDispatchersProvider
import com.marta.habittracker.data.local.database.HabitRepositoryImpl
import com.marta.habittracker.data.network.api.ApiServices
import com.marta.habittracker.data.network.AuthRepositoryImpl
import com.marta.habittracker.domain.coroutines.DispatchersProvider
import com.marta.habittracker.domain.repository.AuthRepository
import com.marta.habittracker.domain.repository.HabitRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    //Usamos binds cuando tenemos interfaz y implementacion
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
        fun provideSupabaseClient(): SupabaseClient {
            return createSupabaseClient(
                supabaseUrl = BuildConfig.SUPABASE_URL,
                supabaseKey = BuildConfig.SUPABASE_ANON_KEY
            ) {
                install(Auth)
                install(Postgrest)
                install(Storage)
            }
        }

        @Provides
        fun provideApiServices(retrofit: Retrofit): ApiServices {
            return retrofit.create(ApiServices::class.java)
        }

        @Provides
        @Singleton
        fun provideRetrofit(json: Json): Retrofit {
            return Retrofit.Builder()
                .baseUrl("https://aristidevs-bd31d-default-rtdb.europe-west1.firebasedatabase.app/")
                .addConverterFactory(json.asConverterFactory("application/json; charset=UTF8".toMediaType()))
                .build()
        }

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
