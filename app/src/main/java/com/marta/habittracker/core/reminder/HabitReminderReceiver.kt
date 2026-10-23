package com.marta.habittracker.core.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.BroadcastReceiver.PendingResult
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.marta.habittracker.R
import com.marta.habittracker.core.toKotlin
import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.model.Habit
import com.marta.habittracker.domain.reminder.NextHabitReminder
import com.marta.habittracker.domain.reminder.shouldSendDailyCheckIn
import com.marta.habittracker.presentation.widgets.WidgetLaunchExtras
import com.marta.habittracker.presentation.widgets.WidgetRefresher
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
        if (intent.action == ACTION_COMPLETE) {
            completeFromNotification(context.applicationContext, intent, pendingResult)
            return
        }
        if (intent.getBooleanExtra(AndroidDailyCheckInScheduler.EXTRA_DAILY_CHECK_IN, false)) {
            handleDailyCheckIn(context.applicationContext, pendingResult)
            return
        }
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

    private fun handleDailyCheckIn(appContext: Context, pendingResult: PendingResult) {
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                Log.d(TAG, "daily check-in fired")
                val entryPoint = EntryPointAccessors.fromApplication(
                    appContext,
                    HabitReminderEntryPoint::class.java,
                )
                val habits = entryPoint.habitRepository().getAllHabitsWithRecords().first()
                val today = LocalDate.now().toKotlin()
                if (shouldSendDailyCheckIn(habits, today)) {
                    showDailyCheckIn(appContext)
                } else {
                    Log.d(TAG, "daily check-in skipped")
                }
                entryPoint.dailyCheckInRepository().reschedule()
                Log.d(TAG, "daily check-in handled")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "daily check-in failed", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showDailyCheckIn(context: Context) {
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        if (notificationManager == null) {
            Log.e(TAG, "daily check-in skipped: NotificationManager missing")
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
            AndroidDailyCheckInScheduler.REQUEST_CODE,
            WidgetLaunchExtras.openHomeIntent(context),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, AndroidHabitReminderScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.daily_check_in_title))
            .setContentText(context.getString(R.string.daily_check_in_body))
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()
        notificationManager.notify(AndroidDailyCheckInScheduler.REQUEST_CODE, notification)
        Log.d(TAG, "daily check-in shown")
    }

    private fun completeFromNotification(
        appContext: Context,
        intent: Intent,
        pendingResult: PendingResult,
    ) {
        val habitId = intent.getStringExtra(WidgetLaunchExtras.EXTRA_HABIT_ID)
        if (habitId.isNullOrBlank()) {
            Log.w(TAG, "complete missing habit id")
            pendingResult.finish()
            return
        }
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                Log.d(TAG, "complete from notification habitId=$habitId")
                val repository = EntryPointAccessors.fromApplication(
                    appContext,
                    HabitReminderEntryPoint::class.java,
                ).habitRepository()
                val habit = repository.getHabitById(habitId).first()
                val notificationId = HabitReminderIds.notificationIdFor(habitId)
                if (habit == null) {
                    Log.w(TAG, "complete skipped: habit missing habitId=$habitId")
                    cancelNotification(appContext, notificationId)
                    return@launch
                }
                val habitWithRecords = habit.copy(
                    records = repository.getRecordsForHabit(habitId).first(),
                    isCompleted = false,
                )
                val today = LocalDate.now()
                if (!NextHabitReminder.shouldCompleteFromReminder(habitWithRecords, today.toKotlin())) {
                    Log.d(TAG, "already completed; dismissed habitId=$habitId")
                    cancelNotification(appContext, notificationId)
                    return@launch
                }
                when (val result = repository.toggleHabitCompletion(habitWithRecords, today)) {
                    is DataResult.Success -> {
                        cancelNotification(appContext, notificationId)
                        WidgetRefresher.refreshAll(appContext)
                        Log.d(TAG, "completed from notification habitId=$habitId")
                    }
                    is DataResult.Error ->
                        Log.e(TAG, "complete from notification failed habitId=$habitId error=${result.error}")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "complete from notification failed habitId=$habitId", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun cancelNotification(context: Context, notificationId: Int) {
        context.getSystemService(NotificationManager::class.java)?.cancel(notificationId)
            ?: Log.e(TAG, "cancel skipped: NotificationManager missing id=$notificationId")
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
        val completeIntent = PendingIntent.getBroadcast(
            context,
            HabitReminderIds.completeActionIdFor(habit.id),
            Intent(context, HabitReminderReceiver::class.java).apply {
                action = ACTION_COMPLETE
                putExtra(WidgetLaunchExtras.EXTRA_HABIT_ID, habit.id)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, AndroidHabitReminderScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(habit.name)
            .setContentText(context.getString(R.string.habit_reminder_body))
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .addAction(
                NotificationCompat.Action(
                    0,
                    context.getString(R.string.habit_reminder_complete),
                    completeIntent,
                ),
            )
            .build()
        notificationManager.notify(HabitReminderIds.notificationIdFor(habit.id), notification)
        Log.d(TAG, "notify shown habitId=${habit.id}")
    }

    companion object {
        private const val TAG = "HabitReminderRecv"
        private const val ACTION_COMPLETE = "com.marta.habittracker.action.COMPLETE_HABIT"
    }
}
