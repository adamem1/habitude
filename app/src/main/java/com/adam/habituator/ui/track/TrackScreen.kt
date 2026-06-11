package com.adam.habituator.ui.track

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.adam.habituator.data.db.CategoryEntity
import com.adam.habituator.data.db.HabitItemEntity
import com.adam.habituator.data.db.LogEntryEntity
import com.adam.habituator.ui.components.ConfettiOverlay
import com.adam.habituator.ui.rememberAppContainer
import com.adam.habituator.ui.settings.SettingsSheet
import com.adam.habituator.ui.theme.GoalMetColor
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackScreen(modifier: Modifier = Modifier) {
    val container = rememberAppContainer()
    val viewModel: TrackViewModel = viewModel(factory = TrackViewModel.factory(container))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var habitSheetTarget by remember { mutableStateOf<HabitEditTarget?>(null) }
    var quantityLogTarget by remember { mutableStateOf<HabitItemEntity?>(null) }
    var collapsedCategoryIds by remember { mutableStateOf(emptySet<Long>()) }
    var showManageCategories by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var categoryDialogTarget by remember { mutableStateOf<CategoryEditTarget?>(null) }
    var confettiTrigger by remember { mutableIntStateOf(0) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    fun showLogFeedback(feedback: LogFeedback?) {
        if (feedback == null) return
        val message = when {
            feedback.newCategoryLevel != null ->
                "${feedback.itemName} logged — ${feedback.categoryName} reached level ${feedback.newCategoryLevel}!"
            feedback.weeklyGoalJustMet ->
                "${feedback.itemName} logged — weekly goal met!"
            else ->
                "Getting shit done. Fuck yeah!"
        }
        if (feedback.weeklyGoalJustMet || feedback.newCategoryLevel != null) {
            confettiTrigger++
        }
        coroutineScope.launch { snackbarHostState.showSnackbar(message) }
    }

    val allCategories = uiState.categories.map { it.category }

    Box(modifier = modifier) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = { Text("Habitude") },
                    actions = {
                        TextButton(onClick = { showManageCategories = true }) {
                            Icon(Icons.Default.Category, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Categories")
                        }
                        IconButton(onClick = { showSettings = true }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    },
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { habitSheetTarget = HabitEditTarget(item = null) },
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add habit")
                }
            },
        ) { innerPadding ->
            if (allCategories.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Tap Categories above to add a category, then tap + to add your first habit.",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(32.dp),
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(bottom = 88.dp),
                ) {
                    val hasWeeklyGoals = uiState.categories.any { cwi ->
                        cwi.category.weeklyGoalCount != null || cwi.items.any { it.item.weeklyGoalCount != null }
                    }
                    if (hasWeeklyGoals) {
                        item(key = "this-week") {
                            ThisWeekSection(
                                categories = uiState.categories,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                            )
                            HorizontalDivider()
                        }
                    }
                    uiState.categories.forEach { categoryWithItems ->
                        val categoryId = categoryWithItems.category.id
                        val expanded = categoryId !in collapsedCategoryIds
                        item(key = "header-$categoryId") {
                            CategoryHeader(
                                category = categoryWithItems.category,
                                level = categoryWithItems.level,
                                points = categoryWithItems.totalPoints,
                                sessionsThisWeek = categoryWithItems.sessionsThisWeek,
                                expanded = expanded,
                                onToggleExpanded = {
                                    collapsedCategoryIds = if (expanded) {
                                        collapsedCategoryIds + categoryId
                                    } else {
                                        collapsedCategoryIds - categoryId
                                    }
                                },
                            )
                        }
                        if (expanded) {
                            items(categoryWithItems.items, key = { "item-${it.item.id}" }) { habitState ->
                                HabitListItem(
                                    state = habitState,
                                    onClick = { habitSheetTarget = HabitEditTarget(item = habitState.item) },
                                    onLog = {
                                        if (habitState.item.tracksQuantity) {
                                            quantityLogTarget = habitState.item
                                        } else {
                                            showLogFeedback(viewModel.logSession(habitState.item.id))
                                        }
                                    },
                                    modifier = Modifier.padding(start = 24.dp),
                                )
                            }
                            if (categoryWithItems.items.isEmpty()) {
                                item(key = "empty-$categoryId") {
                                    Text(
                                        "No habits yet in this category.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        ConfettiOverlay(trigger = confettiTrigger, modifier = Modifier.fillMaxSize())
    }

    habitSheetTarget?.let { target ->
        val recentLogs by remember(target.item?.id) {
            target.item?.let { viewModel.observeLogsForItem(it.id) } ?: emptyFlow<List<LogEntryEntity>>()
        }.collectAsStateWithLifecycle(initialValue = emptyList())

        AddEditHabitSheet(
            item = target.item,
            categories = allCategories,
            recentLogs = recentLogs,
            onDismiss = { habitSheetTarget = null },
            onSave = { habitItem ->
                viewModel.saveHabitItem(habitItem)
                habitSheetTarget = null
            },
            onDelete = { habitItem ->
                viewModel.deleteHabitItem(habitItem)
                habitSheetTarget = null
            },
            onUpdateLog = viewModel::updateLogEntry,
            onDeleteLog = viewModel::deleteLogEntry,
        )
    }

    quantityLogTarget?.let { habitItem ->
        QuantityLogDialog(
            item = habitItem,
            onDismiss = { quantityLogTarget = null },
            onConfirm = { quantity ->
                showLogFeedback(viewModel.logSession(habitItem.id, quantity))
                quantityLogTarget = null
            },
        )
    }

    if (showManageCategories) {
        ManageCategoriesDialog(
            categories = allCategories,
            onDismiss = { showManageCategories = false },
            onAddCategory = {
                showManageCategories = false
                categoryDialogTarget = CategoryEditTarget(category = null)
            },
            onEditCategory = { category ->
                showManageCategories = false
                categoryDialogTarget = CategoryEditTarget(category = category)
            },
        )
    }

    categoryDialogTarget?.let { target ->
        AddEditCategoryDialog(
            category = target.category,
            onDismiss = { categoryDialogTarget = null },
            onSave = { category ->
                viewModel.saveCategory(category)
                categoryDialogTarget = null
            },
            onDelete = { category ->
                viewModel.deleteCategory(category)
                categoryDialogTarget = null
            },
        )
    }

    if (showSettings) {
        SettingsSheet(onDismiss = { showSettings = false })
    }
}

private data class HabitEditTarget(val item: HabitItemEntity?)
private data class CategoryEditTarget(val category: CategoryEntity?)

/** A single weekly goal (per-habit or per-category) and its progress, for the "This week" section. */
private data class WeeklyGoalProgress(
    val name: String,
    val colorArgb: Int,
    val current: Int,
    val goal: Int,
) {
    val met: Boolean get() = current >= goal
    val deficit: Int get() = goal - current

    /** Sessions counted toward the overall weekly progress bar, capped at this goal's target. */
    val completed: Int get() = current.coerceAtMost(goal)
}

@Composable
private fun ThisWeekSection(
    categories: List<CategoryWithItems>,
    modifier: Modifier = Modifier,
) {
    val itemGoals = categories.flatMap { cwi ->
        cwi.items.filter { it.item.weeklyGoalCount != null }.map { itemState ->
            WeeklyGoalProgress(
                name = itemState.item.name,
                colorArgb = cwi.category.colorArgb,
                current = itemState.sessionsThisWeek,
                goal = itemState.item.weeklyGoalCount!!,
            )
        }
    }
    val categoryGoals = categories.mapNotNull { cwi ->
        cwi.category.weeklyGoalCount?.let { goal ->
            WeeklyGoalProgress(
                name = "${cwi.category.name} (overall)",
                colorArgb = cwi.category.colorArgb,
                current = cwi.sessionsThisWeek,
                goal = goal,
            )
        }
    }
    val allGoals = itemGoals + categoryGoals
    val totalCompleted = allGoals.sumOf { it.completed }
    val totalGoal = allGoals.sumOf { it.goal }
    val stillToDo = allGoals.filter { !it.met }.sortedByDescending { it.deficit }

    Column(modifier = modifier) {
        Text("This week", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            LinearProgressIndicator(
                progress = { totalCompleted.toFloat() / totalGoal },
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "$totalCompleted/$totalGoal this week",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (stillToDo.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text("Still to do this week", style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(4.dp))
            stillToDo.forEach { goal ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                ) {
                    Spacer(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(goal.colorArgb))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(goal.name, modifier = Modifier.weight(1f))
                    Text(
                        "${goal.current}/${goal.goal}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "All weekly goals are on track.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CategoryHeader(
    category: CategoryEntity,
    level: Int,
    points: Int,
    sessionsThisWeek: Int,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onToggleExpanded)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Spacer(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(Color(category.colorArgb))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(category.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text(
                "Lvl $level • $points pts",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Icon(
                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        category.weeklyGoalCount?.let { goal ->
            val goalMet = sessionsThisWeek >= goal
            Text(
                "$sessionsThisWeek/$goal this week",
                style = MaterialTheme.typography.labelMedium,
                color = if (goalMet) GoalMetColor else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 20.dp, top = 2.dp),
            )
        }
    }
}
