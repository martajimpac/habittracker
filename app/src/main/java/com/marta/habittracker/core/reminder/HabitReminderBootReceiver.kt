package com.marta.habittracker.core.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class HabitReminderBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_TIMEZONE_CHANGED &&
            action != Intent.ACTION_TIME_CHANGED
        ) {
            Log.w(TAG, "ignored unexpected action=$action")
            return
        }
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                Log.d(TAG, "reschedule start action=$action")
                val entryPoint = EntryPointAccessors.fromApplication(
                    appContext,
                    HabitReminderEntryPoint::class.java,
                )
                val habits = entryPoint.habitRepository().getAllHabitsWithRecords().first()
                entryPoint.habitReminderScheduler().rescheduleAll(habits)
                Log.d(TAG, "reschedule finished action=$action count=${habits.size}")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "reschedule failed action=$action", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "HabitReminderBoot"
    }
}
