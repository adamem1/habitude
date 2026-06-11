package com.adam.habituator.domain

import com.adam.habituator.data.db.CategoryEntity
import com.adam.habituator.data.db.HabitItemEntity
import com.adam.habituator.data.db.LogEntryEntity
import java.time.Instant

/** Pure functions turning raw logs into chart-ready data for the Analytics screen. */
object AnalyticsCalculator {

    /** One week's session/quantity totals for a single habit item, plus whether its goal was met. */
    data class WeeklyVolume(
        val weekStart: Instant,
        val sessionCount: Int,
        val quantityTotal: Double,
        val goalMet: Boolean,
    )

    /**
     * Fraction (0f..1f) of the last [weeks] item-weeks where [items] met their weekly goal.
     * Used as one axis value on the category radar chart. Items with no logs in a given
     * week count as a miss for that week, same as on the Track screen.
     */
    fun categoryGoalMetRatio(
        items: List<HabitItemEntity>,
        logsByItem: Map<Long, List<LogEntryEntity>>,
        weeks: Int = GamificationConfig.RADAR_LOOKBACK_WEEKS,
        now: Instant = Instant.now(),
    ): Float {
        val itemsWithGoals = items.filter { it.weeklyGoalCount != null }
        if (itemsWithGoals.isEmpty()) return 0f

        val weekStarts = WeekUtils.recentWeekStarts(weeks, now)
        var metCount = 0
        for (item in itemsWithGoals) {
            val goal = item.weeklyGoalCount!!
            val sessionsByWeek = logsByItem[item.id].orEmpty()
                .groupBy { WeekUtils.startOfWeek(it.loggedAt) }
                .mapValues { (_, logs) -> logs.size }
            for (weekStart in weekStarts) {
                if ((sessionsByWeek[weekStart] ?: 0) >= goal) metCount++
            }
        }
        val totalCount = itemsWithGoals.size * weekStarts.size
        return metCount.toFloat() / totalCount
    }

    /** A category's typical (median) weekly session count over the last [weeks] weeks. */
    data class CategoryTimeInvestment(
        val category: CategoryEntity,
        val medianWeeklySessions: Double,
    )

    /**
     * Median weekly session count per category over the last [weeks] weeks, across all of that
     * category's habits combined. Categories with no habits are omitted. Sorted with the
     * highest median first, so the first entry is where time is most consistently spent.
     */
    fun medianWeeklySessionsByCategory(
        categories: List<CategoryEntity>,
        items: List<HabitItemEntity>,
        logsByItem: Map<Long, List<LogEntryEntity>>,
        weeks: Int = GamificationConfig.RADAR_LOOKBACK_WEEKS,
        now: Instant = Instant.now(),
    ): List<CategoryTimeInvestment> {
        val weekStarts = WeekUtils.recentWeekStarts(weeks, now)
        val itemsByCategory = items.groupBy { it.categoryId }

        return categories.mapNotNull { category ->
            val categoryItems = itemsByCategory[category.id].orEmpty()
            if (categoryItems.isEmpty()) return@mapNotNull null

            val sessionsPerWeek = weekStarts.map { weekStart ->
                val weekEnd = WeekUtils.endOfWeek(weekStart)
                categoryItems.sumOf { item ->
                    logsByItem[item.id].orEmpty()
                        .count { it.loggedAt >= weekStart && it.loggedAt < weekEnd }
                }
            }
            CategoryTimeInvestment(category = category, medianWeeklySessions = median(sessionsPerWeek))
        }.sortedByDescending { it.medianWeeklySessions }
    }

    private fun median(values: List<Int>): Double {
        if (values.isEmpty()) return 0.0
        val sorted = values.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 1) {
            sorted[mid].toDouble()
        } else {
            (sorted[mid - 1] + sorted[mid]) / 2.0
        }
    }

    /** Per-week session/quantity totals for [item] over the last [weeks] weeks, oldest first. */
    fun weeklyVolumes(
        item: HabitItemEntity,
        logs: List<LogEntryEntity>,
        weeks: Int = GamificationConfig.VOLUME_CHART_WEEKS,
        now: Instant = Instant.now(),
    ): List<WeeklyVolume> {
        val weekStarts = WeekUtils.recentWeekStarts(weeks, now)
        val byWeek = logs.groupBy { WeekUtils.startOfWeek(it.loggedAt) }
        return weekStarts.map { weekStart ->
            val weekLogs = byWeek[weekStart].orEmpty()
            val sessionCount = weekLogs.size
            WeeklyVolume(
                weekStart = weekStart,
                sessionCount = sessionCount,
                quantityTotal = weekLogs.sumOf { it.quantity ?: 0.0 },
                goalMet = item.weeklyGoalCount?.let { sessionCount >= it } ?: (sessionCount > 0),
            )
        }
    }
}
