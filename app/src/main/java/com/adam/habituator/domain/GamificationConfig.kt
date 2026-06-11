package com.adam.habituator.domain

/** Tunable constants for the points/levels gamification layer. */
object GamificationConfig {
    const val BASE_POINTS_PER_LOG = 10
    const val MAX_QUANTITY_BONUS = 30
    const val WEEKLY_GOAL_BONUS = 50
    const val POINTS_PER_LEVEL = 500

    /** Number of trailing weeks used for the Analytics radar chart's success-rate per category. */
    const val RADAR_LOOKBACK_WEEKS = 8

    /** Number of trailing weeks shown in each item's volume bar chart. */
    const val VOLUME_CHART_WEEKS = 12
}
