package com.marta.habittracker

import android.app.Application
import android.util.Log
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.marta.habittracker.presentation.widgets.WidgetRefreshWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit

@HiltAndroidApp
class HabitTracker : Application(), Configuration.Provider {

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()
        schedulePeriodicWidgetRefresh()
    }

    private fun schedulePeriodicWidgetRefresh() {
        // Robolectric unit tests do not need periodic widget work enqueued at Application start.
        if ("robolectric".equals(android.os.Build.FINGERPRINT, ignoreCase = true)) {
            return
        }
        try {
            val request = PeriodicWorkRequestBuilder<WidgetRefreshWorker>(
                45,
                TimeUnit.MINUTES,
            ).build()
            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                WIDGET_REFRESH_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
            Log.d(TAG, "Periodic widget refresh scheduled")
        } catch (e: IllegalStateException) {
            Log.w(TAG, "Skipping widget refresh schedule; WorkManager unavailable", e)
        }
    }

    private companion object {
        const val TAG = "HabitTracker"
        const val WIDGET_REFRESH_WORK_NAME = "widget_refresh"
    }
}
