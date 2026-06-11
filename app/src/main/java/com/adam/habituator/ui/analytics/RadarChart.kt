package com.adam.habituator.ui.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

private const val GRID_RINGS = 4

/** A radar/spider chart with one axis per category, plus a color-coded legend below. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RadarChart(axes: List<RadarAxisUiState>, modifier: Modifier = Modifier) {
    if (axes.isEmpty()) {
        Text(
            "Add categories to see your radar chart.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier,
        )
        return
    }

    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val dataColor = MaterialTheme.colorScheme.primary

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = min(size.width, size.height) / 2f * 0.8f
            val angleStep = (2.0 * PI / axes.size).toFloat()

            fun pointOn(axisRadius: Float, axisIndex: Int): Offset {
                val angle = -(PI / 2.0).toFloat() + axisIndex * angleStep
                return Offset(
                    x = center.x + axisRadius * cos(angle),
                    y = center.y + axisRadius * sin(angle),
                )
            }

            // Background grid rings.
            for (ring in 1..GRID_RINGS) {
                val ringRadius = radius * ring / GRID_RINGS
                val path = Path()
                axes.indices.forEach { i ->
                    val point = pointOn(ringRadius, i)
                    if (i == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
                }
                path.close()
                drawPath(path, color = gridColor, style = Stroke(width = 1.dp.toPx()))
            }

            // Spokes from center to each axis tip.
            axes.indices.forEach { i ->
                drawLine(
                    color = gridColor,
                    start = center,
                    end = pointOn(radius, i),
                    strokeWidth = 1.dp.toPx(),
                )
            }

            // Data polygon.
            val dataPath = Path()
            axes.forEachIndexed { i, axis ->
                val point = pointOn(radius * axis.ratio.coerceIn(0f, 1f), i)
                if (i == 0) dataPath.moveTo(point.x, point.y) else dataPath.lineTo(point.x, point.y)
            }
            dataPath.close()
            drawPath(dataPath, color = dataColor.copy(alpha = 0.25f), style = Fill)
            drawPath(dataPath, color = dataColor, style = Stroke(width = 2.dp.toPx()))

            // Category-colored dot at each axis tip.
            axes.forEachIndexed { i, axis ->
                drawCircle(
                    color = Color(axis.category.colorArgb),
                    radius = 4.dp.toPx(),
                    center = pointOn(radius, i),
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            axes.forEach { axis ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(axis.category.colorArgb))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "${axis.category.name} ${(axis.ratio * 100).roundToInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
    }
}
