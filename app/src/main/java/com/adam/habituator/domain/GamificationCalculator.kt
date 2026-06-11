package com.adam.habituator.domain

import com.adam.habituator.data.db.HabitItemEntity
import com.adam.habituator.data.db.LogEntryEntity

/** Pure functions turning raw logs into points and levels, per [GamificationConfig]. */
object GamificationCalculator {

    /** Total points earned by [item] across all of [logs] (its full log history). */
    fun pointsForItem(item: HabitItemEntity, logs: List<LogEntryEntity>): Int {
        if (logs.isEmpty()) return 0

        val basePoints = logs.sumOf { log ->
            val quantityBonus = if (item.tracksQuantity) {
                (log.quantity ?: 0.0)
                    .coerceAtMost(GamificationConfig.MAX_QUANTITY_BONUS.toDouble())
                    .toInt()
            } else {
                0
            }
            GamificationConfig.BASE_POINTS_PER_LOG + quantityBonus
        }

        val goal = item.weeklyGoalCount
        val weeksGoalMet = if (goal != null) {
            logs.groupBy { WeekUtils.startOfWeek(it.loggedAt) }
                .count { (_, weekLogs) -> weekLogs.size >= goal }
        } else {
            0
        }

        return basePoints + weeksGoalMet * GamificationConfig.WEEKLY_GOAL_BONUS
    }

    /** Sum of [pointsForItem] across every item in a category. */
    fun pointsForCategory(items: List<HabitItemEntity>, logsByItem: Map<Long, List<LogEntryEntity>>): Int =
        items.sumOf { pointsForItem(it, logsByItem[it.id].orEmpty()) }

    /** Levels start at 1 and increase every [GamificationConfig.POINTS_PER_LEVEL] points. */
    fun levelForPoints(points: Int): Int = points / GamificationConfig.POINTS_PER_LEVEL + 1

    /** Progress towards the next level, in points, for display in a progress bar. */
    fun pointsIntoLevel(points: Int): Int = points % GamificationConfig.POINTS_PER_LEVEL
}
