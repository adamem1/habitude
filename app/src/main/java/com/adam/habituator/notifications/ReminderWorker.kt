package com.adam.habituator.notifications

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.adam.habituator.HabituatorApplication
import com.adam.habituator.MainActivity
import com.adam.habituator.R
import com.adam.habituator.data.ReminderFrequency
import com.adam.habituator.data.db.HabitItemEntity
import com.adam.habituator.domain.WeekUtils
import kotlinx.coroutines.flow.first
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/**
 * Periodic check (see [ReminderScheduler]) that posts a reminder notification for eligible habits.
 *
 * Rules applied on every run:
 * - Global reminders must be enabled and POST_NOTIFICATIONS permission granted.
 * - Skips habits created today (first day grace period).
 * - Skips habits already logged today.
 * - Skips habits whose weekly goal is already met.
 * - Skips non-custom habits where the remaining goal exceeds the remaining days this week
 *   (impossible to complete at once-a-day pace).
 * - Custom-time habits (reminderTimeMinutes set): always notified when their time arrives,
 *   not subject to the daily app-wide cap.
 * - Non-custom habits: at most one notification per day for the entire app. In WEEKLY frequency
 *   mode, each habit is also notified at most once per week.
 * - Priority: habits that missed their goal last week are sorted first; their message reads
 *   "Conquer <name> this week!" instead of a progress count.
 * - Tapping a notification opens the app on the Track screen.
 */
class ReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    private data class Candidate(
        val item: HabitItemEntity,
        val sessionsThisWeek: Int,
        val missedLastWeek: Boolean,
        val isCustomTime: Boolean,
    )

    override suspend fun doWork(): Result {
        val container = (applicationContext as HabituatorApplication).container
        val prefs = container.userPreferencesRepository

        if (!prefs.remindersEnabled.first()) return Result.success()
        if (ActivityCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return Result.success()

        val now = Instant.now()
        val zone = ZoneId.systemDefault()
        val todayStart = WeekUtils.startOfDay(now, zone)
        val weekStart = WeekUtils.startOfWeek(now, zone)
        val weekEnd = WeekUtils.endOfWeek(now, zone)
        val lastWeekStart = weekStart.atZone(zone).minusWeeks(1).toInstant()

        val nowZoned = now.atZone(zone)
        val todayBit = 1 shl (nowZoned.dayOfWeek.value - 1)
        val nowMinutesOfDay = nowZoned.hour * 60 + nowZoned.minute
        // Number of days remaining this week including today (Mon=7 … Sun=1)
        val daysRemainingInWeek = 8 - nowZoned.dayOfWeek.value
        val todayEpochDay = nowZoned.toLocalDate().toEpochDay()
        val weekStartEpochDay = nowZoned.toLocalDate()
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            .toEpochDay()

        val frequency = prefs.reminderFrequency.first()
        val autoAlertSentToday = prefs.lastAutoAlertEpochDay.first() == todayEpochDay

        val notifiedHabitIdsThisWeek = prefs.notifiedHabitWeeks.first()
            .mapNotNull { entry ->
                val parts = entry.split(":")
                if (parts.size == 2 && parts[1].toLongOrNull() == weekStartEpochDay) {
                    parts[0].toLongOrNull()
                } else null
            }
            .toSet()

        val items = container.habitRepository.observeItems().first()
        val logsByItem = container.logRepository.observeAll().first().groupBy { it.habitItemId }
        val notificationManager = NotificationManagerCompat.from(applicationContext)

        val tapPendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            Intent(applicationContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val candidates = mutableListOf<Candidate>()

        for (item in items) {
            if (item.archived) continue
            if (!item.reminderEnabled) continue
            if (item.reminderDaysOfWeek and todayBit == 0) continue

            val scheduledMinutes = item.reminderTimeMinutes
            if (scheduledMinutes != null && nowMinutesOfDay < scheduledMinutes) continue

            // Grace period: don't notify on the day a habit was created
            if (item.createdAt >= todayStart) continue

            val itemLogs = logsByItem[item.id].orEmpty()
            if (itemLogs.any { it.loggedAt >= todayStart }) continue

            val sessionsThisWeek = itemLogs.count { it.loggedAt >= weekStart && it.loggedAt < weekEnd }
            val goal = item.weeklyGoalCount
            if (goal != null && sessionsThisWeek >= goal) continue

            val isCustomTime = scheduledMinutes != null

            // If the remaining goal exceeds remaining days, logging once a day can't reach it —
            // skip unless the user has set a custom reminder time for this habit.
            if (!isCustomTime && goal != null && (goal - sessionsThisWeek) > daysRemainingInWeek) continue

            val lastWeekSessions = itemLogs.count { it.loggedAt >= lastWeekStart && it.loggedAt < weekStart }
            val missedLastWeek = goal != null && lastWeekSessions < goal

            candidates.add(Candidate(item, sessionsThisWeek, missedLastWeek, isCustomTime))
        }

        val (customCandidates, autoCandidates) = candidates.partition { it.isCustomTime }

        // Custom-time habits are always notified — not subject to the daily or weekly cap.
        for (candidate in customCandidates) {
            notificationManager.notify(
                candidate.item.id.toInt(),
                buildNotification(candidate, tapPendingIntent),
            )
        }

        // Non-custom: at most one per day for the whole app.
        if (!autoAlertSentToday) {
            // Missed-last-week habits first, then normal.
            val sorted = autoCandidates.sortedByDescending { it.missedLastWeek }

            for (candidate in sorted) {
                // In WEEKLY mode, skip habits already notified this week.
                if (frequency == ReminderFrequency.WEEKLY && candidate.item.id in notifiedHabitIdsThisWeek) {
                    continue
                }

                notificationManager.notify(
                    candidate.item.id.toInt(),
                    buildNotification(candidate, tapPendingIntent),
                )
                prefs.setLastAutoAlertEpochDay(todayEpochDay)
                prefs.addNotifiedHabitWeek("${candidate.item.id}:$weekStartEpochDay")
                break
            }
        }

        prefs.pruneNotifiedHabitWeeks(weekStartEpochDay)

        return Result.success()
    }

    private fun buildNotification(candidate: Candidate, tapPendingIntent: PendingIntent): Notification {
        val item = candidate.item
        val contentText = when {
            candidate.missedLastWeek -> "Conquer ${item.name} this week!"
            item.weeklyGoalCount != null -> "${candidate.sessionsThisWeek}/${item.weeklyGoalCount} this week so far"
            else -> "${candidate.sessionsThisWeek} this week so far"
        }
        return NotificationCompat.Builder(applicationContext, NotificationChannelManager.itemChannelId(item.id))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(item.name)
            .setContentText(contentText)
            .setAutoCancel(true)
            .setContentIntent(tapPendingIntent)
            .build()
    }
}
