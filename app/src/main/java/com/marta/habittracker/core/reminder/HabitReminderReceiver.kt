package com.marta.habittracker.core.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.marta.habittracker.R
import com.marta.habittracker.core.toKotlin
import com.marta.habittracker.domain.model.Habit
import com.marta.habittracker.domain.reminder.NextHabitReminder
import com.marta.habittracker.presentation.widgets.WidgetLaunchExtras
import dagger.hilt.android.EntryPointAccessors
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class HabitReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val habitId = intent.getStringExtra(WidgetLaunchExtras.EXTRA_HABIT_ID)
        if (habitId.isNullOrBlank()) {
            Log.w(TAG, "onReceive missing habit id")
            pendingResult.finish()
            return
        }
        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                Log.d(TAG, "alarm fired habitId=$habitId")
                val entryPoint = EntryPointAccessors.fromApplication(
                    appContext,
                    HabitReminderEntryPoint::class.java,
                )
                val scheduler = entryPoint.habitReminderScheduler()
                val repository = entryPoint.habitRepository()
                val habit = repository.getHabitById(habitId).first()
                if (habit == null) {
                    Log.w(TAG, "habit missing; cancel alarm habitId=$habitId")
                    scheduler.cancel(habitId)
                    return@launch
                }
                val habitWithRecords = habit.copy(
                    records = repository.getRecordsForHabit(habitId).first(),
                )
                val today = LocalDate.now().toKotlin()
                if (NextHabitReminder.shouldNotify(habitWithRecords, today)) {
                    showNotification(appContext, habitWithRecords)
                } else {
                    Log.d(TAG, "skip notify habitId=$habitId")
                }
                scheduler.schedule(habit)
                Log.d(TAG, "handled ok habitId=$habitId")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "handle reminder failed habitId=$habitId", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showNotification(context: Context, habit: Habit) {
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        if (notificationManager == null) {
            Log.e(TAG, "notify skipped: NotificationManager missing habitId=${habit.id}")
            return
        }
        val channel = NotificationChannel(
            AndroidHabitReminderScheduler.CHANNEL_ID,
            context.getString(R.string.habit_reminder_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        notificationManager.createNotificationChannel(channel)

        val contentIntent = PendingIntent.getActivity(
            context,
            HabitReminderIds.notificationIdFor(habit.id),
            WidgetLaunchExtras.openHomeIntent(context, habit.id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, AndroidHabitReminderScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(habit.name)
            .setContentText(context.getString(R.string.habit_reminder_body))
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()
        notificationManager.notify(HabitReminderIds.notificationIdFor(habit.id), notification)
        Log.d(TAG, "notify shown habitId=${habit.id}")
    }

    companion object {
        private const val TAG = "HabitReminderRecv"
    }
}
