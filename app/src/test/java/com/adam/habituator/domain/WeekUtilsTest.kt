package com.adam.habituator.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset

class WeekUtilsTest {

    private val zone = ZoneOffset.UTC

    @Test
    fun `startOfWeek returns preceding Monday midnight`() {
        val wednesday = Instant.parse("2026-06-10T15:30:00Z")
        val expectedMonday = Instant.parse("2026-06-08T00:00:00Z")
        assertEquals(expectedMonday, WeekUtils.startOfWeek(wednesday, zone))
    }

    @Test
    fun `startOfWeek on Monday returns same-day midnight`() {
        val mondayMorning = Instant.parse("2026-06-08T08:00:00Z")
        val expectedMonday = Instant.parse("2026-06-08T00:00:00Z")
        assertEquals(expectedMonday, WeekUtils.startOfWeek(mondayMorning, zone))
    }

    @Test
    fun `endOfWeek is exactly seven days after startOfWeek`() {
        val now = Instant.parse("2026-06-10T15:30:00Z")
        val start = WeekUtils.startOfWeek(now, zone)
        val end = WeekUtils.endOfWeek(now, zone)
        assertEquals(start.plusSeconds(7 * 24 * 3600L), end)
    }

    @Test
    fun `recentWeekStarts returns count weeks ending with the current week, oldest first`() {
        val now = Instant.parse("2026-06-10T15:30:00Z")
        val starts = WeekUtils.recentWeekStarts(4, now, zone)

        assertEquals(4, starts.size)
        assertEquals(WeekUtils.startOfWeek(now, zone), starts.last())
        for (i in 1 until starts.size) {
            assertEquals(starts[i - 1].plusSeconds(7 * 24 * 3600L), starts[i])
        }
    }
}
