package com.adam.habituator.domain

import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/** Week boundaries are Monday-start, per user preference. */
object WeekUtils {

    fun startOfDay(instant: Instant, zone: ZoneId = ZoneId.systemDefault()): Instant =
        instant.atZone(zone).toLocalDate().atStartOfDay(zone).toInstant()

    /** Exclusive end bound: the start of the following day. */
    fun endOfDay(instant: Instant, zone: ZoneId = ZoneId.systemDefault()): Instant =
        startOfDay(instant, zone).atZone(zone).plusDays(1).toInstant()

    fun startOfWeek(instant: Instant, zone: ZoneId = ZoneId.systemDefault()): Instant {
        val monday = instant.atZone(zone).toLocalDate()
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        return monday.atStartOfDay(zone).toInstant()
    }

    /** Exclusive end bound: the start of the following week. */
    fun endOfWeek(instant: Instant, zone: ZoneId = ZoneId.systemDefault()): Instant =
        startOfWeek(instant, zone).atZone(zone).plusWeeks(1).toInstant()

    /** Start-of-week instants for the [count] most recent weeks, oldest first, ending with the current week. */
    fun recentWeekStarts(count: Int, now: Instant = Instant.now(), zone: ZoneId = ZoneId.systemDefault()): List<Instant> {
        val currentWeekStart = startOfWeek(now, zone)
        return (count - 1 downTo 0).map { weeksAgo ->
            currentWeekStart.atZone(zone).minusWeeks(weeksAgo.toLong()).toInstant()
        }
    }
}
