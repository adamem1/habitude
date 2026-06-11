package com.adam.habituator.data.backup

import kotlinx.serialization.Serializable

/** Bumped whenever the backup shape changes in a way that requires migration logic. */
const val BACKUP_SCHEMA_VERSION = 3

@Serializable
data class BackupRoot(
    val schemaVersion: Int = BACKUP_SCHEMA_VERSION,
    val categories: List<BackupCategory>,
    val habitItems: List<BackupHabitItem>,
    val logEntries: List<BackupLogEntry>,
)

@Serializable
data class BackupCategory(
    val id: Long,
    val name: String,
    val colorArgb: Int,
    val sortOrder: Int,
    val weeklyGoalCount: Int? = null,
)

@Serializable
data class BackupHabitItem(
    val id: Long,
    val categoryId: Long,
    val name: String,
    val weeklyGoalCount: Int? = null,
    val tracksQuantity: Boolean,
    val quantityUnit: String?,
    val quantityGoalPerSession: Double?,
    val reminderEnabled: Boolean,
    val reminderDaysOfWeek: Int = 0b1111111,
    val reminderTimeMinutes: Int? = null,
    val archived: Boolean,
    val createdAtEpochMillis: Long,
)

@Serializable
data class BackupLogEntry(
    val id: Long,
    val habitItemId: Long,
    val loggedAtEpochMillis: Long,
    val quantity: Double?,
)
