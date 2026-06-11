package com.adam.habituator.domain

import com.adam.habituator.data.db.HabitItemEntity
import com.adam.habituator.data.db.LogEntryEntity
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.Instant

class GamificationCalculatorTest {

    private val now = Instant.now()
    private val weekStarts = WeekUtils.recentWeekStarts(2, now)
    private val lastWeekStart = weekStarts[0]
    private val thisWeekStart = weekStarts[1]
    private val withinWeek = Duration.ofHours(1)

    private fun item(
        id: Long = 1,
        weeklyGoalCount: Int = 3,
        tracksQuantity: Boolean = false,
    ) = HabitItemEntity(
        id = id,
        categoryId = 1,
        name = "Habit $id",
        weeklyGoalCount = weeklyGoalCount,
        tracksQuantity = tracksQuantity,
        createdAt = now,
    )

    private fun log(at: Instant, habitItemId: Long = 1L, quantity: Double? = null) =
        LogEntryEntity(habitItemId = habitItemId, loggedAt = at, quantity = quantity)

    @Test
    fun `pointsForItem awards base points per log when goal not met`() {
        val habit = item(weeklyGoalCount = 5)
        val logs = listOf(log(thisWeekStart.plus(withinWeek)))
        assertEquals(GamificationConfig.BASE_POINTS_PER_LOG, GamificationCalculator.pointsForItem(habit, logs))
    }

    @Test
    fun `pointsForItem adds capped quantity bonus when tracksQuantity is true`() {
        val habit = item(weeklyGoalCount = 5, tracksQuantity = true)
        val logs = listOf(log(thisWeekStart.plus(withinWeek), quantity = 100.0))
        val expected = GamificationConfig.BASE_POINTS_PER_LOG + GamificationConfig.MAX_QUANTITY_BONUS
        assertEquals(expected, GamificationCalculator.pointsForItem(habit, logs))
    }

    @Test
    fun `pointsForItem ignores quantity when tracksQuantity is false`() {
        val habit = item(weeklyGoalCount = 5, tracksQuantity = false)
        val logs = listOf(log(thisWeekStart.plus(withinWeek), quantity = 100.0))
        assertEquals(GamificationConfig.BASE_POINTS_PER_LOG, GamificationCalculator.pointsForItem(habit, logs))
    }

    @Test
    fun `pointsForItem awards weekly goal bonus once per week the goal is met`() {
        val habit = item(weeklyGoalCount = 2)
        val logs = listOf(
            log(thisWeekStart.plus(withinWeek)),
            log(thisWeekStart.plus(withinWeek.multipliedBy(2))),
            log(lastWeekStart.plus(withinWeek)),
        )
        // This week: 2 logs -> goal met (+50 bonus). Last week: 1 log -> not met.
        val expected = 3 * GamificationConfig.BASE_POINTS_PER_LOG + GamificationConfig.WEEKLY_GOAL_BONUS
        assertEquals(expected, GamificationCalculator.pointsForItem(habit, logs))
    }

    @Test
    fun `pointsForCategory sums points across items`() {
        val habitA = item(id = 1, weeklyGoalCount = 5)
        val habitB = item(id = 2, weeklyGoalCount = 5)
        val logsByItem = mapOf(
            1L to listOf(log(thisWeekStart.plus(withinWeek), habitItemId = 1)),
            2L to listOf(log(thisWeekStart.plus(withinWeek), habitItemId = 2)),
        )
        assertEquals(
            2 * GamificationConfig.BASE_POINTS_PER_LOG,
            GamificationCalculator.pointsForCategory(listOf(habitA, habitB), logsByItem),
        )
    }

    @Test
    fun `levelForPoints starts at 1 and increases every POINTS_PER_LEVEL points`() {
        assertEquals(1, GamificationCalculator.levelForPoints(0))
        assertEquals(1, GamificationCalculator.levelForPoints(GamificationConfig.POINTS_PER_LEVEL - 1))
        assertEquals(2, GamificationCalculator.levelForPoints(GamificationConfig.POINTS_PER_LEVEL))
    }

    @Test
    fun `pointsIntoLevel is the remainder after the current level`() {
        val points = GamificationConfig.POINTS_PER_LEVEL + 42
        assertEquals(42, GamificationCalculator.pointsIntoLevel(points))
    }
}
