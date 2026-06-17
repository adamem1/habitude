package com.adam.habituator.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "habituator_prefs")

enum class ReminderFrequency { DAILY, WEEKLY }

/** Persists global reminder preferences, independent of any per-item toggle. */
class UserPreferencesRepository(private val context: Context) {

    val remindersEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[REMINDERS_ENABLED_KEY] ?: true
    }

    suspend fun setRemindersEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[REMINDERS_ENABLED_KEY] = enabled }
    }

    val reminderFrequency: Flow<ReminderFrequency> = context.dataStore.data.map { prefs ->
        when (prefs[REMINDER_FREQUENCY_KEY]) {
            ReminderFrequency.WEEKLY.name -> ReminderFrequency.WEEKLY
            else -> ReminderFrequency.DAILY
        }
    }

    suspend fun setReminderFrequency(frequency: ReminderFrequency) {
        context.dataStore.edit { prefs -> prefs[REMINDER_FREQUENCY_KEY] = frequency.name }
    }

    /** Epoch day of the last time a non-custom (auto-scheduled) alert was sent app-wide. */
    val lastAutoAlertEpochDay: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[LAST_AUTO_ALERT_EPOCH_DAY_KEY] ?: -1L
    }

    suspend fun setLastAutoAlertEpochDay(epochDay: Long) {
        context.dataStore.edit { prefs -> prefs[LAST_AUTO_ALERT_EPOCH_DAY_KEY] = epochDay }
    }

    /**
     * Set of keys in the form `"habitId:weekStartEpochDay"` recording which habits have already
     * received a non-custom notification in a given week. Used for WEEKLY frequency mode.
     */
    val notifiedHabitWeeks: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[NOTIFIED_HABIT_WEEKS_KEY] ?: emptySet()
    }

    suspend fun addNotifiedHabitWeek(key: String) {
        context.dataStore.edit { prefs ->
            prefs[NOTIFIED_HABIT_WEEKS_KEY] = (prefs[NOTIFIED_HABIT_WEEKS_KEY] ?: emptySet()) + key
        }
    }

    /** Removes entries that don't belong to the current week, keeping the set from growing. */
    suspend fun pruneNotifiedHabitWeeks(currentWeekStartEpochDay: Long) {
        context.dataStore.edit { prefs ->
            val current = prefs[NOTIFIED_HABIT_WEEKS_KEY] ?: return@edit
            prefs[NOTIFIED_HABIT_WEEKS_KEY] = current
                .filter { it.endsWith(":$currentWeekStartEpochDay") }
                .toSet()
        }
    }

    private companion object {
        val REMINDERS_ENABLED_KEY = booleanPreferencesKey("reminders_enabled")
        val REMINDER_FREQUENCY_KEY = stringPreferencesKey("reminder_frequency")
        val LAST_AUTO_ALERT_EPOCH_DAY_KEY = longPreferencesKey("last_auto_alert_epoch_day")
        val NOTIFIED_HABIT_WEEKS_KEY = stringSetPreferencesKey("notified_habit_weeks")
    }
}
