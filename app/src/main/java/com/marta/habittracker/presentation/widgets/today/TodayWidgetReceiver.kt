package com.marta.habittracker.presentation.widgets.today

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.marta.habittracker.presentation.widgets.WidgetRefresher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TodayWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayGlanceWidget()

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            TodayWidgetMidnightAlarm.ACTION_MIDNIGHT,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> {
                TodayWidgetMidnightAlarm.schedule(context.applicationContext)
                pushToday(context, intent.action)
            }
            else -> super.onReceive(context, intent)
        }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        TodayWidgetMidnightAlarm.schedule(context)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        // Alarms are lost on reboot; the periodic/boot update re-arms it (same PendingIntent, no duplicates).
        TodayWidgetMidnightAlarm.schedule(context)
        // A new widget has no stored list yet, and the periodic update must catch a missed day.
        pushToday(context, "update")
    }

    override fun onDisabled(context: Context) {
        TodayWidgetMidnightAlarm.cancel(context)
        super.onDisabled(context)
    }

    /** Loads today's habits from Room and writes them into the widget state. */
    private fun pushToday(context: Context, reason: String?) {
        Log.d(TAG, "push today's habits reason=$reason")
        val appContext = context.applicationContext
        // Null inside onUpdate: Glance's own onUpdate already took the async result for this broadcast.
        val pendingResult: PendingResult? = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                WidgetRefresher.refreshTodayWidget(appContext)
            } finally {
                pendingResult?.finish()
            }
        }
    }

    private companion object {
        const val TAG = "TodayWidgetReceiver"
    }
}
