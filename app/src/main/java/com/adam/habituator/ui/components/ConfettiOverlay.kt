package com.adam.habituator.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import com.adam.habituator.ui.theme.CategoryColorPalette
import kotlin.random.Random

private data class ConfettiParticle(
    val startX: Float,
    val fallTo: Float,
    val driftX: Float,
    val size: Float,
    val rotationTurns: Float,
    val color: Color,
    val delay: Float,
)

private const val PARTICLE_COUNT = 36
private const val DURATION_MS = 1300

/**
 * Brief confetti burst overlay. Fires whenever [trigger] changes to a value greater than 0,
 * and is otherwise invisible. Reuses the category color palette so it fits the app's theme.
 */
@Composable
fun ConfettiOverlay(trigger: Int, modifier: Modifier = Modifier) {
    val particles = remember {
        List(PARTICLE_COUNT) {
            ConfettiParticle(
                startX = Random.nextFloat(),
                fallTo = 0.4f + Random.nextFloat() * 0.6f,
                driftX = (Random.nextFloat() - 0.5f) * 0.6f,
                size = 8f + Random.nextFloat() * 10f,
                rotationTurns = 1f + Random.nextFloat() * 3f,
                color = CategoryColorPalette.random().color,
                delay = Random.nextFloat() * 0.2f,
            )
        }
    }
    val progress = remember { Animatable(0f) }
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(trigger) {
        if (trigger <= 0) return@LaunchedEffect
        visible = true
        progress.snapTo(0f)
        progress.animateTo(1f, animationSpec = tween(DURATION_MS, easing = LinearEasing))
        visible = false
    }

    if (visible) {
        Canvas(modifier = modifier.fillMaxSize()) {
            particles.forEach { particle ->
                val t = ((progress.value - particle.delay) / (1f - particle.delay)).coerceIn(0f, 1f)
                if (t <= 0f) return@forEach
                val x = (particle.startX + particle.driftX * t) * size.width
                val y = (-0.1f + (particle.fallTo + 0.1f) * t) * size.height
                val alpha = if (t > 0.7f) (1f - (t - 0.7f) / 0.3f) else 1f
                rotate(degrees = particle.rotationTurns * t * 360f, pivot = Offset(x, y)) {
                    drawRect(
                        color = particle.color.copy(alpha = alpha),
                        topLeft = Offset(x - particle.size / 2, y - particle.size / 2),
                        size = Size(particle.size, particle.size),
                    )
                }
            }
        }
    }
}
