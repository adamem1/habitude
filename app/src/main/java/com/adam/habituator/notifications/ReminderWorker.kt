package com.adam.habituator.notifications

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.adam.habituator.HabituatorApplication
import com.adam.habituator.R
import com.adam.habituator.domain.WeekUtils
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.ZoneId

/**
 * Periodic check (see [ReminderScheduler]) that posts a reminder notification, on the
 * relevant habit's own channel, for any habit that hasn't been logged today and hasn't
 * yet hit its weekly goal.
 */
class ReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as HabituatorApplication).container

        if (!container.userPreferencesRepository.remindersEnabled.first()) {
            return Result.success()
        }
        if (ActivityCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return Result.success()
        }

        val now = Instant.now()
        val zone = ZoneId.systemDefault()
        val todayStart = WeekUtils.startOfDay(now, zone)
        val weekStart = WeekUtils.startOfWeek(now, zone)
        val weekEnd = WeekUtils.endOfWeek(now, zone)

        val nowZoned = now.atZone(zone)
        val todayBit = 1 shl (nowZoned.dayOfWeek.value - 1)
        val nowMinutesOfDay = nowZoned.hour * 60 + nowZoned.minute

        val items = container.habitRepository.observeItems().first()
        val logsByItem = container.logRepository.observeAll().first().groupBy { it.habitItemId }

        val notificationManager = NotificationManagerCompat.from(applicationContext)

        for (item in items) {
            if (!item.reminderEnabled) continue
            if (item.reminderDaysOfWeek and todayBit == 0) continue
            val scheduledMinutes = item.reminderTimeMinutes
            if (scheduledMinutes != null && nowMinutesOfDay < scheduledMinutes) continue

            val itemLogs = logsByItem[item.id].orEmpty()
            val loggedToday = itemLogs.any { it.loggedAt >= todayStart }
            if (loggedToday) continue

            val sessionsThisWeek = itemLogs.count { it.loggedAt >= weekStart && it.loggedAt < weekEnd }
            val goal = item.weeklyGoalCount
            if (goal != null && sessionsThisWeek >= goal) continue

            val contentText = if (goal != null) {
                "$sessionsThisWeek/$goal this week so far"
            } else {
                "$sessionsThisWeek this week so far"
            }
            val notification = NotificationCompat.Builder(
                applicationContext,
                NotificationChannelManager.itemChannelId(item.id),
            )
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(item.name)
                .setContentText(contentText)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(item.id.toInt(), notification)
        }

        return Result.success()
    }
}
