package com.adam.habituator.ui.track

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.adam.habituator.data.db.CategoryEntity
import com.adam.habituator.data.db.HabitItemEntity
import com.adam.habituator.data.db.LogEntryEntity
import com.adam.habituator.ui.components.formatLogTimestamp
import com.adam.habituator.ui.components.formatQuantity
import com.adam.habituator.ui.components.formatTimeOfDay
import java.time.DayOfWeek
import java.time.Instant
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditHabitSheet(
    item: HabitItemEntity?,
    categories: List<CategoryEntity>,
    recentLogs: List<LogEntryEntity> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (HabitItemEntity) -> Unit,
    onDelete: ((HabitItemEntity) -> Unit)? = null,
    onUpdateLog: ((LogEntryEntity) -> Unit)? = null,
    onDeleteLog: ((LogEntryEntity) -> Unit)? = null,
) {
    var editLogTarget by remember { mutableStateOf<Pair<LogEntryEntity, HabitItemEntity>?>(null) }
    var name by remember { mutableStateOf(item?.name.orEmpty()) }
    var selectedCategoryId by remember {
        mutableStateOf(item?.categoryId ?: categories.firstOrNull()?.id)
    }
    var hasWeeklyGoal by remember { mutableStateOf(item?.weeklyGoalCount != null) }
    var weeklyGoal by remember { mutableIntStateOf(item?.weeklyGoalCount ?: 3) }
    var tracksQuantity by remember { mutableStateOf(item?.tracksQuantity ?: false) }
    var quantityUnit by remember { mutableStateOf(item?.quantityUnit.orEmpty()) }
    var quantityGoalText by remember {
        mutableStateOf(item?.quantityGoalPerSession?.let(::formatQuantity) ?: "")
    }
    var reminderEnabled by remember { mutableStateOf(item?.reminderEnabled ?: false) }
    var reminderDaysOfWeek by remember {
        mutableIntStateOf(item?.reminderDaysOfWeek ?: HabitItemEntity.ALL_DAYS_MASK)
    }
    var reminderTimeMinutes by remember { mutableStateOf(item?.reminderTimeMinutes) }
    var showTimePicker by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
        ) {
            Text(
                if (item == null) "New habit" else "Edit habit",
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text("Category", style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(8.dp))
            if (categories.isEmpty()) {
                Text(
                    "Create a category first using the Categories button in the top bar.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    categories.forEach { category ->
                        FilterChip(
                            selected = selectedCategoryId == category.id,
                            onClick = { selectedCategoryId = category.id },
                            label = { Text(category.name) },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(Color(category.colorArgb))
                                )
                            },
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Weekly goal", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Track how many times per week you do this habit specifically.",
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
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Track a quantity (reps, minutes, etc.)", modifier = Modifier.weight(1f))
                Switch(checked = tracksQuantity, onCheckedChange = { tracksQuantity = it })
            }
            if (tracksQuantity) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = quantityUnit,
                    onValueChange = { quantityUnit = it },
                    label = { Text("Unit (e.g. reps, minutes)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = quantityGoalText,
                    onValueChange = { quantityGoalText = it },
                    label = { Text("Target per session (optional)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Remind me", modifier = Modifier.weight(1f))
                Switch(checked = reminderEnabled, onCheckedChange = { reminderEnabled = it })
            }
            if (reminderEnabled) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Remind on", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DayOfWeek.entries.forEach { day ->
                        val bit = 1 shl (day.value - 1)
                        val selected = reminderDaysOfWeek and bit != 0
                        FilterChip(
                            selected = selected,
                            onClick = {
                                reminderDaysOfWeek = if (selected) {
                                    reminderDaysOfWeek and bit.inv()
                                } else {
                                    reminderDaysOfWeek or bit
                                }
                            },
                            label = { Text(day.getDisplayName(TextStyle.SHORT, Locale.getDefault())) },
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Reminder time", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Only remind at or after this time. " +
                                (reminderTimeMinutes?.let(::formatTimeOfDay) ?: "Any time"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (reminderTimeMinutes != null) {
                        TextButton(onClick = { reminderTimeMinutes = null }) { Text("Clear") }
                    }
                    TextButton(onClick = { showTimePicker = true }) { Text("Set time") }
                }
            }

            if (item != null && recentLogs.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Recent sessions", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(4.dp))
                recentLogs.take(5).forEach { log ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { editLogTarget = log to item }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(formatLogTimestamp(log.loggedAt), modifier = Modifier.weight(1f))
                        if (item.tracksQuantity && log.quantity != null) {
                            Text(
                                "${formatQuantity(log.quantity)}${item.quantityUnit?.let { " $it" } ?: ""}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit session",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                if (item != null && onDelete != null) {
                    TextButton(onClick = { onDelete(item) }) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Button(
                    onClick = {
                        val categoryId = selectedCategoryId ?: return@Button
                        val base = item ?: HabitItemEntity(
                            categoryId = categoryId,
                            name = "",
                            createdAt = Instant.now(),
                        )
                        onSave(
                            base.copy(
                                categoryId = categoryId,
                                name = name.trim(),
                                weeklyGoalCount = if (hasWeeklyGoal) weeklyGoal else null,
                                tracksQuantity = tracksQuantity,
                                quantityUnit = if (tracksQuantity) quantityUnit.trim().ifBlank { null } else null,
                                quantityGoalPerSession = if (tracksQuantity) quantityGoalText.toDoubleOrNull() else null,
                                reminderEnabled = reminderEnabled,
                                reminderDaysOfWeek = reminderDaysOfWeek,
                                reminderTimeMinutes = reminderTimeMinutes,
                            )
                        )
                    },
                    enabled = name.isNotBlank() && selectedCategoryId != null,
                ) { Text("Save") }
            }
        }
    }

    if (showTimePicker) {
        val initialMinutes = reminderTimeMinutes ?: (18 * 60)
        val timePickerState = rememberTimePickerState(
            initialHour = initialMinutes / 60,
            initialMinute = initialMinutes % 60,
            is24Hour = false,
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    reminderTimeMinutes = timePickerState.hour * 60 + timePickerState.minute
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            },
            text = { TimePicker(state = timePickerState) },
        )
    }

    editLogTarget?.let { (log, habitItem) ->
        EditLogEntryDialog(
            entry = log,
            item = habitItem,
            onDismiss = { editLogTarget = null },
            onSave = { updated ->
                onUpdateLog?.invoke(updated)
                editLogTarget = null
            },
            onDelete = { toDelete ->
                onDeleteLog?.invoke(toDelete)
                editLogTarget = null
            },
        )
    }
}
