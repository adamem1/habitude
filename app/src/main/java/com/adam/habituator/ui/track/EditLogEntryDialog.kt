package com.adam.habituator.ui.track

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.adam.habituator.data.db.HabitItemEntity
import com.adam.habituator.data.db.LogEntryEntity
import com.adam.habituator.ui.components.formatLogTimestamp
import com.adam.habituator.ui.components.formatQuantity

@Composable
fun EditLogEntryDialog(
    entry: LogEntryEntity,
    item: HabitItemEntity,
    onDismiss: () -> Unit,
    onSave: (LogEntryEntity) -> Unit,
    onDelete: (LogEntryEntity) -> Unit,
) {
    var quantityText by remember {
        mutableStateOf(entry.quantity?.let(::formatQuantity) ?: "")
    }
    val quantity = quantityText.toDoubleOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit session") },
        text = {
            Column {
                Text(
                    formatLogTimestamp(entry.loggedAt),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (item.tracksQuantity) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = { quantityText = it },
                        label = { Text(item.quantityUnit ?: "Amount") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = { onDelete(entry) }) {
                    Text("Delete this session", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(entry.copy(quantity = if (item.tracksQuantity) quantity else entry.quantity))
                },
                enabled = !item.tracksQuantity || (quantity != null && quantity > 0),
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
