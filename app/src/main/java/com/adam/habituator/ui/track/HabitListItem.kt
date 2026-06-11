package com.adam.habituator.ui.track

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import com.adam.habituator.ui.components.formatQuantity
import com.adam.habituator.ui.theme.GoalMetColor
import com.adam.habituator.ui.theme.NotLoggedColor
import com.adam.habituator.ui.theme.OnGoalMetColor
import com.adam.habituator.ui.theme.OnNotLoggedColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Minimum gap between taps on the log button, to avoid accidental double-logging. */
private const val LOG_DEBOUNCE_MILLIS = 600L

@Composable
fun HabitListItem(
    state: HabitItemUiState,
    onClick: () -> Unit,
    onLog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val item = state.item
    val loggedToday = state.sessionsToday > 0

    var bounceKey by remember { mutableIntStateOf(0) }
    val scale = remember { Animatable(1f) }

    LaunchedEffect(bounceKey) {
        if (bounceKey == 0) return@LaunchedEffect
        scale.snapTo(1f)
        scale.animateTo(1.3f, animationSpec = tween(100))
        scale.animateTo(1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy))
    }

    var clickEnabled by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(item.name, style = MaterialTheme.typography.bodyLarge)
            val progress = item.weeklyGoalCount?.let { goal ->
                "${state.sessionsThisWeek}/$goal this week"
            } ?: "${state.sessionsThisWeek} this week"
            val progressText = if (item.tracksQuantity && item.quantityUnit != null) {
                "$progress • ${formatQuantity(state.quantityThisWeek)} ${item.quantityUnit}"
            } else {
                progress
            }
            Text(
                progressText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        val containerColor = if (loggedToday) GoalMetColor else NotLoggedColor
        val contentColor = if (loggedToday) OnGoalMetColor else OnNotLoggedColor
        Box(
            modifier = Modifier
                .scale(scale.value)
                .size(40.dp)
                .clip(CircleShape)
                .background(containerColor)
                .clickable(enabled = clickEnabled) {
                    clickEnabled = false
                    bounceKey++
                    onLog()
                    coroutineScope.launch {
                        delay(LOG_DEBOUNCE_MILLIS)
                        clickEnabled = true
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            if (loggedToday) {
                Text(
                    text = state.sessionsToday.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = contentColor,
                )
            } else {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Log ${item.name}",
                    tint = contentColor,
                )
            }
        }
    }
}
