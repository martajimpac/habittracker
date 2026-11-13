package com.marta.habittracker.presentation.widgets.today

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.time.Instant
import java.time.ZonedDateTime

/**
 * Wakes the Today widget just after local midnight so it switches to the new day's habits.
 * The alarm re-arms itself each time it fires; it lives only while a Today widget exists.
 */
object TodayWidgetMidnightAlarm {

    const val ACTION_MIDNIGHT = "com.marta.habittracker.action.TODAY_WIDGET_MIDNIGHT"

    /** Small margin so `LocalDate.now()` is already the new day when the widget redraws. */
    private const val MARGIN_SECONDS = 5L
    private const val REQUEST_CODE = 1_700_000_100
    private const val TAG = "TodayWidgetMidnight"

    internal fun nextTrigger(now: ZonedDateTime): Instant =
        now.toLocalDate()
            .plusDays(1)
            .atStartOfDay(now.zone)
            .plusSeconds(MARGIN_SECONDS)
            .toInstant()

    fun schedule(context: Context) {
        try {
            val alarmManager = context.getSystemService(AlarmManager::class.java)
            if (alarmManager == null) {
                Log.e(TAG, "schedule failed: AlarmManager missing")
                return
            }
            val trigger = nextTrigger(ZonedDateTime.now()).toEpochMilli()
            val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                alarmManager.canScheduleExactAlarms()
            if (canExact) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pendingIntent(context))
            } else {
                // Without exact alarm permission the system may delay it a bit; still better than the 30 min poll.
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pendingIntent(context))
            }
            Log.d(TAG, "schedule ok exact=$canExact")
        } catch (e: Exception) {
            Log.e(TAG, "schedule failed", e)
        }
    }

    fun cancel(context: Context) {
        try {
            val alarmManager = context.getSystemService(AlarmManager::class.java)
            if (alarmManager == null) {
                Log.e(TAG, "cancel failed: AlarmManager missing")
                return
            }
            alarmManager.cancel(pendingIntent(context))
            Log.d(TAG, "cancel ok")
        } catch (e: Exception) {
            Log.e(TAG, "cancel failed", e)
        }
    }

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, TodayWidgetReceiver::class.java).setAction(ACTION_MIDNIGHT)
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
