package com.adam.habituator.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.adam.habituator.data.AppContainer
import com.adam.habituator.data.UserPreferencesRepository
import com.adam.habituator.data.repository.BackupRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface BackupStatus {
    data class Success(val message: String) : BackupStatus
    data class Error(val message: String) : BackupStatus
}

class SettingsViewModel(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val backupRepository: BackupRepository,
) : ViewModel() {

    val remindersEnabled: StateFlow<Boolean> = userPreferencesRepository.remindersEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    private val _backupStatus = MutableStateFlow<BackupStatus?>(null)
    val backupStatus: StateFlow<BackupStatus?> = _backupStatus.asStateFlow()

    fun setRemindersEnabled(enabled: Boolean) {
        viewModelScope.launch { userPreferencesRepository.setRemindersEnabled(enabled) }
    }

    fun exportBackup(context: Context, uri: Uri) {
        viewModelScope.launch {
            _backupStatus.value = runCatching {
                val json = backupRepository.export()
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
                        ?: error("Could not open file for writing")
                }
            }.fold(
                onSuccess = { BackupStatus.Success("Backup exported successfully.") },
                onFailure = { BackupStatus.Error("Export failed: ${it.message}") },
            )
        }
    }

    fun importBackup(context: Context, uri: Uri) {
        viewModelScope.launch {
            _backupStatus.value = runCatching {
                val text = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { it.bufferedReader().readText() }
                        ?: error("Could not open file for reading")
                }
                backupRepository.import(text)
            }.fold(
                onSuccess = { BackupStatus.Success("Backup restored successfully.") },
                onFailure = { BackupStatus.Error("Import failed: ${it.message}") },
            )
        }
    }

    fun clearBackupStatus() {
        _backupStatus.value = null
    }

    companion object {
        fun factory(container: AppContainer) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                SettingsViewModel(container.userPreferencesRepository, container.backupRepository) as T
        }
    }
}
