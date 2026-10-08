package com.marta.habittracker.di

import android.content.Context
import android.util.Log
import com.marta.habittracker.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule {

    private const val TAG = "SupabaseModule"

    @Provides
    @Singleton
    fun provideSupabaseClient(
        @ApplicationContext context: Context
    ): SupabaseClient {
        Log.d(TAG, "Creating Supabase client with session persistence enabled")
        return createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY,
        ) {
            install(Auth) {
                scheme = "habittracker"
                host = "auth"
            }
            install(Postgrest)
            install(Storage)
        }.also {
            Log.d(TAG, "Supabase client created successfully")
        }
    }
}
