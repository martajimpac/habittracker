package com.marta.habittracker.core.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.marta.habittracker.domain.reminder.DailyCheckInScheduler
import com.marta.habittracker.domain.reminder.NextHabitReminder
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.datetime.DayOfWeek

@Singleton
class AndroidDailyCheckInScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) : DailyCheckInScheduler {

    override fun schedule(enabled: Boolean, time: String) {
        try {
            val trigger = NextHabitReminder.nextTrigger(
                now = ZonedDateTime.now(),
                reminderTime = time,
                daysOfWeek = DayOfWeek.entries.toSet(),
            )
            if (!enabled || trigger == null) {
                cancel()
                return
            }
            val alarmManager = context.getSystemService(AlarmManager::class.java)
            if (alarmManager == null) {
                Log.e(TAG, "schedule failed: AlarmManager missing")
                return
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                !alarmManager.canScheduleExactAlarms()
            ) {
                Log.w(TAG, "schedule skipped: exact alarms disabled")
                return
            }
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                trigger.toEpochMilli(),
                pendingIntent(),
            )
            Log.d(TAG, "schedule ok")
        } catch (e: Exception) {
            Log.e(TAG, "schedule failed", e)
            throw e
        }
    }

    private fun cancel() {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        if (alarmManager == null) {
            Log.e(TAG, "cancel failed: AlarmManager missing")
            return
        }
        alarmManager.cancel(pendingIntent())
        Log.d(TAG, "cancel ok")
    }

    private fun pendingIntent(): PendingIntent {
        val intent = Intent(context, HabitReminderReceiver::class.java).apply {
            putExtra(EXTRA_DAILY_CHECK_IN, true)
        }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        private const val TAG = "DailyCheckInSched"
        const val EXTRA_DAILY_CHECK_IN = "extra_daily_check_in"
        const val REQUEST_CODE = 1_700_000_001
    }
}
