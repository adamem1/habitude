package com.adam.habituator.ui.components

import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Renders whole numbers without a trailing ".0" while keeping decimals for fractional amounts. */
fun formatQuantity(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()

private val LOG_TIME_FORMATTER = DateTimeFormatter.ofPattern("h:mm a")
private val LOG_DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM d")

/** Renders a log timestamp as "Today, 2:34 PM", "Yesterday, 6:00 AM", or "Jun 3, 6:00 AM". */
fun formatLogTimestamp(
    instant: Instant,
    zone: ZoneId = ZoneId.systemDefault(),
    now: Instant = Instant.now(),
): String {
    val zoned = instant.atZone(zone)
    val date = zoned.toLocalDate()
    val today = now.atZone(zone).toLocalDate()
    val time = LOG_TIME_FORMATTER.format(zoned)
    return when (date) {
        today -> "Today, $time"
        today.minusDays(1) -> "Yesterday, $time"
        else -> "${LOG_DATE_FORMATTER.format(date)}, $time"
    }
}

/** Renders minutes-since-midnight (e.g. 1080) as "6:00 PM". */
fun formatTimeOfDay(minutesSinceMidnight: Int): String =
    LOG_TIME_FORMATTER.format(LocalTime.of(minutesSinceMidnight / 60, minutesSinceMidnight % 60))
