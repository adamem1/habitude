package com.adam.habituator.domain

import com.adam.habituator.data.db.CategoryEntity
import com.adam.habituator.data.db.HabitItemEntity
import com.adam.habituator.data.db.LogEntryEntity
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.Instant

class AnalyticsCalculatorTest {

    private val now = Instant.now()
    private val withinWeek = Duration.ofHours(1)

    private fun item(id: Long, weeklyGoalCount: Int, categoryId: Long = 1) = HabitItemEntity(
        id = id,
        categoryId = categoryId,
        name = "Habit $id",
        weeklyGoalCount = weeklyGoalCount,
        createdAt = now,
    )

    private fun category(id: Long, name: String) = CategoryEntity(
        id = id,
        name = name,
        colorArgb = 0xFF000000.toInt(),
        sortOrder = id.toInt(),
    )

    private fun log(habitItemId: Long, at: Instant, quantity: Double? = null) =
        LogEntryEntity(habitItemId = habitItemId, loggedAt = at, quantity = quantity)

    @Test
    fun `categoryGoalMetRatio is zero when there are no items`() {
        assertEquals(
            0f,
            AnalyticsCalculator.categoryGoalMetRatio(emptyList(), emptyMap(), weeks = 4, now = now),
        )
    }

    @Test
    fun `categoryGoalMetRatio counts each item-week independently`() {
        val weekStarts = WeekUtils.recentWeekStarts(2, now)
        val itemA = item(id = 1, weeklyGoalCount = 1)
        val itemB = item(id = 2, weeklyGoalCount = 1)
        // itemA hits its goal both weeks, itemB hits it only the most recent week.
        val logsByItem = mapOf(
            1L to listOf(
                log(1, weekStarts[0].plus(withinWeek)),
                log(1, weekStarts[1].plus(withinWeek)),
            ),
            2L to listOf(
                log(2, weekStarts[1].plus(withinWeek)),
            ),
        )
        // 3 of 4 item-weeks met their goal.
        assertEquals(
            0.75f,
            AnalyticsCalculator.categoryGoalMetRatio(listOf(itemA, itemB), logsByItem, weeks = 2, now = now),
        )
    }

    @Test
    fun `medianWeeklySessionsByCategory omits categories with no habits`() {
        val categories = listOf(category(1, "Has habits"), category(2, "Empty"))
        val items = listOf(item(id = 1, weeklyGoalCount = 1, categoryId = 1))

        val result = AnalyticsCalculator.medianWeeklySessionsByCategory(
            categories = categories,
            items = items,
            logsByItem = emptyMap(),
            weeks = 2,
            now = now,
        )

        assertEquals(listOf("Has habits"), result.map { it.category.name })
    }

    @Test
    fun `medianWeeklySessionsByCategory computes the median across recent weeks and ranks categories`() {
        val weekStarts = WeekUtils.recentWeekStarts(3, now)
        val busy = item(id = 1, weeklyGoalCount = 1, categoryId = 1)
        val quiet = item(id = 2, weeklyGoalCount = 1, categoryId = 2)
        val categories = listOf(category(1, "Busy"), category(2, "Quiet"))

        // Busy: 1, 3, 2 sessions across the three weeks -> median 2.
        // Quiet: 0, 0, 1 sessions -> median 0.
        val logsByItem = mapOf(
            1L to listOf(
                log(1, weekStarts[0].plus(withinWeek)),
                log(1, weekStarts[1].plus(withinWeek)),
                log(1, weekStarts[1].plus(withinWeek.multipliedBy(2))),
                log(1, weekStarts[1].plus(withinWeek.multipliedBy(3))),
                log(1, weekStarts[2].plus(withinWeek)),
                log(1, weekStarts[2].plus(withinWeek.multipliedBy(2))),
            ),
            2L to listOf(
                log(2, weekStarts[2].plus(withinWeek)),
            ),
        )

        val result = AnalyticsCalculator.medianWeeklySessionsByCategory(
            categories = categories,
            items = listOf(busy, quiet),
            logsByItem = logsByItem,
            weeks = 3,
            now = now,
        )

        assertEquals(listOf("Busy", "Quiet"), result.map { it.category.name })
        assertEquals(2.0, result[0].medianWeeklySessions, 0.0)
        assertEquals(0.0, result[1].medianWeeklySessions, 0.0)
    }

    @Test
    fun `weeklyVolumes reports session counts and goal status per week, oldest first`() {
        val weekStarts = WeekUtils.recentWeekStarts(2, now)
        val habit = item(id = 1, weeklyGoalCount = 2)
        val logs = listOf(
            log(1, weekStarts[0].plus(withinWeek), quantity = 5.0),
            log(1, weekStarts[1].plus(withinWeek)),
            log(1, weekStarts[1].plus(withinWeek.multipliedBy(2))),
        )

        val volumes = AnalyticsCalculator.weeklyVolumes(habit, logs, weeks = 2, now = now)

        assertEquals(2, volumes.size)
        assertEquals(weekStarts[0], volumes[0].weekStart)
        assertEquals(1, volumes[0].sessionCount)
        assertEquals(5.0, volumes[0].quantityTotal, 0.0)
        assertEquals(false, volumes[0].goalMet)

        assertEquals(weekStarts[1], volumes[1].weekStart)
        assertEquals(2, volumes[1].sessionCount)
        assertEquals(true, volumes[1].goalMet)
    }
}
