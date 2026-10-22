package com.marta.habittracker.core.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.marta.habittracker.domain.model.Habit
import com.marta.habittracker.domain.reminder.HabitReminderScheduler
import com.marta.habittracker.domain.reminder.NextHabitReminder
import kotlinx.datetime.DayOfWeek
import com.marta.habittracker.presentation.widgets.WidgetLaunchExtras
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidHabitReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) : HabitReminderScheduler {

    override fun schedule(habit: Habit) {
        try {
            val trigger = NextHabitReminder.nextTrigger(
                now = ZonedDateTime.now(),
                reminderTime = habit.reminderTime,
                daysOfWeek = DayOfWeek.entries.toSet(),
            )
            if (trigger == null || !habit.reminderEnabled) {
                cancel(habit.id)
                return
            }
            val alarmManager = context.getSystemService(AlarmManager::class.java)
            if (alarmManager == null) {
                Log.e(TAG, "schedule failed: AlarmManager missing habitId=${habit.id}")
                return
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                !alarmManager.canScheduleExactAlarms()
            ) {
                Log.w(TAG, "schedule skipped: exact alarms disabled habitId=${habit.id}")
                return
            }
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                trigger.toEpochMilli(),
                pendingIntent(habit.id),
            )
            Log.d(TAG, "schedule ok habitId=${habit.id}")
        } catch (e: Exception) {
            Log.e(TAG, "schedule failed habitId=${habit.id}", e)
            throw e
        }
    }

    override fun cancel(habitId: String) {
        try {
            val alarmManager = context.getSystemService(AlarmManager::class.java)
            if (alarmManager == null) {
                Log.e(TAG, "cancel failed: AlarmManager missing habitId=$habitId")
                return
            }
            alarmManager.cancel(pendingIntent(habitId))
            Log.d(TAG, "cancel ok habitId=$habitId")
        } catch (e: Exception) {
            Log.e(TAG, "cancel failed habitId=$habitId", e)
            throw e
        }
    }

    /**
     * Cancels each listed habit first, then schedules only those with a reminder time.
     * Habits omitted from the list (soft-deleted on sync) keep a leftover alarm until
     * [HabitReminderReceiver] loads a null habit and cancels that id.
     */
    override fun rescheduleAll(habits: List<Habit>) {
        try {
            Log.d(TAG, "rescheduleAll start count=${habits.size}")
            habits.forEach { habit ->
                cancel(habit.id)
                if (habit.reminderEnabled && !habit.reminderTime.isNullOrBlank()) {
                    schedule(habit)
                }
            }
            Log.d(TAG, "rescheduleAll finished count=${habits.size}")
        } catch (e: Exception) {
            Log.e(TAG, "rescheduleAll failed count=${habits.size}", e)
            throw e
        }
    }

    private fun pendingIntent(habitId: String): PendingIntent {
        val intent = Intent(context, HabitReminderReceiver::class.java).apply {
            putExtra(WidgetLaunchExtras.EXTRA_HABIT_ID, habitId)
        }
        return PendingIntent.getBroadcast(
            context,
            HabitReminderIds.notificationIdFor(habitId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        private const val TAG = "HabitReminderSched"
        const val CHANNEL_ID = "habit_reminder_channel"
    }
}
