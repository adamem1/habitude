package com.adam.habituator.ui.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.adam.habituator.domain.AnalyticsCalculator
import com.adam.habituator.domain.GamificationConfig
import com.adam.habituator.ui.components.formatQuantity
import com.adam.habituator.ui.rememberAppContainer

@Composable
fun AnalyticsScreen(modifier: Modifier = Modifier) {
    val container = rememberAppContainer()
    val viewModel: AnalyticsViewModel = viewModel(factory = AnalyticsViewModel.factory(container))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState.radarAxes.isEmpty() && uiState.items.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "Add categories and habits, then log a few sessions to see your analytics.",
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(32.dp),
            )
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
    ) {
        if (uiState.radarAxes.isNotEmpty()) {
            item {
                Text("Category performance", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Share of the last 8 weeks each category's habits hit their weekly goal.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(12.dp))
                RadarChart(axes = uiState.radarAxes, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        if (uiState.timeInvestment.isNotEmpty()) {
            item {
                TimeInvestmentSection(
                    investments = uiState.timeInvestment,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        if (uiState.items.isNotEmpty()) {
            item {
                Text("Habit detail", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(12.dp))

                var selectedItemId by remember { mutableStateOf<Long?>(null) }
                LaunchedEffect(uiState.items) {
                    if (uiState.items.none { it.item.id == selectedItemId }) {
                        selectedItemId = uiState.items.firstOrNull()?.item?.id
                    }
                }
                val selectedItem = uiState.items.firstOrNull { it.item.id == selectedItemId }

                HabitDropdown(
                    items = uiState.items,
                    selected = selectedItem,
                    onSelect = { selectedItemId = it },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(12.dp))
                selectedItem?.let { itemState ->
                    ItemAnalyticsCard(state = itemState, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HabitDropdown(
    items: List<ItemAnalyticsUiState>,
    selected: ItemAnalyticsUiState?,
    onSelect: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = selected?.item?.name ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text("Habit") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            items.forEach { itemState ->
                DropdownMenuItem(
                    text = { Text("${itemState.category.name} • ${itemState.item.name}") },
                    onClick = {
                        onSelect(itemState.item.id)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun TimeInvestmentSection(
    investments: List<AnalyticsCalculator.CategoryTimeInvestment>,
    modifier: Modifier = Modifier,
) {
    val maxValue = investments.maxOf { it.medianWeeklySessions }.coerceAtLeast(1.0)
    Column(modifier = modifier) {
        Text("Where your time goes", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Median sessions per week over the last ${GamificationConfig.RADAR_LOOKBACK_WEEKS} weeks, by category.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(12.dp))
        investments.forEach { investment ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color(investment.category.colorArgb))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    investment.category.name,
                    modifier = Modifier.width(96.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(
                                fraction = (investment.medianWeeklySessions / maxValue)
                                    .toFloat()
                                    .coerceIn(0f, 1f),
                            )
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(investment.category.colorArgb))
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "${formatQuantity(investment.medianWeeklySessions)}/wk",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ItemAnalyticsCard(state: ItemAnalyticsUiState, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color(state.category.colorArgb))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    state.item.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Text("Lvl ${state.level}", style = MaterialTheme.typography.labelLarge)
            }
            Spacer(modifier = Modifier.height(2.dp))
            val goalText = state.item.weeklyGoalCount?.let { "goal $it/wk" } ?: "no weekly goal"
            Text(
                "${state.totalPoints} pts • ${state.category.name} • $goalText",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(12.dp))
            VolumeBarChart(item = state.item, volumes = state.weeklyVolumes)
        }
    }
}
