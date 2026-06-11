package com.adam.habituator.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "habituator_prefs")

/** Persists the master reminder on/off toggle, independent of any per-item toggle. */
class UserPreferencesRepository(private val context: Context) {

    val remindersEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[REMINDERS_ENABLED_KEY] ?: true
    }

    suspend fun setRemindersEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[REMINDERS_ENABLED_KEY] = enabled }
    }

    private companion object {
        val REMINDERS_ENABLED_KEY = booleanPreferencesKey("reminders_enabled")
    }
}
