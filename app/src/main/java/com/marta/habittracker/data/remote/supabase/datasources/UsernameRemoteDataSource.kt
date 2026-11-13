package com.marta.habittracker.data.remote.supabase.datasources

import android.util.Log
import com.marta.habittracker.data.remote.supabase.SupabaseRpc
import com.marta.habittracker.data.remote.supabase.dto.UsernameAvailabilityDto
import com.marta.habittracker.data.remote.supabase.dto.UsernameAvailabilityParams
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException

@Singleton
class UsernameRemoteDataSource @Inject constructor(
    private val supabase: SupabaseClient,
) {

    suspend fun checkAvailability(requested: String): UsernameAvailabilityDto {
        return try {
            Log.d(TAG, "checkAvailability started")
            val row = supabase.postgrest.rpc(
                function = SupabaseRpc.USERNAME_AVAILABILITY,
                parameters = UsernameAvailabilityParams(requested = requested),
            ).decodeSingle<UsernameAvailabilityDto>()
            Log.d(TAG, "checkAvailability finished available=${row.available}")
            row
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "checkAvailability failed", e)
            throw e
        }
    }

    private companion object {
        const val TAG = "UsernameRemoteDS"
    }
}
