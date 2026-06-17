package com.adam.habituator.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.adam.habituator.data.ReminderFrequency
import com.adam.habituator.ui.rememberAppContainer
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val container = rememberAppContainer()
    val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(container))
    val remindersEnabled by viewModel.remindersEnabled.collectAsStateWithLifecycle()
    val reminderFrequency by viewModel.reminderFrequency.collectAsStateWithLifecycle()
    val backupStatus by viewModel.backupStatus.collectAsStateWithLifecycle()

    var showRestoreConfirm by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri -> if (uri != null) viewModel.exportBackup(context, uri) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> if (uri != null) viewModel.importBackup(context, uri) }

    ModalBottomSheet(
        onDismissRequest = {
            viewModel.clearBackupStatus()
            onDismiss()
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
        ) {
            Text("Settings", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Reminders", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Turn off to silence all habit reminders, regardless of per-item settings.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = remindersEnabled, onCheckedChange = viewModel::setRemindersEnabled)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Reminder frequency", style = MaterialTheme.typography.bodyLarge)
            Text(
                "Daily: one nudge per day for your top priority habit. Weekly: each habit reminded at most once per week.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { viewModel.setReminderFrequency(ReminderFrequency.DAILY) },
                    modifier = Modifier.weight(1f),
                    border = if (reminderFrequency == ReminderFrequency.DAILY)
                        BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                ) { Text("Daily") }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = { viewModel.setReminderFrequency(ReminderFrequency.WEEKLY) },
                    modifier = Modifier.weight(1f),
                    border = if (reminderFrequency == ReminderFrequency.WEEKLY)
                        BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                ) { Text("Weekly") }
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(24.dp))

            Text("Backup", style = MaterialTheme.typography.bodyLarge)
            Text(
                "Export all categories, habits, and logged sessions to a JSON file, or restore from a previous export.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { exportLauncher.launch(defaultBackupFileName()) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Export")
                }
                Spacer(modifier = Modifier.width(12.dp))
                OutlinedButton(
                    onClick = { showRestoreConfirm = true },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Restore")
                }
            }

            backupStatus?.let { status ->
                Spacer(modifier = Modifier.height(12.dp))
                val message = when (status) {
                    is BackupStatus.Success -> status.message
                    is BackupStatus.Error -> status.message
                }
                val color = when (status) {
                    is BackupStatus.Success -> MaterialTheme.colorScheme.primary
                    is BackupStatus.Error -> MaterialTheme.colorScheme.error
                }
                Text(message, style = MaterialTheme.typography.bodySmall, color = color)
            }
        }
    }

    if (showRestoreConfirm) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirm = false },
            title = { Text("Restore backup?") },
            text = {
                Text(
                    "This replaces all current categories, habits, and logged sessions with the " +
                        "contents of the backup file. This cannot be undone.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRestoreConfirm = false
                        importLauncher.launch(arrayOf("application/json"))
                    },
                ) {
                    Text("Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirm = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

private fun defaultBackupFileName(): String = "habituator-backup-${LocalDate.now()}.json"
