package com.marta.habittracker.data.remote.supabase.datasources

import android.util.Log
import com.marta.habittracker.data.remote.supabase.SupabaseFunctions
import com.marta.habittracker.data.remote.supabase.SupabaseTables
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.from
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException

/** FCM device tokens used by server push (challenges, friend requests). */
@Singleton
class DeviceTokenRemoteDataSource @Inject constructor(
    private val supabase: SupabaseClient,
) {

    suspend fun registerDeviceToken(token: String) {
        try {
            Log.d(TAG, "registerDeviceToken started")
            supabase.functions.invoke(
                SupabaseFunctions.REGISTER_DEVICE,
                body = mapOf("token" to token),
            )
            Log.d(TAG, "registerDeviceToken finished")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "registerDeviceToken failed", e)
            throw e
        }
    }

    suspend fun deleteDeviceToken(token: String) {
        try {
            Log.d(TAG, "deleteDeviceToken started")
            supabase.from(SupabaseTables.DEVICE_TOKENS).delete {
                filter {
                    eq("token", token)
                }
            }
            Log.d(TAG, "deleteDeviceToken finished")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "deleteDeviceToken failed", e)
            throw e
        }
    }

    private companion object {
        const val TAG = "DeviceTokenRemoteDS"
    }
}
