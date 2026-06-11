package com.adam.habituator.data.repository

import androidx.room.withTransaction
import com.adam.habituator.data.backup.BACKUP_SCHEMA_VERSION
import com.adam.habituator.data.backup.BackupCategory
import com.adam.habituator.data.backup.BackupHabitItem
import com.adam.habituator.data.backup.BackupLogEntry
import com.adam.habituator.data.backup.BackupRoot
import com.adam.habituator.data.db.AppDatabase
import com.adam.habituator.data.db.CategoryEntity
import com.adam.habituator.data.db.HabitItemEntity
import com.adam.habituator.data.db.LogEntryEntity
import com.adam.habituator.notifications.NotificationChannelManager
import java.time.Instant
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class BackupRepository(
    private val database: AppDatabase,
    private val notificationChannelManager: NotificationChannelManager,
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    suspend fun export(): String {
        val categories = database.categoryDao().getAll()
        val habitItems = database.habitItemDao().getAll()
        val logEntries = database.logEntryDao().getAll()

        val root = BackupRoot(
            categories = categories.map {
                BackupCategory(
                    id = it.id,
                    name = it.name,
                    colorArgb = it.colorArgb,
                    sortOrder = it.sortOrder,
                    weeklyGoalCount = it.weeklyGoalCount,
                )
            },
            habitItems = habitItems.map {
                BackupHabitItem(
                    id = it.id,
                    categoryId = it.categoryId,
                    name = it.name,
                    weeklyGoalCount = it.weeklyGoalCount,
                    tracksQuantity = it.tracksQuantity,
                    quantityUnit = it.quantityUnit,
                    quantityGoalPerSession = it.quantityGoalPerSession,
                    reminderEnabled = it.reminderEnabled,
                    reminderDaysOfWeek = it.reminderDaysOfWeek,
                    reminderTimeMinutes = it.reminderTimeMinutes,
                    archived = it.archived,
                    createdAtEpochMillis = it.createdAt.toEpochMilli(),
                )
            },
            logEntries = logEntries.map {
                BackupLogEntry(
                    id = it.id,
                    habitItemId = it.habitItemId,
                    loggedAtEpochMillis = it.loggedAt.toEpochMilli(),
                    quantity = it.quantity,
                )
            },
        )
        return json.encodeToString(root)
    }

    /** Replaces all existing data with the contents of [jsonText]. Throws on invalid/incompatible input. */
    suspend fun import(jsonText: String) {
        val root = json.decodeFromString<BackupRoot>(jsonText)
        check(root.schemaVersion <= BACKUP_SCHEMA_VERSION) {
            "Backup was created by a newer version of Habituator and cannot be restored here."
        }

        val categories = root.categories.map {
            CategoryEntity(
                id = it.id,
                name = it.name,
                colorArgb = it.colorArgb,
                sortOrder = it.sortOrder,
                weeklyGoalCount = it.weeklyGoalCount,
            )
        }
        val habitItems = root.habitItems.map {
            HabitItemEntity(
                id = it.id,
                categoryId = it.categoryId,
                name = it.name,
                weeklyGoalCount = it.weeklyGoalCount,
                tracksQuantity = it.tracksQuantity,
                quantityUnit = it.quantityUnit,
                quantityGoalPerSession = it.quantityGoalPerSession,
                reminderEnabled = it.reminderEnabled,
                reminderDaysOfWeek = it.reminderDaysOfWeek,
                reminderTimeMinutes = it.reminderTimeMinutes,
                archived = it.archived,
                createdAt = Instant.ofEpochMilli(it.createdAtEpochMillis),
            )
        }
        val logEntries = root.logEntries.map {
            LogEntryEntity(
                id = it.id,
                habitItemId = it.habitItemId,
                loggedAt = Instant.ofEpochMilli(it.loggedAtEpochMillis),
                quantity = it.quantity,
            )
        }

        database.withTransaction {
            database.categoryDao().deleteAll()
            database.categoryDao().insertAll(categories)
            database.habitItemDao().insertAll(habitItems)
            database.logEntryDao().insertAll(logEntries)
        }

        notificationChannelManager.syncAll(categories, habitItems)
    }
}
