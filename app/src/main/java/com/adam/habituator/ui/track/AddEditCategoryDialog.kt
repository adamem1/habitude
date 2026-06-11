package com.adam.habituator.ui.track

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.adam.habituator.data.db.CategoryEntity
import com.adam.habituator.ui.components.ColorSwatchPicker
import com.adam.habituator.ui.theme.CategoryColorPalette
import kotlin.math.roundToInt

@Composable
fun AddEditCategoryDialog(
    category: CategoryEntity?,
    onDismiss: () -> Unit,
    onSave: (CategoryEntity) -> Unit,
    onDelete: ((CategoryEntity) -> Unit)? = null,
) {
    var name by remember { mutableStateOf(category?.name.orEmpty()) }
    var colorArgb by remember {
        mutableIntStateOf(category?.colorArgb ?: CategoryColorPalette.first().color.toArgb())
    }
    var hasWeeklyGoal by remember { mutableStateOf(category?.weeklyGoalCount != null) }
    var weeklyGoal by remember { mutableIntStateOf(category?.weeklyGoalCount ?: 3) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (category == null) "New category" else "Edit category") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(16.dp))
                ColorSwatchPicker(
                    selectedArgb = colorArgb,
                    onSelect = { colorArgb = it },
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Category-level goal", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Track total sessions across all habits in this category, " +
                                "regardless of which one.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = hasWeeklyGoal, onCheckedChange = { hasWeeklyGoal = it })
                }
                if (hasWeeklyGoal) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Times per week: $weeklyGoal", style = MaterialTheme.typography.labelLarge)
                    Slider(
                        value = weeklyGoal.toFloat(),
                        onValueChange = { weeklyGoal = it.roundToInt() },
                        valueRange = 1f..14f,
                        steps = 12,
                    )
                }

                if (category != null && onDelete != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = { onDelete(category) }) {
                        Text("Delete category", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        (category ?: CategoryEntity(name = "", colorArgb = colorArgb)).copy(
                            name = name.trim(),
                            colorArgb = colorArgb,
                            weeklyGoalCount = if (hasWeeklyGoal) weeklyGoal else null,
                        )
                    )
                },
                enabled = name.isNotBlank(),
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
