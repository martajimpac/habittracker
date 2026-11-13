package com.marta.habittracker.data.repository

import android.util.Log
import com.marta.habittracker.data.remote.supabase.datasources.UsernameRemoteDataSource
import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.model.errors.AppError
import com.marta.habittracker.domain.model.UsernameAvailability
import com.marta.habittracker.domain.repository.UsernameAvailabilityRepository
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException

@Singleton
class UsernameAvailabilityRepositoryImpl @Inject constructor(
    private val remote: UsernameRemoteDataSource,
) : UsernameAvailabilityRepository {

    override suspend fun check(raw: String): DataResult<UsernameAvailability, AppError> {
        return try {
            Log.d(TAG, "Username availability check started")
            val row = remote.checkAvailability(requested = raw)
            Log.d(TAG, "Username availability check finished available=${row.available}")
            DataResult.Success(
                UsernameAvailability(
                    normalized = row.normalized,
                    available = row.available,
                    suggestions = row.suggestions,
                ),
            )
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: IOException) {
            Log.e(TAG, "Username availability check failed: network", exception)
            DataResult.Error(AppError.Common.Network)
        } catch (exception: Exception) {
            Log.e(TAG, "Username availability check failed", exception)
            DataResult.Error(AppError.Common.Unknown)
        }
    }

    private companion object {
        const val TAG = "UsernameCheck"
    }
}
