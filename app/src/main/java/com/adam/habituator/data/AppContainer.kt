package com.adam.habituator.data

import android.content.Context
import com.adam.habituator.data.db.AppDatabase
import com.adam.habituator.data.repository.BackupRepository
import com.adam.habituator.data.repository.CategoryRepository
import com.adam.habituator.data.repository.HabitRepository
import com.adam.habituator.data.repository.LogRepository
import com.adam.habituator.notifications.NotificationChannelManager

/**
 * Hand-rolled DI container (no Hilt) — small enough to wire up directly and
 * easier to keep buildable without a local Gradle/SDK toolchain to iterate against.
 */
class AppContainer(context: Context) {
    private val database = AppDatabase.getInstance(context)

    val categoryRepository = CategoryRepository(database.categoryDao())
    val habitRepository = HabitRepository(database.habitItemDao())
    val logRepository = LogRepository(database.logEntryDao())
    val userPreferencesRepository = UserPreferencesRepository(context)
    val notificationChannelManager = NotificationChannelManager(context)
    val backupRepository = BackupRepository(database, notificationChannelManager)
}
