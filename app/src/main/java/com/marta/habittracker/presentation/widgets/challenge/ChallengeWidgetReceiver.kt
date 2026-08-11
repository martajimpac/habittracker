package com.marta.habittracker.presentation.widgets.challenge

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.marta.habittracker.presentation.widgets.WidgetEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ChallengeWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ChallengeGlanceWidget()

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val preferences = EntryPointAccessors
                    .fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
                    .widgetPreferencesDataSource()
                appWidgetIds.forEach { appWidgetId ->
                    preferences.clearWidget(appWidgetId)
                    Log.d(TAG, "Cleared preferences for deleted widget $appWidgetId")
                }
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to clear preferences for deleted challenge widget", exception)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private companion object {
        const val TAG = "ChallengeWidgetReceiver"
    }
}
