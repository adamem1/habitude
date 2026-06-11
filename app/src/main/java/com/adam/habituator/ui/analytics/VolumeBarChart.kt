package com.adam.habituator.ui.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.unit.dp
import com.adam.habituator.data.db.HabitItemEntity
import com.adam.habituator.domain.AnalyticsCalculator
import com.adam.habituator.domain.WeekUtils
import com.adam.habituator.ui.theme.GoalMetColor
import com.adam.habituator.ui.theme.GoalMissedColor
import com.adam.habituator.ui.theme.GoalPendingColor
import java.time.Instant

/**
 * Weekly session-count bars for one habit item, oldest week first. Each bar is colored by
 * whether that week's [HabitItemEntity.weeklyGoalCount] was met, missed, or (for the
 * current, still-in-progress week) pending. A dashed line marks the weekly goal.
 */
@Composable
fun VolumeBarChart(
    item: HabitItemEntity,
    volumes: List<AnalyticsCalculator.WeeklyVolume>,
    modifier: Modifier = Modifier,
) {
    if (volumes.isEmpty()) return

    val currentWeekStart = WeekUtils.startOfWeek(Instant.now())
    val maxValue = maxOf(volumes.maxOf { it.sessionCount }, item.weeklyGoalCount ?: 0, 1)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp),
    ) {
        val barSpacing = 4.dp.toPx()
        val barWidth = (size.width - barSpacing * (volumes.size - 1)) / volumes.size

        volumes.forEachIndexed { index, volume ->
            val barHeight = size.height * volume.sessionCount / maxValue.toFloat()
            val color = when {
                volume.goalMet -> GoalMetColor
                volume.weekStart == currentWeekStart -> GoalPendingColor
                else -> GoalMissedColor
            }
            val left = index * (barWidth + barSpacing)
            drawRect(
                color = color,
                topLeft = Offset(left, size.height - barHeight),
                size = Size(barWidth, barHeight),
            )
        }

        item.weeklyGoalCount?.let { goal ->
            val goalLineY = size.height - size.height * goal / maxValue.toFloat()
            drawLine(
                color = Color.White.copy(alpha = 0.35f),
                start = Offset(0f, goalLineY),
                end = Offset(size.width, goalLineY),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f),
            )
        }
    }
}
