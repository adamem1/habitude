package com.adam.habituator.notifications

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/** Schedules the recurring [ReminderWorker] check. Safe to call on every app start. */
object ReminderScheduler {
    private const val WORK_NAME = "habit_reminder_check"
    private const val INTERVAL_MINUTES = 60L

    fun schedulePeriodicReminders(context: Context) {
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(INTERVAL_MINUTES, TimeUnit.MINUTES).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }
}
