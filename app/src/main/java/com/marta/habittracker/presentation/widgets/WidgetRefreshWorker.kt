package com.marta.habittracker.presentation.widgets

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class WidgetRefreshWorker(
    appContext: Context,
    workerParameters: WorkerParameters,
) : CoroutineWorker(appContext, workerParameters) {

    override suspend fun doWork(): Result =
        try {
            Log.d(TAG, "Periodic widget refresh started")
            WidgetRefresher.refreshAll(applicationContext)
            Log.d(TAG, "Periodic widget refresh finished")
            Result.success()
        } catch (exception: Exception) {
            Log.e(TAG, "Periodic widget refresh failed", exception)
            Result.retry()
        }

    private companion object {
        const val TAG = "WidgetRefreshWorker"
    }
}
